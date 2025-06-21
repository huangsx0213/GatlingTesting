package com.qa.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.GatlingTest;
import com.qa.app.model.threadgroups.SteppingThreadGroup;
import com.qa.app.model.threadgroups.UltimateThreadGroupStep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GatlingTestExecutor {

    private static String assembleClasspath() {
        String cp = System.getProperty("java.class.path");
        try {
            String selfPath = new java.io.File(GatlingTestExecutor.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getPath();
            if (!cp.contains(selfPath)) {
                cp += java.io.File.pathSeparator + selfPath;
            }
        } catch (Exception ignored) { }
        return cp;
    }

    public static void execute(GatlingTest test, GatlingLoadParameters params, Endpoint endpoint) {
        try {
            // print test details
            System.out.println("========== Gatling Test Details ==========");
            System.out.println("Test ID: " + test.getId());
            System.out.println("Test TCID: " + test.getTcid());
            System.out.println("Test Suite: " + test.getSuite());

            System.out.println("\n----- Endpoint Information -----");
            System.out.println("Endpoint Name: " + endpoint.getName());
            System.out.println("URL: " + endpoint.getUrl());
            System.out.println("Method: " + endpoint.getMethod());

            System.out.println("\n----- Request Headers -----");
            System.out.println(test.getHeaders());

            System.out.println("\n----- Request Body -----");
            System.out.println(test.getBody());

            System.out.println("\n----- Test Parameters -----");
            System.out.println("Load Profile Type: " + params.getType());
            switch (params.getType()) {
                case STANDARD:
                    System.out.println("  Users: " + params.getStandardThreadGroup().getNumThreads());
                    System.out.println("  Ramp-up (s): " + params.getStandardThreadGroup().getRampUp());
                    System.out.println("  Duration (s): " + params.getStandardThreadGroup().getDuration());
                    break;
                case STEPPING:
                    SteppingThreadGroup steppingConfig = params.getSteppingThreadGroup();
                    System.out.println("  Total Threads: " + steppingConfig.getNumThreads());
                    System.out.println("  Initial Delay: " + steppingConfig.getInitialDelay() + "s");
                    System.out.println("  Initial Users: " + steppingConfig.getStartUsers());
                    System.out.println("  Increment Users: " + steppingConfig.getIncrementUsers());
                    System.out.println("  Increment Interval: " + steppingConfig.getIncrementTime() + "s");
                    System.out.println("  Hold Load For: " + steppingConfig.getHoldLoad() + "s");
                    break;
                case ULTIMATE:
                    System.out.println("  --- Ultimate Steps ---");
                    for(UltimateThreadGroupStep step : params.getUltimateThreadGroup().getSteps()){
                         System.out.println(String.format("    - Step: %d users, delay %ds, rampUp %ds, hold %ds, rampDown %ds",
                            step.getInitialLoad(), step.getStartTime(), step.getStartupTime(), step.getHoldTime(), step.getShutdownTime()));
                    }
                    break;
            }
            System.out.println("Expected status code: " + test.getExpStatus());
            System.out.println("Wait time (seconds): " + test.getWaitTime());
            System.out.println("===================================");

            // ===== 状态栏：已开始 =====
            com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test " + test.getTcid() + " Starting", com.qa.app.ui.vm.MainViewModel.StatusType.INFO);

            // Execute Gatling in a separate process to avoid System.exit() in the main app
            String javaHome = System.getProperty("java.home");
            String javaBin = Paths.get(javaHome, "bin", "java").toString();
            String classpath = assembleClasspath();
            String gatlingMain = "io.gatling.app.Gatling";
            String simulationClass = DynamicJavaSimulation.class.getName();
            String resultsPath = Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();

            // Serialize parameters to JSON and write to temp files to avoid command line arg issues
            ObjectMapper objectMapper = new ObjectMapper();

            File testFile = File.createTempFile("gatling_test_", ".json");
            testFile.deleteOnExit();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile))) {
                writer.write(objectMapper.writeValueAsString(test));
            }

            File paramsFile = File.createTempFile("gatling_params_", ".json");
            paramsFile.deleteOnExit();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(paramsFile))) {
                writer.write(objectMapper.writeValueAsString(params));
            }

            File endpointFile = File.createTempFile("gatling_endpoint_", ".json");
            endpointFile.deleteOnExit();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(endpointFile))) {
                writer.write(objectMapper.writeValueAsString(endpoint));
            }

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("--add-opens");
            command.add("java.base/java.lang=ALL-UNNAMED");
            command.add("-cp");
            command.add(classpath);
            command.add("-Dgatling.test.file=" + testFile.getAbsolutePath());
            command.add("-Dgatling.params.file=" + paramsFile.getAbsolutePath());
            command.add("-Dgatling.endpoint.file=" + endpointFile.getAbsolutePath());
            command.add(gatlingMain);
            command.add("-s");
            command.add(simulationClass);
            command.add("-rf");
            command.add(resultsPath);

            System.out.println("Starting Gatling test in background...");
            // Run the process in a new thread to avoid blocking the caller (e.g., UI thread)
            new Thread(() -> {
                try {
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test " + test.getTcid() + " Running", com.qa.app.ui.vm.MainViewModel.StatusType.INFO);

                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    processBuilder.inheritIO();

                    Process process = processBuilder.start();
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        System.out.println("Gatling test execution completed.");
                        com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test " + test.getTcid() + " Completed", com.qa.app.ui.vm.MainViewModel.StatusType.SUCCESS);
                    } else {
                        System.out.println("Gatling test execution failed, exit code: " + exitCode);
                        com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test " + test.getTcid() + " Failed, exit code: " + exitCode, com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                    }
                } catch (Exception e) {
                    System.err.println("Gatling test execution failed: " + e.getMessage());
                    e.printStackTrace();
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test " + test.getTcid() + " Exception: " + e.getMessage(), com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                }
            }, "gatling-test-runner").start();

        } catch (Exception e) {
            System.err.println("Failed to initialize Gatling test execution: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start Gatling test process", e);
        }
    }

    public static void executeSync(GatlingTest test, GatlingLoadParameters params, Endpoint endpoint) {
        try {
            // Reuse the same code as execute() but without spawning a new thread
            System.out.println("========== Gatling Test Details ==========");
            System.out.println("Test ID: " + test.getId());
            System.out.println("Test TCID: " + test.getTcid());
            System.out.println("Test Suite: " + test.getSuite());

            System.out.println("\n----- Endpoint Information -----");
            System.out.println("Endpoint Name: " + endpoint.getName());
            System.out.println("URL: " + endpoint.getUrl());
            System.out.println("Method: " + endpoint.getMethod());

            System.out.println("\n----- Request Headers -----");
            System.out.println(test.getHeaders());

            System.out.println("\n----- Request Body -----");
            System.out.println(test.getBody());

            System.out.println("\n----- Test Parameters -----");
            System.out.println("Load Profile Type: " + params.getType());
            // Output params similar to execute() but omitted here for brevity

            String javaHome = System.getProperty("java.home");
            String javaBin = Paths.get(javaHome, "bin", "java").toString();
            String classpath = assembleClasspath();
            String gatlingMain = "io.gatling.app.Gatling";
            String simulationClass = DynamicJavaSimulation.class.getName();
            String resultsPath = Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();

            ObjectMapper objectMapper = new ObjectMapper();

            File testFile = File.createTempFile("gatling_test_", ".json");
            testFile.deleteOnExit();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile))) {
                writer.write(objectMapper.writeValueAsString(test));
            }

            File paramsFile = File.createTempFile("gatling_params_", ".json");
            paramsFile.deleteOnExit();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(paramsFile))) {
                writer.write(objectMapper.writeValueAsString(params));
            }

            File endpointFile = File.createTempFile("gatling_endpoint_", ".json");
            endpointFile.deleteOnExit();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(endpointFile))) {
                writer.write(objectMapper.writeValueAsString(endpoint));
            }

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("--add-opens");
            command.add("java.base/java.lang=ALL-UNNAMED");
            command.add("-cp");
            command.add(classpath);
            command.add("-Dgatling.test.file=" + testFile.getAbsolutePath());
            command.add("-Dgatling.params.file=" + paramsFile.getAbsolutePath());
            command.add("-Dgatling.endpoint.file=" + endpointFile.getAbsolutePath());
            command.add(gatlingMain);
            command.add("-s");
            command.add(simulationClass);
            command.add("-rf");
            command.add(resultsPath);

            System.out.println("Starting Gatling test synchronously...");
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO();

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Gatling test execution completed.");
            } else {
                System.out.println("Gatling test execution failed, exit code: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize Gatling test execution: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start Gatling test process", e);
        }
    }

    public static void executeBatchSync(java.util.List<GatlingTest> tests, GatlingLoadParameters params, java.util.List<Endpoint> endpoints) {
        try {
            if (tests.size() != endpoints.size()) {
                throw new IllegalArgumentException("Tests and Endpoints list size mismatch");
            }

            String javaHome = System.getProperty("java.home");
            String javaBin = Paths.get(javaHome, "bin", "java").toString();
            String classpath = assembleClasspath();
            String gatlingMain = "io.gatling.app.Gatling";
            String simulationClass = DynamicJavaSimulation.class.getName();
            String resultsPath = Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();

            ObjectMapper objectMapper = new ObjectMapper();

            // Build BatchItem list
            java.util.List<java.util.Map<String, Object>> batchItems = new java.util.ArrayList<>();
            for (int i = 0; i < tests.size(); i++) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("test", tests.get(i));
                map.put("endpoint", endpoints.get(i));
                batchItems.add(map);
            }

            File batchFile = File.createTempFile("gatling_batch_tests_", ".json");
            batchFile.deleteOnExit();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(batchFile))) {
                writer.write(objectMapper.writeValueAsString(batchItems));
            }

            File paramsFile = File.createTempFile("gatling_params_", ".json");
            paramsFile.deleteOnExit();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(paramsFile))) {
                writer.write(objectMapper.writeValueAsString(params));
            }

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("--add-opens");
            command.add("java.base/java.lang=ALL-UNNAMED");
            command.add("-cp");
            command.add(classpath);
            command.add("-Dgatling.tests.file=" + batchFile.getAbsolutePath());
            command.add("-Dgatling.params.file=" + paramsFile.getAbsolutePath());
            command.add(gatlingMain);
            command.add("-s");
            command.add(simulationClass);
            command.add("-rf");
            command.add(resultsPath);

            System.out.println("========== Gatling Batch Execution ==========");
            for (int i = 0; i < tests.size(); i++) {
                System.out.println(String.format("  %d) %s [ %s %s ]", i + 1,
                        tests.get(i).getTcid(),
                        endpoints.get(i).getMethod(),
                        endpoints.get(i).getUrl()));
            }
            System.out.println("Starting Gatling batch test synchronously...");
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO();

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Gatling batch execution completed.");
            } else {
                System.out.println("Gatling batch execution failed, exit code: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Failed to start Gatling batch process: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start Gatling batch test process", e);
        }
    }

    // Async batch execution, avoid blocking the caller (e.g., JavaFX UI thread)
    public static void executeBatch(java.util.List<GatlingTest> tests, GatlingLoadParameters params, java.util.List<Endpoint> endpoints) {
        // Reuse the same code as executeBatchSync but run the process in a new thread and wait for it to finish.
        new Thread(() -> {
            try {
                if (tests == null || endpoints == null || tests.size() != endpoints.size()) {
                    throw new IllegalArgumentException("Tests and Endpoints list size mismatch or null");
                }

                String javaHome = System.getProperty("java.home");
                String javaBin = java.nio.file.Paths.get(javaHome, "bin", "java").toString();
                String classpath = assembleClasspath();
                String gatlingMain = "io.gatling.app.Gatling";
                String simulationClass = DynamicJavaSimulation.class.getName();
                String resultsPath = java.nio.file.Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();

                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

                // Build BatchItem list
                java.util.List<java.util.Map<String, Object>> batchItems = new java.util.ArrayList<>();
                for (int i = 0; i < tests.size(); i++) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("test", tests.get(i));
                    map.put("endpoint", endpoints.get(i));
                    batchItems.add(map);
                }

                java.io.File batchFile = java.io.File.createTempFile("gatling_batch_tests_", ".json");
                batchFile.deleteOnExit();
                try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(batchFile))) {
                    writer.write(objectMapper.writeValueAsString(batchItems));
                }

                java.io.File paramsFile = java.io.File.createTempFile("gatling_params_", ".json");
                paramsFile.deleteOnExit();
                try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(paramsFile))) {
                    writer.write(objectMapper.writeValueAsString(params));
                }

                java.util.List<String> command = new java.util.ArrayList<>();
                command.add(javaBin);
                command.add("--add-opens");
                command.add("java.base/java.lang=ALL-UNNAMED");
                command.add("-cp");
                command.add(classpath);
                command.add("-Dgatling.tests.file=" + batchFile.getAbsolutePath());
                command.add("-Dgatling.params.file=" + paramsFile.getAbsolutePath());
                command.add(gatlingMain);
                command.add("-s");
                command.add(simulationClass);
                command.add("-rf");
                command.add(resultsPath);

                // Status bar: running
                com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Batch test is running", com.qa.app.ui.vm.MainViewModel.StatusType.INFO);

                System.out.println("========== Gatling Batch Execution (Async) ==========");
                for (int i = 0; i < tests.size(); i++) {
                    System.out.println(String.format("  %d) %s [ %s %s ]", i + 1,
                            tests.get(i).getTcid(),
                            endpoints.get(i).getMethod(),
                            endpoints.get(i).getUrl()));
                }
                System.out.println("Starting Gatling batch test in background...");

                // Status bar: running
                com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Batch test is running", com.qa.app.ui.vm.MainViewModel.StatusType.INFO);

                java.lang.ProcessBuilder processBuilder = new java.lang.ProcessBuilder(command);
                processBuilder.inheritIO();

                java.lang.Process process = processBuilder.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    System.out.println("Gatling batch execution completed.");
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Batch test is completed", com.qa.app.ui.vm.MainViewModel.StatusType.SUCCESS);
                } else {
                    System.out.println("Gatling batch execution failed, exit code: " + exitCode);
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Batch test failed, exit code: " + exitCode, com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                }
            } catch (Exception e) {
                System.err.println("Failed to start Gatling batch process: " + e.getMessage());
                e.printStackTrace();
                com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Batch test exception: " + e.getMessage(), com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
            }
        }, "gatling-batch-runner").start();
    }
} 