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
 *   generate --pattern <id>      --subsys X --app Y --func Z [--ndm N] [--out DIR] [--mapping FILE]
 *   generate --requirement "..." --subsys X --app Y --func Z [--ndm N] [--out DIR] [--mapping FILE]
 *
 * The reasoning engine is pluggable - see {@link Recommender} (the AI seam).
 * Mapping support: Use --mapping to provide a CSV file with field mappings for XML-to-JSON conversion.
 */
public class FlowSmith {

    /** Build marker - bump this when rebuilding so you can confirm the running jar is current. */
    private static final String BUILD = "2026-07-08b (CSV mappings, PTP default)";

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
            case "generate":   doGenerate(recommender, catalog, repoRoot, opt); break;
            case "selftest":   doSelfTest(home); break;
            case "filetest":   doFileTest(home, opt); break;
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
    private static void doGenerate(Recommender recommender, Catalog catalog, Path repoRoot,
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

        // Load mapping document if provided
        MappingDocument mappingDoc = null;
        if (opt.containsKey("mapping")) {
            String mappingPath = opt.get("mapping");
            // Allow "NONE" to skip mapping (useful for launch configs)
            if (!mappingPath.equalsIgnoreCase("NONE")) {
                Path mappingFile = Paths.get(mappingPath);
                if (!Files.exists(mappingFile)) {
                    System.out.println("ERROR: mapping file not found: " + mappingFile);
                    System.exit(2);
                }
                System.out.println("[AI] Loading mapping document: " + mappingFile.getFileName());
                mappingDoc = MappingDocument.load(mappingFile);
                System.out.println("[AI] Loaded " + mappingDoc.size() + " field mappings");
            }
        }

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

        // --- Test 1: CSV mapping document parses ---
        Path csv = home.resolve("example-mapping.csv");
        if (!Files.exists(csv)) {
            System.out.println("  [FAIL] example-mapping.csv not found at " + csv);
            System.exit(1);
        }
        MappingDocument doc = MappingDocument.load(csv);
        boolean t1 = doc.size() >= 6;
        System.out.println((t1 ? "  [PASS]" : "  [FAIL]")
                + " Test 1: CSV mapping parses (" + doc.size() + " mappings loaded)");
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
     * Generates a sample XML dynamically from the mapping document, drops it into
     * the flow's input folder, then polls the output folder for the transformed
     * result. The deployed flow (already polling the folder) does the transform;
     * this command only drives the drop and watches for output.
     *
     *   java -jar flowsmith.jar filetest --app CUST --mapping example-mapping.csv
     *     [--base C:\temp\test] [--indir in] [--outdir out] [--timeout 30]
     *     [--input existing.xml]
     */
    private static void doFileTest(Path home, Map<String, String> opt) throws Exception {
        banner();
        String app = opt.get("app");
        if (blank(app)) {
            System.out.println("ERROR: filetest requires --app <name>");
            System.exit(2);
        }

        String base = opt.getOrDefault("base", "C:\\temp\\test");
        Path appDir = Paths.get(base, app);
        Path inDir  = appDir.resolve(opt.getOrDefault("indir", "in"));
        Path outDir = appDir.resolve(opt.getOrDefault("outdir", "out"));
        int timeoutSec = Integer.parseInt(opt.getOrDefault("timeout", "30"));

        System.out.println("[TEST] File-drop test for application: " + app);
        System.out.println("       input  dir : " + inDir);
        System.out.println("       output dir : " + outDir);

        // 1. Build the input XML - dynamically from the mapping, or from --input
        String xml;
        if (opt.containsKey("input")) {
            Path inFile = Paths.get(opt.get("input"));
            if (!Files.exists(inFile)) {
                System.out.println("ERROR: --input file not found: " + inFile);
                System.exit(2);
            }
            xml = new String(Files.readAllBytes(inFile), StandardCharsets.UTF_8);
            System.out.println("       input xml  : " + inFile.getFileName() + " (provided)");
        } else {
            String mappingPath = opt.getOrDefault("mapping",
                    home.resolve("example-mapping.csv").toString());
            Path mappingFile = Paths.get(mappingPath);
            if (!Files.exists(mappingFile)) {
                System.out.println("ERROR: mapping file not found: " + mappingFile);
                System.exit(2);
            }
            MappingDocument doc = MappingDocument.load(mappingFile);
            xml = generateSampleXml(doc.getMappings());
            System.out.println("       input xml  : generated from " + mappingFile.getFileName()
                    + " (" + doc.size() + " fields)");
        }

        // 2. Ensure folders exist (the flow should already own inDir if deployed)
        if (!Files.isDirectory(inDir)) {
            System.out.println("[TEST] WARNING: input dir does not exist - creating it. "
                    + "Is the flow deployed and polling this folder?");
            Files.createDirectories(inDir);
        }
        Files.createDirectories(outDir);

        // 3. Snapshot the output folder BEFORE dropping the input
        Set<String> before = listFileNames(outDir);

        // 4. Drop the input file with a unique name
        String dropName = "flowsmith-test-" + System.currentTimeMillis() + ".xml";
        Path dropped = inDir.resolve(dropName);
        Files.write(dropped, xml.getBytes(StandardCharsets.UTF_8));
        System.out.println("\n[TEST] Dropped input file: " + dropped);
        System.out.println("[TEST] Waiting for the deployed flow to produce output (timeout "
                + timeoutSec + "s)...");

        // 5. Poll the output folder for a NEW file
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        Path produced = null;
        while (System.currentTimeMillis() < deadline) {
            Set<String> now = listFileNames(outDir);
            now.removeAll(before);
            if (!now.isEmpty()) {
                produced = outDir.resolve(now.iterator().next());
                break;
            }
            Thread.sleep(1000);
            System.out.print(".");
        }
        System.out.println();

        // 6. Report
        if (produced != null) {
            Thread.sleep(500); // let the flow finish writing
            System.out.println("\n[TEST] ============================================");
            System.out.println("[TEST]  OUTPUT RECEIVED: " + produced);
            System.out.println("[TEST] ============================================\n");
            System.out.println(new String(Files.readAllBytes(produced), StandardCharsets.UTF_8));
            System.out.println("\n[TEST] Review the JSON above against your mapping - "
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

    /** File names (not dirs) currently in a folder; empty set if the folder is missing. */
    private static Set<String> listFileNames(Path dir) {
        Set<String> names = new HashSet<>();
        File[] files = dir.toFile().listFiles(File::isFile);
        if (files != null) for (File f : files) names.add(f.getName());
        return names;
    }

    /**
     * Build a sample XML document from the SOURCE column of the mappings.
     * Each source path (e.g. "customer/id") becomes a nested element, filled
     * with a placeholder value, so the generated input always matches the
     * fields the mapping expects.
     */
    @SuppressWarnings("unchecked")
    private static String generateSampleXml(List<MappingDocument.FieldMapping> mappings) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (MappingDocument.FieldMapping m : mappings) {
            String path = m.sourceField.replace('/', '.').replaceAll("^\\.|\\.$", "");
            if (path.isEmpty()) continue;
            String[] parts = path.split("\\.");
            Map<String, Object> cur = root;
            for (int i = 0; i < parts.length - 1; i++) {
                Object child = cur.get(parts[i]);
                if (!(child instanceof Map)) {
                    Map<String, Object> next = new LinkedHashMap<>();
                    cur.put(parts[i], next);
                    child = next;
                }
                cur = (Map<String, Object>) child;
            }
            String leaf = parts[parts.length - 1];
            if (!cur.containsKey(leaf)) cur.put(leaf, "SAMPLE_" + leaf);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<root>\n");
        writeXmlNodes(sb, root, 1);
        sb.append("</root>\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeXmlNodes(StringBuilder sb, Map<String, Object> node, int depth) {
        StringBuilder ind = new StringBuilder();
        for (int i = 0; i < depth; i++) ind.append("    ");
        for (Map.Entry<String, Object> e : node.entrySet()) {
            if (e.getValue() instanceof Map) {
                sb.append(ind).append("<").append(e.getKey()).append(">\n");
                writeXmlNodes(sb, (Map<String, Object>) e.getValue(), depth + 1);
                sb.append(ind).append("</").append(e.getKey()).append(">\n");
            } else {
                sb.append(ind).append("<").append(e.getKey()).append(">")
                  .append(e.getValue()).append("</").append(e.getKey()).append(">\n");
            }
        }
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
        System.out.println("  java -jar flowsmith.jar filetest --app <name> [--mapping FILE.csv] "
                + "[--base DIR] [--timeout SEC]");
        System.out.println("  java -jar flowsmith.jar recommend \"<requirement>\"");
        System.out.println("  java -jar flowsmith.jar generate --pattern <id> "
                + "--subsys X --app Y --func Z [--ndm N] [--out DIR] [--mapping FILE.csv]");
        System.out.println("  java -jar flowsmith.jar generate --requirement \"...\" "
                + "--subsys X --app Y --func Z [--mapping FILE.csv]");
        System.out.println("\nMapping Document:");
        System.out.println("  Use --mapping to provide a CSV file (.csv) with field mappings.");
        System.out.println("  Format: Column A = Source field (XML), Column B = Target field (JSON)");
        System.out.println("  Example: customer/name -> customer.name");
    }
}
