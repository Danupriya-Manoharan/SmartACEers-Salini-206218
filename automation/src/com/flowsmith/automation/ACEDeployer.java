package com.flowsmith.automation;
 
import java.io.*;
import java.util.Scanner;
 
/**
* ACE FlowSmith AI - Automated Deployment Tool
* 
* Automates the complete deployment workflow:
* 1. Build BAR file from generated project
* 2. Start Queue Manager
* 3. Start Integration Node
* 4. Deploy BAR file to Integration Server
* 
* Usage:
*   java ACEDeployer <projectName> [queueManager] [integrationNode] [integrationServer]
* 
* Example:
*   java ACEDeployer XAJ_PUB_TLMTF_FINANCING_FIL MB8QMGR Test_node_test default
*/
public class ACEDeployer {
    private static final String DEFAULT_QUEUE_MANAGER = "MB8QMGR";
    private static final String DEFAULT_INTEGRATION_NODE = "Test_node_test";
    private static final String DEFAULT_INTEGRATION_SERVER = "default";
    private static final String DEFAULT_WORKSPACE = System.getProperty("user.home") + "\\git\\FlowSmith_Generated";
    private static String ACE_TOOLKIT_PATH = null;
    private String projectName;
    private String queueManager;
    private String integrationNode;
    private String integrationServer;
    private String workspace;
    private String barOutputDir;
    private String barFile;
    
    // BAR override configuration (configurable via system properties)
    private String testBaseDir = System.getProperty("test.base", "C:\\temp\\test");
    private String testApp = System.getProperty("test.app");
    private String fileInNodeLabel = System.getProperty("test.filein", "FILEIN");
    private String fileOutNodeLabel = System.getProperty("test.fileout", "FILEOUT");
    private String flowName = System.getProperty("test.flow", "Adapter");
    
    public ACEDeployer(String projectName, String queueManager, String integrationNode, String integrationServer) {
        this.projectName = projectName;
        this.queueManager = queueManager != null ? queueManager : DEFAULT_QUEUE_MANAGER;
        this.integrationNode = integrationNode != null ? integrationNode : DEFAULT_INTEGRATION_NODE;
        this.integrationServer = integrationServer != null ? integrationServer : DEFAULT_INTEGRATION_SERVER;
        this.workspace = DEFAULT_WORKSPACE;
        this.barOutputDir = workspace + "\\BAR_Files";
        this.barFile = barOutputDir + "\\" + projectName + ".bar";
        
        // Default test app to project name if not specified
        if (testApp == null) {
            testApp = projectName;
        }
    }
    public static void main(String[] args) {
        printBanner();
        String projectName = null;
        String queueManager = null;
        String integrationNode = null;
        String integrationServer = null;
        // Parse command line arguments or prompt user
        if (args.length > 0) {
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
            deployer.deploy();
        } catch (Exception e) {
            System.err.println("\nERROR: Deployment failed");
            e.printStackTrace();
            System.exit(1);
        }
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
        // Step 1: Build BAR file
        buildBarFile();
        
        // Step 1.5: Apply BAR overrides for testing (before deployment)
        applyBarOverrides();
        
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
    private void applyBarOverrides() throws Exception {
        System.out.println();
        System.out.println("[Step 1.5/4] Applying BAR overrides for local testing...");
        System.out.println("========================================================================");
        
        // Build test directory paths
        String testInDir = testBaseDir + "\\" + testApp + "\\in";
        String testOutDir = testBaseDir + "\\" + testApp + "\\out";
        
        System.out.println("Test directories:");
        System.out.println("  Input  : " + testInDir);
        System.out.println("  Output : " + testOutDir);
        
        // Create test directories if they don't exist
        File inDir = new File(testInDir);
        File outDir = new File(testOutDir);
        if (!inDir.exists()) {
            inDir.mkdirs();
            System.out.println("Created input directory: " + testInDir);
        }
        if (!outDir.exists()) {
            outDir.mkdirs();
            System.out.println("Created output directory: " + testOutDir);
        }
        
        // Build override keys
        String fileInKey = String.format("%s#%s.inputDirectory", flowName, fileInNodeLabel);
        String fileOutKey = String.format("%s#%s.directory", flowName, fileOutNodeLabel);
        
        // Build override string (comma-separated key=value pairs)
        String overrides = String.format("%s=%s,%s=%s", 
            fileInKey, testInDir,
            fileOutKey, testOutDir);
        
        System.out.println("Override keys:");
        System.out.println("  " + fileInKey + " = " + testInDir);
        System.out.println("  " + fileOutKey + " = " + testOutDir);
        
        // Build output BAR file name
        String testBarFile = barFile.replace(".bar", "-test.bar");
        
        // Use mqsiapplybaroverride (simpler, inline overrides)
        String command = String.format(
            "call \"%s\\server\\bin\\mqsiprofile.cmd\" && " +
            "\"%s\\server\\bin\\mqsiapplybaroverride\" -b \"%s\" -o \"%s\" -m \"%s\"",
            ACE_TOOLKIT_PATH,
            ACE_TOOLKIT_PATH,
            barFile,
            testBarFile,
            overrides
        );
        
        System.out.println("Applying overrides...");
        System.out.println("Executing: mqsiapplybaroverride");
        
        int exitCode = executeCommand(command);
        if (exitCode != 0) {
            System.out.println("WARNING: BAR override failed (exit code: " + exitCode + ")");
            System.out.println("Continuing with original BAR file...");
            System.out.println("This may happen if the flow doesn't have FileInput/FileOutput nodes.");
            return;
        }
        
        // Update barFile to point to the test BAR
        barFile = testBarFile;
        
        System.out.println("SUCCESS: BAR overrides applied");
        System.out.println("  Test BAR file: " + testBarFile);
        System.out.println();
        System.out.println("To test the deployed flow:");
        System.out.println("  1. Copy test XML to: " + testInDir);
        System.out.println("  2. Check output in: " + testOutDir);
    }

        int exitCode = executeCommand(startCommand);
        if (exitCode != 0) {
            throw new Exception("Failed to start Integration Node (exit code: " + exitCode + ")");
        }
        // Wait for Integration Node to start
        Thread.sleep(10000);
        System.out.println("SUCCESS: Integration Node started");
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
        		"call \"%s\\server\\bin\\mqsiprofile.cmd\" && \"%s\\server\\bin\\mqsilist %s -e %s -d 2",
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
        System.out.println();
        
        // Show test directories if BAR overrides were applied
        if (barFile.contains("-test.bar")) {
            System.out.println("  Test Directories:");
            System.out.println("  Input  : " + testBaseDir + "\\" + testApp + "\\in");
            System.out.println("  Output : " + testBaseDir + "\\" + testApp + "\\out");
            System.out.println();
        }
        
        System.out.println("  Next Steps:");
        if (barFile.contains("-test.bar")) {
            System.out.println("  1. Copy test XML to input directory");
            System.out.println("  2. Check output directory for transformed JSON");
            System.out.println("  3. Monitor logs: mqsireadlog " + integrationNode);
        } else {
            System.out.println("  1. Test the deployed flow");
            System.out.println("  2. Monitor logs: mqsireadlog " + integrationNode);
            System.out.println("  3. Check message flow status in ACE Toolkit");
        }
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
