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

            // Execute Gatling in a separate process to avoid System.exit() in the main app
            String javaHome = System.getProperty("java.home");
            String javaBin = Paths.get(javaHome, "bin", "java").toString();
            String classpath = System.getProperty("java.class.path");
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
                    System.err.println("Gatling test execution failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            System.err.println("Failed to initialize Gatling test execution: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start Gatling test process", e);
        }
    }
} 