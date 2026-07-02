package com.flowsmith.automation;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ACE FlowSmith Automation - Standalone Java Application
 * Automates BAR file creation and deployment to ACE Integration Server
 * 
 * Usage: java -cp . com.flowsmith.automation.ACEAutomation <project_name>
 * Example: java -cp . com.flowsmith.automation.ACEAutomation XAJ_PUB_DEMO_TEST_FIL
 */
public class ACEAutomation {
    
    private static final String ACE_TOOLKIT_PATH = "C:\\Program Files\\IBM\\ACE\\12.0";
    private static final String WORKSPACE_PATH = System.getProperty("user.dir");
    private static final String QUEUE_MANAGER = "QM1";
    private static final String INTEGRATION_NODE = "ACENODE";
    private static final String INTEGRATION_SERVER = "default";
    
    private String projectName;
    private String barFileName;
    private PrintWriter logWriter;
    
    public ACEAutomation(String projectName) {
        this.projectName = projectName;
        this.barFileName = projectName + ".bar";
        try {
            this.logWriter = new PrintWriter(new FileWriter("automation_log.txt", true));
        } catch (IOException e) {
            System.err.println("Warning: Could not create log file");
        }
    }
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        ACE FlowSmith AI - Automated Deployment            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        if (args.length < 1) {
            System.err.println("❌ Error: Project name required");
            System.err.println("Usage: java -cp . com.flowsmith.automation.ACEAutomation <project_name>");
            System.err.println("Example: java -cp . com.flowsmith.automation.ACEAutomation XAJ_PUB_DEMO_TEST_FIL");
            System.exit(1);
        }
        
        String projectName = args[0];
        ACEAutomation automation = new ACEAutomation(projectName);
        
        try {
            automation.run();
            System.out.println("\n✅ Deployment completed successfully!");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("\n❌ Deployment failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void run() throws Exception {
        log("Starting ACE FlowSmith Automation for project: " + projectName);
        
        // Step 1: Validate environment
        System.out.println("📋 Step 1/6: Validating environment...");
        validateEnvironment();
        System.out.println("   ✓ Environment validated");
        
        // Step 2: Build BAR file
        System.out.println("\n📦 Step 2/6: Building BAR file...");
        buildBarFile();
        System.out.println("   ✓ BAR file created: " + barFileName);
        
        // Step 3: Start Queue Manager
        System.out.println("\n🔧 Step 3/6: Starting Queue Manager...");
        startQueueManager();
        System.out.println("   ✓ Queue Manager started");
        
        // Step 4: Start Integration Node
        System.out.println("\n🚀 Step 4/6: Starting Integration Node...");
        startIntegrationNode();
        System.out.println("   ✓ Integration Node started");
        
        // Step 5: Deploy BAR file
        System.out.println("\n📤 Step 5/6: Deploying BAR file...");
        deployBarFile();
        System.out.println("   ✓ BAR file deployed");
        
        // Step 6: Verify deployment
        System.out.println("\n✅ Step 6/6: Verifying deployment...");
        verifyDeployment();
        System.out.println("   ✓ Deployment verified");
        
        log("Automation completed successfully");
    }
    
    private void validateEnvironment() throws Exception {
        // Check if ACE Toolkit is installed
        File aceToolkit = new File(ACE_TOOLKIT_PATH);
        if (!aceToolkit.exists()) {
            throw new Exception("ACE Toolkit not found at: " + ACE_TOOLKIT_PATH);
        }
        
        // Check if project exists
        File projectDir = new File(WORKSPACE_PATH, projectName);
        if (!projectDir.exists()) {
            throw new Exception("Project not found: " + projectName);
        }
        
        log("Environment validation passed");
    }
    
    private void buildBarFile() throws Exception {
        String mqsiCreateBarCmd = String.format(
            "\"%s\\server\\bin\\mqsicreatebar.exe\" -data \"%s\" -b \"%s\" -a \"%s\" -cleanBuild",
            ACE_TOOLKIT_PATH,
            WORKSPACE_PATH,
            barFileName,
            projectName
        );
        
        log("Executing: " + mqsiCreateBarCmd);
        executeCommand(mqsiCreateBarCmd, 120);
        
        // Verify BAR file was created
        File barFile = new File(barFileName);
        if (!barFile.exists()) {
            throw new Exception("BAR file was not created");
        }
        
        log("BAR file created successfully: " + barFile.getAbsolutePath());
    }
    
    private void startQueueManager() throws Exception {
        // Check if Queue Manager is already running
        String checkCmd = "dspmq -m " + QUEUE_MANAGER;
        try {
            String output = executeCommand(checkCmd, 10);
            if (output.contains("Running")) {
                log("Queue Manager already running");
                return;
            }
        } catch (Exception e) {
            log("Queue Manager not found, will attempt to start");
        }
        
        // Start Queue Manager
        String startCmd = "strmqm " + QUEUE_MANAGER;
        log("Executing: " + startCmd);
        executeCommand(startCmd, 30);
        
        // Wait for Queue Manager to start
        Thread.sleep(5000);
        log("Queue Manager started");
    }
    
    private void startIntegrationNode() throws Exception {
        // Check if Integration Node is already running
        String checkCmd = String.format(
            "\"%s\\server\\bin\\mqsilist.exe\"",
            ACE_TOOLKIT_PATH
        );
        
        try {
            String output = executeCommand(checkCmd, 10);
            if (output.contains(INTEGRATION_NODE) && output.contains("running")) {
                log("Integration Node already running");
                return;
            }
        } catch (Exception e) {
            log("Integration Node not running, will start");
        }
        
        // Start Integration Node
        String startCmd = String.format(
            "\"%s\\server\\bin\\mqsistart.exe\" %s",
            ACE_TOOLKIT_PATH,
            INTEGRATION_NODE
        );
        
        log("Executing: " + startCmd);
        executeCommand(startCmd, 60);
        
        // Wait for Integration Node to start
        Thread.sleep(10000);
        log("Integration Node started");
    }
    
    private void deployBarFile() throws Exception {
        String deployCmd = String.format(
            "\"%s\\server\\bin\\mqsideploy.exe\" %s -e %s -a \"%s\"",
            ACE_TOOLKIT_PATH,
            INTEGRATION_NODE,
            INTEGRATION_SERVER,
            new File(barFileName).getAbsolutePath()
        );
        
        log("Executing: " + deployCmd);
        executeCommand(deployCmd, 60);
        
        // Wait for deployment to complete
        Thread.sleep(5000);
        log("BAR file deployed successfully");
    }
    
    private void verifyDeployment() throws Exception {
        String verifyCmd = String.format(
            "\"%s\\server\\bin\\mqsilist.exe\" %s -e %s -d 2",
            ACE_TOOLKIT_PATH,
            INTEGRATION_NODE,
            INTEGRATION_SERVER
        );
        
        log("Executing: " + verifyCmd);
        String output = executeCommand(verifyCmd, 30);
        
        if (output.contains(projectName)) {
            log("Deployment verified - application is running");
        } else {
            throw new Exception("Deployment verification failed - application not found in server");
        }
    }
    
    private String executeCommand(String command, int timeoutSeconds) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("   " + line);
            }
        }
        
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new Exception("Command timed out after " + timeoutSeconds + " seconds");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new Exception("Command failed with exit code: " + exitCode);
        }
        
        return output.toString();
    }
    
    private void log(String message) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logMessage = timestamp + " - " + message;
        
        if (logWriter != null) {
            logWriter.println(logMessage);
            logWriter.flush();
        }
    }
    
    @Override
    protected void finalize() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}

// Made with Bob
