package com.flowsmith.automation;
 
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
 
/**
* ACE FlowSmith AI - Automated Build &amp; Deployment Tool
*
* Two modes:
*
* 1. One-button "Build Application" (flag mode) - generate the ACE project from
*    the committed sample XML/JSON, then rewrite paths, build the BAR and deploy:
*      java ... ACEDeployer --subsys X --app Y --func Z [--ndm N] --jar &lt;flowsmith.jar&gt;
*                           [--qmgr QM] [--node NODE] [--server SRV]
*
* 2. Deploy-only (legacy positional mode) - project already generated:
*      java ... ACEDeployer <projectName> [queueManager] [integrationNode] [integrationServer]
*
* Deployment workflow: (generate ->) rewrite flow dirs -> build BAR -> start
* Queue Manager -> start Integration Node -> deploy BAR.
*/
public class ACEDeployer {
    private static final String DEFAULT_QUEUE_MANAGER = "MB8QMGR";
    private static final String DEFAULT_INTEGRATION_NODE = "Test_node_test";
    private static final String DEFAULT_INTEGRATION_SERVER = "default";
    private static final String DEFAULT_WORKSPACE = System.getProperty("user.home") + "\\git\\f\\FlowSmith_Generated";
    private static String ACE_TOOLKIT_PATH = null;
    private String projectName;
    private String queueManager;
    private String integrationNode;
    private String integrationServer;
    private String workspace;
    private String barOutputDir;
    private String barFile;

    /** Local test root the flow's /mgmt/data/esb directories are rewritten to. */
    private final String testRoot;

    public ACEDeployer(String projectName, String queueManager, String integrationNode, String integrationServer) {
        this.projectName = projectName;
        this.queueManager = queueManager != null ? queueManager : DEFAULT_QUEUE_MANAGER;
        this.integrationNode = integrationNode != null ? integrationNode : DEFAULT_INTEGRATION_NODE;
        this.integrationServer = integrationServer != null ? integrationServer : DEFAULT_INTEGRATION_SERVER;
        this.workspace = DEFAULT_WORKSPACE;
        this.barOutputDir = workspace + "\\BAR_Files";
        this.barFile = barOutputDir + "\\" + projectName + ".bar";
        this.testRoot = "C:/Temp/test/" + projectName;
    }
    public static void main(String[] args) {
        printBanner();
        String projectName = null;
        String queueManager = null;
        String integrationNode = null;
        String integrationServer = null;
        // Generate inputs (flag mode only); null means "deploy only".
        String subsys = null, app = null, func = null, ndm = "NONE", flowsmithJar = null;

        if (args.length > 0 && args[0].startsWith("--")) {
            // Flag mode: one-button build (generate + deploy).
            Map<String, String> flags = parseFlags(args);
            subsys = flags.get("subsys");
            app = flags.get("app");
            func = flags.get("func");
            ndm = flags.getOrDefault("ndm", "NONE");
            flowsmithJar = flags.get("jar");
            queueManager = flags.get("qmgr");
            integrationNode = flags.get("node");
            integrationServer = flags.get("server");
            if (isBlank(subsys) || isBlank(app) || isBlank(func)) {
                System.err.println("ERROR: --subsys, --app and --func are required");
                System.exit(2);
            }
            projectName = subsys.toUpperCase() + "_PTP_" + app + "_" + func + "_FIL";
        } else if (args.length > 0) {
            // Legacy positional mode: deploy an already-generated project.
            projectName = args[0];
            queueManager = args.length > 1 ? args[1] : null;
            integrationNode = args.length > 2 ? args[2] : null;
            integrationServer = args.length > 3 ? args[3] : null;
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter Project Name (e.g., XAJ_PUB_TLMTF_FINANCING_FIL): ");
            projectName = scanner.nextLine().trim();
            if (projectName.isEmpty()) {
                System.err.println("ERROR: Project name is required");
                System.exit(1);
            }
            System.out.print("Enter Queue Manager Name [default: " + DEFAULT_QUEUE_MANAGER + "]: ");
            String input = scanner.nextLine().trim();
            queueManager = input.isEmpty() ? null : input;
            System.out.print("Enter Integration Node Name [default: " + DEFAULT_INTEGRATION_NODE + "]: ");
            input = scanner.nextLine().trim();
            integrationNode = input.isEmpty() ? null : input;
            System.out.print("Enter Integration Server Name [default: " + DEFAULT_INTEGRATION_SERVER + "]: ");
            input = scanner.nextLine().trim();
            integrationServer = input.isEmpty() ? null : input;
            scanner.close();
        }
        try {
            ACE_TOOLKIT_PATH = detectACEToolkit();
            System.out.println("✓ Detected ACE Toolkit: " + ACE_TOOLKIT_PATH);
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            System.exit(1);
        }
        ACEDeployer deployer = new ACEDeployer(projectName, queueManager, integrationNode, integrationServer);
        try {
            deployer.printConfiguration();
            // Flag mode: generate the ACE project first (one-button build).
            if (subsys != null) {
                deployer.generateApplication(flowsmithJar, subsys, app, func, ndm);
            }
            deployer.deploy();
        } catch (Exception e) {
            System.err.println("\nERROR: Deployment failed");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Parse "--key value" / "--flag" style arguments into a map. */
    private static Map<String, String> parseFlags(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
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

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    /**
     * Run FlowSmith 'generate' in a child JVM to build the ACE project from the
     * committed sample XML/JSON before deployment. First step of the one-button
     * "Build Application" flow - pure Java orchestration, no batch script.
     */
    private void generateApplication(String flowsmithJar, String subsys, String app,
                                     String func, String ndm) throws Exception {
        System.out.println();
        System.out.println("[Generate] Generating ACE application from sample XML/JSON...");
        System.out.println("========================================================================");
        if (isBlank(flowsmithJar)) {
            throw new Exception("--jar <path to flowsmith.jar> is required in build mode");
        }
        if (!new File(flowsmithJar).exists()) {
            throw new Exception("flowsmith.jar not found: " + flowsmithJar);
        }
        // Use the same JVM that is running this deployer to run the jar.
        String javaExe = System.getProperty("java.home") + File.separator + "bin"
                + File.separator + "java";
        String command = String.format(
            "\"%s\" -jar \"%s\" generate --subsys %s --app %s --func %s --ndm %s",
            javaExe, flowsmithJar, subsys, app, func, ndm);
        System.out.println("Executing: " + command);
        int exitCode = executeCommand(command);
        if (exitCode != 0) {
            throw new Exception("Generation failed (exit code: " + exitCode + ")");
        }
        System.out.println("SUCCESS: application generated");
    }
    private static String detectACEToolkit() throws Exception {
        String baseDir = "C:\\Program Files\\IBM\\ACE";
        File aceDir = new File(baseDir);
        if (!aceDir.exists()) {
            throw new Exception("ACE installation directory not found: " + baseDir);
        }
        // Look for version directories (12.0, 13.0, 14.0, etc.)
        String[] versions = {"14.0", "13.0", "12.0", "11.0"};
        for (String version : versions) {
            File versionDir = new File(aceDir, version);
            if (versionDir.exists() && versionDir.isDirectory()) {
                // Verify it has the server/bin directory
                File binDir = new File(versionDir, "server\\bin");
                if (binDir.exists()) {
                    return versionDir.getAbsolutePath();
                }
            }
        }
        // If no standard version found, look for any subdirectory
        File[] subdirs = aceDir.listFiles(File::isDirectory);
        if (subdirs != null && subdirs.length > 0) {
            for (File subdir : subdirs) {
                File binDir = new File(subdir, "server\\bin");
                if (binDir.exists()) {
                    return subdir.getAbsolutePath();
                }
            }
        }
        throw new Exception("No valid ACE Toolkit version found in: " + baseDir);
    }
    private static void printBanner() {
        System.out.println();
        System.out.println("========================================================================");
        System.out.println("  ACE FlowSmith AI - Automated Deployment");
        System.out.println("========================================================================");
        System.out.println();
    }
    private void printConfiguration() {
        System.out.println("Configuration:");
        System.out.println("  Project Name      : " + projectName);
        System.out.println("  Queue Manager     : " + queueManager);
        System.out.println("  Integration Node  : " + integrationNode);
        System.out.println("  Integration Server: " + integrationServer);
        System.out.println("  Workspace         : " + workspace);
        System.out.println("  BAR File          : " + barFile);
        System.out.println();
    }
    public void deploy() throws Exception {
        // Create BAR output directory if it doesn't exist
        File barDir = new File(barOutputDir);
        if (!barDir.exists()) {
            barDir.mkdirs();
        }
        // Step 0: Rewrite the flow's /mgmt/data/esb directories to the local test
        // root BEFORE packaging, so the deployed flow polls C:\Temp\test\<project>.
        rewriteMsgflowPaths();
        // Step 1: Build BAR file
        buildBarFile();
        // Step 2: Start Queue Manager
        startQueueManager();
        // Step 3: Start Integration Node
        startIntegrationNode();
        // Step 4: Deploy BAR file
        deployBarFile();
        // Verification
        verifyDeployment();
        printSuccess();
    }
    private void buildBarFile() throws Exception {
        System.out.println();
        System.out.println("[Step 1/4] Building BAR file...");
        System.out.println("========================================================================");
       /* String command = String.format(
            "mqsicreatebar -data \"%s\" -b \"%s\" -a \"%s\" -o",
            workspace, barFile, projectName
        );*/
        String command = String.format(
    		    "call \"%s\\server\\bin\\mqsiprofile.cmd\" && " +
    		    "\"%s\\server\\bin\\ibmint.exe\" package " +
    		    "--input-path \"%s\" " +
    		    "--output-bar-file \"%s\" " +
    		    "--project \"%s\"",
    		    ACE_TOOLKIT_PATH,
    		    ACE_TOOLKIT_PATH,
    		    DEFAULT_WORKSPACE + File.separator + projectName,
    		    barFile,
    		    projectName
    		);
        System.out.println("Executing: " + command);
        int exitCode = executeCommand(command);
        if (exitCode != 0) {
            throw new Exception("Failed to create BAR file (exit code: " + exitCode + ")");
        }
        System.out.println("SUCCESS: BAR file created at " + barFile);
    }
    private void startQueueManager() throws Exception {
        System.out.println();
        System.out.println("[Step 2/4] Starting Queue Manager...");
        System.out.println("========================================================================");
        // Check if Queue Manager is already running
        String checkCommand = "dspmq -m " + queueManager;
        String output = executeCommandWithOutput(checkCommand);
        if (output.contains("Running")) {
            System.out.println("Queue Manager " + queueManager + " is already running");
            return;
        }
        // Start Queue Manager
        System.out.println("Starting Queue Manager " + queueManager + "...");
        String startCommand = "strmqm " + queueManager;
        int exitCode = executeCommand(startCommand);
        if (exitCode != 0) {
            throw new Exception("Failed to start Queue Manager (exit code: " + exitCode + ")");
        }
        // Wait for Queue Manager to start
        Thread.sleep(5000);
        System.out.println("SUCCESS: Queue Manager started");
    }
    private void startIntegrationNode() throws Exception {
        System.out.println();
        System.out.println("[Step 3/4] Starting Integration Node...");
        System.out.println("========================================================================");
        // Check if Integration Node is already running
        String checkCommand = String.format(
        	    "call \"%s\\server\\bin\\mqsiprofile.cmd\" && \"%s\\server\\bin\\mqsilist\"",
        	    ACE_TOOLKIT_PATH,
        	    ACE_TOOLKIT_PATH
        	);
        String output = executeCommandWithOutput(checkCommand);
        if (output.contains(integrationNode) && output.contains("running")) {
            System.out.println("Integration Node " + integrationNode + " is already running");
            return;
        }
        // Start Integration Node
        System.out.println("Starting Integration Node " + integrationNode + "...");
        String startCommand = String.format(
        	    "call \"%s\\server\\bin\\mqsiprofile.cmd\" && \"%s\\server\\bin\\mqsistart\" %s",
        	    ACE_TOOLKIT_PATH,
        	    ACE_TOOLKIT_PATH,
        	    integrationNode
        	);
        int exitCode = executeCommand(startCommand);
        if (exitCode != 0) {
            throw new Exception("Failed to start Integration Node (exit code: " + exitCode + ")");
        }
        // Wait for Integration Node to start
        Thread.sleep(10000);
        System.out.println("SUCCESS: Integration Node started");
    }
    /**
     * Rewrite the flow's file directories for local testing BEFORE the BAR is
     * built. Every "/mgmt/data/esb" prefix in the generated project's .msgflow
     * files is replaced with "C:/Temp/test/&lt;projectName&gt;", keeping the rest
     * of the path (the "suffix") unchanged. The declared input/output folders
     * are then created so the deployed FileInput node can start polling cleanly.
     * This replaces the old mqsiapplybaroverride approach.
     */
    private void rewriteMsgflowPaths() throws Exception {
        System.out.println();
        System.out.println("[Rewrite] Rewriting flow directories for local testing...");
        System.out.println("========================================================================");
        System.out.println("Path prefix /mgmt/data/esb -> " + testRoot);

        Path projectDir = Paths.get(DEFAULT_WORKSPACE, projectName);
        if (!Files.isDirectory(projectDir)) {
            throw new Exception("Generated project not found: " + projectDir
                    + " (run FlowSmith 'generate' first)");
        }

        List<Path> flows;
        try (Stream<Path> s = Files.walk(projectDir)) {
            flows = s.filter(Files::isRegularFile)
                     .filter(p -> p.getFileName().toString().endsWith(".msgflow"))
                     .collect(Collectors.toList());
        }

        int rewritten = 0;
        for (Path flow : flows) {
            String content = new String(Files.readAllBytes(flow), StandardCharsets.UTF_8);
            if (!content.contains("/mgmt/data/esb")) continue;
            String updated = content.replace("/mgmt/data/esb", testRoot);
            Files.write(flow, updated.getBytes(StandardCharsets.UTF_8));
            rewritten++;
            System.out.println("  Rewrote: " + projectDir.relativize(flow));
            // Create the directories the flow polls / writes to.
            createDir(attrValue(updated, "inputDirectory"));
            createDir(attrValue(updated, "outDirectory"));
        }

        if (rewritten == 0) {
            System.out.println("WARNING: no .msgflow under " + projectDir
                    + " contained /mgmt/data/esb - nothing rewritten.");
        } else {
            System.out.println("SUCCESS: rewrote " + rewritten + " message flow(s)");
        }
    }

    /** Create a directory (and parents) if a non-empty path was supplied. */
    private void createDir(String dir) {
        if (dir == null || dir.trim().isEmpty()) return;
        File d = new File(dir);
        if (!d.exists() && d.mkdirs()) {
            System.out.println("  Created dir: " + dir);
        }
    }

    /** First value of an XML attribute like inputDirectory="..." in the flow, or null. */
    private static String attrValue(String xml, String name) {
        Matcher m = Pattern.compile(name + "=\"([^\"]*)\"").matcher(xml);
        return m.find() ? m.group(1) : null;
    }
    private void deployBarFile() throws Exception {
        System.out.println();
        System.out.println("[Step 4/4] Deploying BAR file to Integration Server...");
        System.out.println("========================================================================");
        String command = String.format(
        	    "call \"%s\\server\\bin\\mqsiprofile.cmd\" && " +
        	    	    "\"%s\\server\\bin\\mqsideploy\" %s -e %s -a \"%s\"",
        	    	    ACE_TOOLKIT_PATH,
        	    	    ACE_TOOLKIT_PATH,
        	    	    integrationNode,
        	    	    integrationServer,
        	    	    barFile
        	    	);
        System.out.println("Deploying " + barFile + " to " + integrationNode + ":" + integrationServer + "...");
        System.out.println("Executing: " + command);
        int exitCode = executeCommand(command);
        if (exitCode != 0) {
            throw new Exception("Failed to deploy BAR file (exit code: " + exitCode + ")");
        }
        // Wait for deployment to complete
        Thread.sleep(5000);
        System.out.println("SUCCESS: BAR file deployed");
    }
    private void verifyDeployment() throws Exception {
        System.out.println();
        System.out.println("[Verification] Checking deployment status...");
        System.out.println("========================================================================");
        String command = String.format(
        		"call \"%s\\server\\bin\\mqsiprofile.cmd\" && \"%s\\server\\bin\\mqsilist\" %s -e %s -d 2",
        		ACE_TOOLKIT_PATH,ACE_TOOLKIT_PATH,integrationNode, integrationServer
        );
        executeCommand(command);
    }
    private void printSuccess() {
        System.out.println();
        System.out.println("========================================================================");
        System.out.println("  Deployment Complete!");
        System.out.println("========================================================================");
        System.out.println();
        System.out.println("  Project: " + projectName);
        System.out.println("  BAR File: " + barFile);
        System.out.println("  Deployed to: " + integrationNode + ":" + integrationServer);
        System.out.println("  Test Root  : " + testRoot);
        System.out.println();

        System.out.println("  Next Steps:");
        System.out.println("  1. Run 'Test Application' to drop the sample input and read the output");
        System.out.println("  2. Monitor logs: mqsireadlog " + integrationNode);
        System.out.println("  3. Check message flow status in ACE Toolkit");
        System.out.println();
        System.out.println("========================================================================");
    }
    private int executeCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        pb.redirectErrorStream(true);
        pb.inheritIO();
        Process process = pb.start();
        return process.waitFor();
    }
    private String executeCommandWithOutput(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        process.waitFor();
        return output.toString();
    }
}
 
// Made with Bob
