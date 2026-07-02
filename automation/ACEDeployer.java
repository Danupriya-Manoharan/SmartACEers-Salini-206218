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
    
    private String projectName;
    private String queueManager;
    private String integrationNode;
    private String integrationServer;
    private String workspace;
    private String barOutputDir;
    private String barFile;
    
    public ACEDeployer(String projectName, String queueManager, String integrationNode, String integrationServer) {
        this.projectName = projectName;
        this.queueManager = queueManager != null ? queueManager : DEFAULT_QUEUE_MANAGER;
        this.integrationNode = integrationNode != null ? integrationNode : DEFAULT_INTEGRATION_NODE;
        this.integrationServer = integrationServer != null ? integrationServer : DEFAULT_INTEGRATION_SERVER;
        this.workspace = DEFAULT_WORKSPACE;
        this.barOutputDir = workspace + "\\BAR_Files";
        this.barFile = barOutputDir + "\\" + projectName + ".bar";
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
        
        String command = String.format(
            "mqsicreatebar -data \"%s\" -b \"%s\" -a \"%s\" -o",
            workspace, barFile, projectName
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
        String checkCommand = "mqsilist";
        String output = executeCommandWithOutput(checkCommand);
        
        if (output.contains(integrationNode) && output.contains("running")) {
            System.out.println("Integration Node " + integrationNode + " is already running");
            return;
        }
        
        // Start Integration Node
        System.out.println("Starting Integration Node " + integrationNode + "...");
        String startCommand = "mqsistart " + integrationNode;
        
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
            "mqsideploy %s -e %s -a \"%s\"",
            integrationNode, integrationServer, barFile
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
            "mqsilist %s -e %s -d 2",
            integrationNode, integrationServer
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
        System.out.println("  Next Steps:");
        System.out.println("  1. Test the deployed flow");
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
