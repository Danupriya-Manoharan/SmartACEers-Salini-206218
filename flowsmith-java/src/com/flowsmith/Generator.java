package com.flowsmith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * The "act" step of the agent: instantiate a chosen pattern into a ready-to-
 * import ACE application by copying the template and substituting the org
 * tokens (SUBSYS / APPNM / FUNCNM / NDMNM) in folder names, file names AND
 * file contents - while leaving shared-framework references untouched.
 */
public class Generator {

    /** Extensions treated as text (contents rewritten). */
    private static final Set<String> TEXT_EXT = new HashSet<>(Arrays.asList(
            ".msgflow", ".esql", ".subflow", ".prop", ".properties", ".project",
            ".descriptor", ".xml", ".map", ".mset", ".mxsd", ".xsd", ".json", ".txt"));
    /** Extensionless config files splitext can't classify. */
    private static final Set<String> TEXT_NAMES = new HashSet<>(Arrays.asList(
            ".project", ".classpath"));

    public static class Result {
        public Path outputProject;
        public String appProjectName;
        public int filesChanged;
        public int tokenHits;
        public List<String> tree = new ArrayList<>();
    }

    /**
     * @param templateRoot absolute path to the pattern template folder
     * @param outRoot      absolute path of the output directory
     * @param appProject   substituted application name (output container name)
     * @param repl         token -> value (e.g. SUBSYS -> XAJ)
     * @param force        overwrite existing output
     * @param mappingDoc   optional mapping document for field transformations
     */
    public static Result generate(Path templateRoot, Path outRoot, String appProject,
                                  Map<String, String> repl, boolean force, MappingDocument mappingDoc) throws IOException {
        Result res = new Result();
        res.appProjectName = appProject;
        Path dest = outRoot.resolve(appProject);

        if (Files.exists(dest)) {
            if (!force) throw new IOException(dest + " already exists (use --force to overwrite)");
            deleteTree(dest);
        }
        Files.createDirectories(dest);

        // Single pass: walk the template, substitute tokens in the RELATIVE PATH
        // (renames folders + files) and in text-file CONTENTS, then write.
        try (Stream<Path> walk = Files.walk(templateRoot)) {
            List<Path> all = new ArrayList<>();
            walk.forEach(all::add);
            for (Path src : all) {
                String rel = templateRoot.relativize(src).toString();
                if (rel.isEmpty()) continue;
                String name = src.getFileName().toString();
                if (name.equals(".DS_Store") || rel.contains(".git")) continue;

                String relSub = applyTokens(rel, repl);
                Path target = dest.resolve(relSub.replace('/', java.io.File.separatorChar));

                if (Files.isDirectory(src)) {
                    Files.createDirectories(target);
                } else {
                    if (target.getParent() != null) Files.createDirectories(target.getParent());
                    if (isTextFile(name)) {
                        String content = new String(Files.readAllBytes(src), StandardCharsets.UTF_8);
                        int[] hits = new int[1];
                        String out = applyTokensCounting(content, repl, hits);
                        
                        // Inject field mappings into ESQL files if mapping document provided
                        if (mappingDoc != null && !mappingDoc.isEmpty() && name.endsWith(".esql") &&
                            (name.contains("Compute") || name.contains("Map"))) {
                            out = ESQLMappingGenerator.injectMappingsIntoTemplate(
                                out, mappingDoc.getMappings(), "XMLNSC", "JSON");
                            res.filesChanged++; // Count mapping injection as a change
                        }
                        
                        Files.write(target, out.getBytes(StandardCharsets.UTF_8));
                        if (hits[0] > 0) { res.tokenHits += hits[0]; res.filesChanged++; }
                    } else {
                        Files.copy(src, target);
                    }
                    res.tree.add(dest.relativize(target).toString());
                }
            }
        }
        res.tree.sort(Comparator.naturalOrder());
        res.outputProject = dest;
        return res;
    }

    /** SUBSYS/APPNM/FUNCNM/NDMNM -> values. NONE means "skip NDM". */
    public static Map<String, String> buildReplacements(String subsys, String app,
                                                        String func, String ndm) {
        Map<String, String> repl = new LinkedHashMap<>();
        if (notBlank(subsys)) repl.put("SUBSYS", subsys.toUpperCase());
        if (notBlank(app))    repl.put("APPNM", app);
        if (notBlank(func))   repl.put("FUNCNM", func);
        if (notBlank(ndm) && !ndm.trim().equalsIgnoreCase("NONE")) {
            repl.put("NDMNM", ndm.toUpperCase());
        }
        return repl;
    }

    public static String applyTokens(String s, Map<String, String> repl) {
        for (Map.Entry<String, String> e : repl.entrySet()) {
            s = s.replace(e.getKey(), e.getValue());
        }
        return s;
    }

    private static String applyTokensCounting(String s, Map<String, String> repl, int[] hits) {
        for (Map.Entry<String, String> e : repl.entrySet()) {
            String k = e.getKey();
            int from = 0, idx;
            while ((idx = s.indexOf(k, from)) >= 0) { hits[0]++; from = idx + k.length(); }
            s = s.replace(k, e.getValue());
        }
        return s;
    }

    private static boolean isTextFile(String name) {
        if (TEXT_NAMES.contains(name)) return true;
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        return TEXT_EXT.contains(name.substring(dot).toLowerCase());
    }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) { }
            });
        }
    }
}
