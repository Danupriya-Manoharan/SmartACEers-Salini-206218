package com.flowsmith;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ACE FlowSmith (AI) - MVP entry point.
 *
 * An agent that accelerates IBM ACE development:
 *   perceive (learn org patterns) -> reason (recommend) -> act (generate)
 *   -> human-in-the-loop (developer reviews).
 *
 * Commands:
 *   list
 *   recommend "<requirement>"
 *   generate --subsys X --app Y --func Z [--ndm N] [--out DIR] [--sample-input XML --sample-output JSON]
 *
 * The reasoning engine is pluggable - see {@link Recommender} (the AI seam).
 * Mapping support:
 *   - Field mappings are inferred 1:1 (position-based) from a sample input XML
 *     and a sample output JSON (--sample-input / --sample-output).
 *   - Both default to the committed samples under test-data/, so no extra input
 *     is required. The same sample XML is reused by 'filetest'.
 */
public class FlowSmith {

    /** Build marker - bump this when rebuilding so you can confirm the running jar is current. */
    private static final String BUILD = "2026-07-08c (sample-based mappings, esb->test path rewrite)";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { usage(); System.exit(0); }

        Path home = jarDir();                 // .../flowsmith-java
        Path repoRoot = home.getParent();     // project root (has Existing_Templates)
        Path catalogFile = home.resolve("patterns.txt");
        Catalog catalog = Catalog.load(catalogFile);

        String cmd = args[0];
        Map<String, String> opt = parseOptions(args);
        Recommender recommender = chooseRecommender(home, opt);

        switch (cmd) {
            case "list":       doList(catalog); break;
            case "recommend":  doRecommend(recommender, catalog, args.length > 1 ? args[1] : ""); break;
            case "generate":   doGenerate(recommender, catalog, home, repoRoot, opt); break;
            case "selftest":   doSelfTest(home); break;
            case "filetest":   doFileTest(catalog, home, opt); break;
            default:           usage();
        }
    }

    /**
     * Pick the reasoning engine (the AI). Default: watsonx.ai when it is
     * configured (credentials present), otherwise the rule-based stand-in.
     * Override with --engine watsonx|keyword.
     */
    private static Recommender chooseRecommender(Path home, Map<String, String> opt) {
        String engine = opt.get("engine");
        if (engine == null) engine = System.getenv("FLOWSMITH_ENGINE");
        if (engine == null) engine = "";

        if (engine.equalsIgnoreCase("keyword")) return new KeywordRecommender();

        WatsonxConfig cfg = WatsonxConfig.load(home);
        if (engine.equalsIgnoreCase("watsonx") || cfg.isComplete()) {
            // watsonx for reasoning, rule-based as a safety net
            return new WatsonxRecommender(cfg, new KeywordRecommender());
        }
        return new KeywordRecommender();
    }

    // ----------------------------------------------------------------- list
    private static void doList(Catalog catalog) {
        banner();
        System.out.println("[AI] Learned " + catalog.patterns().size()
                + " reusable integration patterns from the org knowledge base:\n");
        System.out.printf("  %-20s %-6s %-9s %s%n", "ID", "TYPE", "CONNECT", "TITLE");
        System.out.println("  " + dashes(70));
        for (Pattern p : catalog.patterns()) {
            System.out.printf("  %-20s %-6s %-9s %s%n",
                    p.id, p.integrationType, p.connectivity, p.title);
        }
        System.out.println();
    }

    // ------------------------------------------------------------- recommend
    private static void doRecommend(Recommender recommender, Catalog catalog, String requirement) {
        banner();
        System.out.println("[AI] Reasoning engine : " + recommender.engineName());
        System.out.println("[AI] Requirement      : \"" + requirement + "\"");
        System.out.println("[AI] Matching intent against learned patterns...\n");

        List<Recommender.Recommendation> ranked = recommender.recommend(requirement, catalog);
        if (ranked.isEmpty()) {
            System.out.println("    No confident match. Run 'list' and choose a pattern with --pattern.\n");
            return;
        }
        for (Recommender.Recommendation r : ranked) {
            System.out.printf("    %-20s  score %-3d %s   (%s)%n",
                    r.pattern.id, r.score, stars(r.score), r.rationale);
        }
        Recommender.Recommendation top = ranked.get(0);
        System.out.println("\n[AI] Recommended pattern: " + top.pattern.id
                + "  (" + top.pattern.title + ")");
        System.out.println("[AI] Generate it with:");
        System.out.println("     generate --pattern " + top.pattern.id
                + " --subsys XAJ --app TLMTF --func FINANCING\n");
    }

    // -------------------------------------------------------------- generate
    private static void doGenerate(Recommender recommender, Catalog catalog, Path home, Path repoRoot,
                                   Map<String, String> opt) throws Exception {
        banner();

        // Always generate the PTP file-to-file flow (supports XML->JSON field mappings).
        // Any --pattern / --requirement input is accepted but ignored for pattern choice.
        final String DEFAULT_PATTERN = "ptp_file";

        if (opt.containsKey("requirement")) {
            System.out.println("[AI] Reasoning engine : " + recommender.engineName());
            System.out.println("[AI] Requirement      : \"" + opt.get("requirement") + "\"");
        }

        Pattern pattern = catalog.byId(DEFAULT_PATTERN);
        if (pattern == null) {
            System.out.println("ERROR: pattern '" + DEFAULT_PATTERN
                    + "' not found in catalog (patterns.txt).");
            System.exit(2);
        }
        System.out.println("[AI] Pattern          : " + pattern.id
                + "  (" + pattern.title + ")");

        String subsys = opt.get("subsys"), app = opt.get("app"),
               func = opt.get("func"),     ndm = opt.getOrDefault("ndm", "NONE");
        if (blank(subsys) || blank(app) || blank(func)) {
            System.out.println("ERROR: pattern '" + pattern.id
                    + "' requires --subsys, --app and --func.");
            System.exit(2);
        }

        // Field mappings are ALWAYS inferred 1:1 (position-based) from a sample
        // input XML + sample output JSON. Both default to the committed samples
        // under test-data/, so the tool needs no separate mapping document.
        String xmlPath  = opt.getOrDefault("sample-input",  sampleInputXml(home).toString());
        String jsonPath = opt.getOrDefault("sample-output", sampleOutputJson(home).toString());

        Path xmlFile  = Paths.get(xmlPath);
        Path jsonFile = Paths.get(jsonPath);
        if (!Files.exists(xmlFile)) {
            System.out.println("ERROR: sample input XML not found: " + xmlFile);
            System.exit(2);
        }
        if (!Files.exists(jsonFile)) {
            System.out.println("ERROR: sample output JSON not found: " + jsonFile);
            System.exit(2);
        }

        System.out.println("[AI] Inferring field mappings from sample files:");
        System.out.println("       - Input  : " + xmlFile.getFileName());
        System.out.println("       - Output : " + jsonFile.getFileName());

        Map<String, String> inferredMappings =
                MappingInferencer.inferMappingsFromFiles(xmlPath, jsonPath);
        MappingDocument mappingDoc = MappingInferencer.toMappingDocument(inferredMappings);

        System.out.println("[AI] Inferred " + mappingDoc.size() + " field mappings (1:1 position-based)");

        Map<String, String> repl = Generator.buildReplacements(subsys, app, func, ndm);
        String appProject = Generator.applyTokens(pattern.appProject, repl);

        Path templateRoot = repoRoot.resolve(pattern.templateDir.replace('/',
                java.io.File.separatorChar));
        if (!Files.isDirectory(templateRoot)) {
            System.out.println("ERROR: template not found: " + templateRoot);
            System.exit(2);
        }
        // default output: a sibling of the project root, e.g. .../git/FlowSmith_Generated
        Path outRoot = opt.containsKey("out")
                ? Paths.get(opt.get("out"))
                : repoRoot.getParent().resolve("FlowSmith_Generated");
        Files.createDirectories(outRoot);

        System.out.println("[AI] Generating standardized ACE application...");
        System.out.println("       - org naming convention : " + appProject);
        System.out.println("       - enterprise subflows    : ED6 logging / error-handling framework");
        System.out.println("       - env params             : DEV / ACC / PRO");
        if (mappingDoc != null) {
            System.out.println("       - field mappings         : XML to JSON transformation");
        }

        Generator.Result res = Generator.generate(templateRoot, outRoot, appProject, repl,
                opt.containsKey("force") || true, mappingDoc);

        System.out.println("\n[AI] Done - developer review required.");
        System.out.println("       tokens applied : " + repl);
        System.out.println("       output project : " + res.outputProject);
        System.out.println("       substitutions  : " + res.tokenHits + " token hits across "
                + res.filesChanged + " files");
        if (mappingDoc != null) {
            System.out.println("       field mappings : " + mappingDoc.size() + " mappings injected into ESQL");
        }
        System.out.println("\nGenerated artifacts:");
        for (String t : res.tree) System.out.println("       " + t);
        System.out.println("\nNext: ACE Toolkit > File > Import > Existing Projects, root = "
                + res.outputProject.getParent());
        System.out.println("      Review the .msgflow / .esql, then build the BAR and deploy.\n");
    }

    // -------------------------------------------------------------- selftest
    /**
     * Runs the mapping pipeline (CSV parse -> ESQL generation) and asserts the
     * output, with NO ACE runtime required. This is Tier-1 testing: it verifies
     * FlowSmith produces correct ESQL from a mapping document, and guards the
     * source/target root (source must read InputRoot, target must write OutputRoot).
     */
    private static void doSelfTest(Path home) throws Exception {
        banner();
        System.out.println("[TEST] FlowSmith mapping self-test (no ACE runtime required)\n");
        int pass = 0, fail = 0;

        // --- Test 1: mappings are inferred 1:1 from the sample XML + JSON ---
        Path xml  = sampleInputXml(home);
        Path json = sampleOutputJson(home);
        if (!Files.exists(xml) || !Files.exists(json)) {
            System.out.println("  [FAIL] sample files not found: " + xml + " / " + json);
            System.exit(1);
        }
        MappingDocument doc = MappingInferencer.toMappingDocument(
                MappingInferencer.inferMappingsFromFiles(xml.toString(), json.toString()));
        boolean t1 = doc.size() >= 6;
        System.out.println((t1 ? "  [PASS]" : "  [FAIL]")
                + " Test 1: sample-based mapping inferred (" + doc.size() + " mappings)");
        if (t1) pass++; else fail++;

        // --- Test 2: ESQL generation produces the expected SET statements ---
        String esql = ESQLMappingGenerator.generateMappingCode(
                doc.getMappings(), "XMLNSC", "JSON");
        String[] expected = {
            "SET OutputRoot.JSON.Data.customer.customerId = InputRoot.XMLNSC.customer.id;",
            "SET OutputRoot.JSON.Data.customer.fullName = InputRoot.XMLNSC.customer.name;",
            "SET OutputRoot.JSON.Data.order.id = InputRoot.XMLNSC.order.orderId;",
        };
        for (String want : expected) {
            boolean ok = esql.contains(want);
            System.out.println((ok ? "  [PASS]" : "  [FAIL]")
                    + " Test 2: generated ESQL contains -> " + want);
            if (ok) pass++; else fail++;
        }

        // --- Test 3: guard the source/target root bug ---
        // Source fields must read from InputRoot; a source rooted at OutputRoot is the bug.
        boolean t3 = esql.contains("= InputRoot.XMLNSC.") && !esql.contains("= OutputRoot.XMLNSC.");
        System.out.println((t3 ? "  [PASS]" : "  [FAIL]")
                + " Test 3: source reads InputRoot (not OutputRoot)");
        if (t3) pass++; else fail++;

        System.out.println("\n[TEST] Result: " + pass + " passed, " + fail + " failed.");
        System.out.println("[TEST] (Runtime XML->JSON transform requires deploying the generated");
        System.out.println("        flow to an ACE integration server - see TESTING_GUIDE.md.)\n");
        System.exit(fail == 0 ? 0 : 1);
    }

    // -------------------------------------------------------------- filetest
    /**
     * End-to-end file-drop test against a DEPLOYED ptp file flow.
     *
     * Reads the generated project's .msgflow to find the flow's input/output
     * directories (rewritten to C:\Temp\test\<projectName>\... by the build
     * step), drops the committed sample input XML into the input folder, then
     * polls the output folder for the JSON the deployed flow produces.
     *
     *   java -jar flowsmith.jar filetest --subsys X --app Y --func Z [--ndm N]
     *     [--workspace DIR] [--input existing.xml] [--timeout 30]
     */
    private static void doFileTest(Catalog catalog, Path home, Map<String, String> opt) throws Exception {
        banner();
        if (blank(opt.get("subsys")) || blank(opt.get("app")) || blank(opt.get("func"))) {
            System.out.println("ERROR: filetest requires --subsys, --app and --func");
            System.exit(2);
        }
        String projectName = computeProjectName(catalog, opt);
        int timeoutSec = Integer.parseInt(opt.getOrDefault("timeout", "30"));

        // Locate the generated project and its message flow.
        Path workspace = opt.containsKey("workspace")
                ? Paths.get(opt.get("workspace"))
                : Paths.get(System.getProperty("user.home"), "git", "f", "FlowSmith_Generated");
        Path projectDir = workspace.resolve(projectName);
        if (!Files.isDirectory(projectDir)) {
            System.out.println("ERROR: generated project not found: " + projectDir);
            System.out.println("       Run 'Build Application' first (it generates + deploys).");
            System.exit(2);
        }
        Path msgflow = findMsgflow(projectDir);
        if (msgflow == null) {
            System.out.println("ERROR: no .msgflow with an inputDirectory found under " + projectDir);
            System.exit(2);
        }

        String flow = new String(Files.readAllBytes(msgflow), StandardCharsets.UTF_8);
        String rawIn  = attr(flow, "inputDirectory");
        String rawOut = attr(flow, "outDirectory");
        if (rawIn == null) {
            System.out.println("ERROR: could not read inputDirectory from " + msgflow.getFileName());
            System.exit(2);
        }
        // Fallback rewrite in case the on-disk flow still carries the /mgmt/data/esb prefix.
        Path inDir  = Paths.get(rewriteEsbPath(rawIn, projectName));
        Path outDir = rawOut != null ? Paths.get(rewriteEsbPath(rawOut, projectName))
                                     : inDir.getParent();

        System.out.println("[TEST] File-drop test for project: " + projectName);
        System.out.println("       msgflow    : " + msgflow.getFileName());
        System.out.println("       input  dir : " + inDir);
        System.out.println("       output dir : " + outDir);

        // Input XML: --input override, else the committed sample used to generate the flow.
        Path xmlFile = opt.containsKey("input") ? Paths.get(opt.get("input")) : sampleInputXml(home);
        if (!Files.exists(xmlFile)) {
            System.out.println("ERROR: input XML not found: " + xmlFile);
            System.exit(2);
        }
        byte[] xml = Files.readAllBytes(xmlFile);
        System.out.println("       input xml  : " + xmlFile.getFileName());

        // Ensure folders exist (the deployed flow should already own inDir).
        if (!Files.isDirectory(inDir)) {
            System.out.println("[TEST] WARNING: input dir does not exist - creating it. "
                    + "Is the flow deployed and polling this folder?");
            Files.createDirectories(inDir);
        }
        Files.createDirectories(outDir);

        // Snapshot the output folder BEFORE dropping the input.
        Set<String> before = listFileNamesRecursive(outDir);

        // Drop the input file with a unique name.
        String dropName = "flowsmith-test-" + System.currentTimeMillis() + ".xml";
        Path dropped = inDir.resolve(dropName);
        Files.write(dropped, xml);
        System.out.println("\n[TEST] Dropped input file: " + dropped);
        System.out.println("[TEST] Waiting for the deployed flow to produce output (timeout "
                + timeoutSec + "s)...");

        // Poll the output folder (recursively) for a NEW file.
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        Path produced = null;
        while (System.currentTimeMillis() < deadline) {
            Set<String> now = listFileNamesRecursive(outDir);
            now.removeAll(before);
            if (!now.isEmpty()) {
                produced = outDir.resolve(now.iterator().next());
                break;
            }
            Thread.sleep(1000);
            System.out.print(".");
        }
        System.out.println();

        // Report.
        if (produced != null) {
            Thread.sleep(500); // let the flow finish writing
            System.out.println("\n[TEST] ============================================");
            System.out.println("[TEST]  OUTPUT RECEIVED: " + produced);
            System.out.println("[TEST] ============================================\n");
            System.out.println(new String(Files.readAllBytes(produced), StandardCharsets.UTF_8));
            System.out.println("\n[TEST] Review the JSON above against the sample output - "
                    + "PASS if the fields match.\n");
        } else {
            System.out.println("[TEST] NO OUTPUT within " + timeoutSec + "s. Check that:");
            System.out.println("        - the flow is DEPLOYED and STARTED on the integration server");
            System.out.println("        - it is polling " + inDir);
            System.out.println("        - the FileInput message domain is XMLNSC (so mappings populate)");
            System.out.println("        - look in the backout folder / server logs for errors\n");
            System.exit(1);
        }
    }

    /** Relative names of all files (recursively) under dir; empty if dir is missing. */
    private static Set<String> listFileNamesRecursive(Path dir) {
        Set<String> names = new HashSet<>();
        if (!Files.isDirectory(dir)) return names;
        try (java.util.stream.Stream<Path> s = Files.walk(dir)) {
            s.filter(Files::isRegularFile).forEach(p -> names.add(dir.relativize(p).toString()));
        } catch (Exception e) { /* dir vanished / unreadable - treat as empty */ }
        return names;
    }

    /** Recursively find the first *.msgflow under dir that declares an inputDirectory. */
    private static Path findMsgflow(Path dir) throws Exception {
        try (java.util.stream.Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".msgflow"))
                    .filter(p -> {
                        try {
                            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8)
                                    .contains("inputDirectory");
                        } catch (Exception e) { return false; }
                    })
                    .findFirst().orElse(null);
        }
    }

    /** Read an XML attribute value like inputDirectory="..." (first match), or null. */
    private static String attr(String xml, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile(name + "=\"([^\"]*)\"").matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    /** Canonical path rewrite: /mgmt/data/esb -> C:/Temp/test/<projectName> (suffix kept). */
    private static String rewriteEsbPath(String path, String projectName) {
        return path.replace("/mgmt/data/esb", "C:/Temp/test/" + projectName);
    }

    /** The ptp project name for the given tokens (same naming 'generate' uses). */
    private static String computeProjectName(Catalog catalog, Map<String, String> opt) {
        Pattern p = catalog.byId("ptp_file");
        Map<String, String> repl = Generator.buildReplacements(
                opt.get("subsys"), opt.get("app"), opt.get("func"), opt.getOrDefault("ndm", "NONE"));
        return Generator.applyTokens(p.appProject, repl);
    }

    private static Path sampleInputXml(Path home) {
        return home.resolve("test-data").resolve("sample-input.xml");
    }

    private static Path sampleOutputJson(Path home) {
        return home.resolve("test-data").resolve("expected-output.json");
    }

    // ------------------------------------------------------------- helpers
    private static void banner() {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println("  ACE FlowSmith AI  -  Intelligent Integration Generator");
        System.out.println("  agent loop: perceive -> reason -> act -> human review");
        System.out.println("  build: " + BUILD);
        System.out.println("=========================================================");
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    m.put(key, args[++i]);
                } else {
                    m.put(key, "true");
                }
            }
        }
        return m;
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }

    private static String stars(int score) {
        int n = Math.min(score, 5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('*');
        return sb.toString();
    }

    private static String dashes(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('-');
        return sb.toString();
    }

    /** Folder that contains the running jar (or classes), so paths are portable. */
    private static Path jarDir() {
        try {
            URI uri = FlowSmith.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI();
            Path p = Paths.get(uri);
            return Files.isDirectory(p) ? p : p.getParent();
        } catch (Exception e) {
            return Paths.get("").toAbsolutePath();
        }
    }

    private static void usage() {
        System.out.println("ACE FlowSmith AI - usage:");
        System.out.println("  java -jar flowsmith.jar list");
        System.out.println("  java -jar flowsmith.jar selftest   (verify mapping->ESQL, no ACE needed)");
        System.out.println("  java -jar flowsmith.jar recommend \"<requirement>\"");
        System.out.println("  java -jar flowsmith.jar generate --subsys X --app Y --func Z "
                + "[--ndm N] [--out DIR] [--sample-input XML --sample-output JSON]");
        System.out.println("  java -jar flowsmith.jar filetest --subsys X --app Y --func Z "
                + "[--ndm N] [--workspace DIR] [--input XML] [--timeout SEC]");
        System.out.println("\nField mappings:");
        System.out.println("  Mappings are inferred 1:1 (position-based) from a sample input XML");
        System.out.println("  and a sample output JSON:");
        System.out.println("    --sample-input  <file.xml>   Sample input XML  (default: test-data/sample-input.xml)");
        System.out.println("    --sample-output <file.json>  Sample output JSON (default: test-data/expected-output.json)");
        System.out.println("  The same sample input XML is reused by 'filetest' to exercise the deployed flow.");
    }
}
