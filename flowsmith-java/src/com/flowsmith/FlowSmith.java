package com.flowsmith;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        System.out.println(">>>>> FLOWSMITH NEW CODE ACTIVE - build 2026-07-08c - if you see this line, the rebuild worked <<<<<");
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
