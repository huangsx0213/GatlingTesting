package com.qa.app.util;

import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingRunParameters;
import com.qa.app.model.GatlingTest;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GatlingTestExecutor {

    public static void execute(GatlingTest test, GatlingRunParameters params, Endpoint endpoint) {
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
            try {
                if (test.getHeaders() != null && !test.getHeaders().trim().isEmpty()) {
                    String headersText = test.getHeaders().trim();
                    if (headersText.startsWith("{")) {
                        // JSON format
                        JSONObject headers = new JSONObject(headersText);
                        for (String key : headers.keySet()) {
                            System.out.println(key + ": " + headers.getString(key));
                        }
                    } else {
                        // multi-line text format
                        String[] lines = headersText.split("\\r?\\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                System.out.println(line);
                            }
                        }
                    }
                } else {
                    System.out.println("(No request headers)");
                }
            } catch (Exception e) {
                System.out.println("Request headers parsing failed: " + e.getMessage());
                System.out.println("Original request headers: " + test.getHeaders());
            }
            
            System.out.println("\n----- Request Body -----");
            if (test.getBody() != null && !test.getBody().trim().isEmpty()) {
                try {
                    // try to format JSON
                    JSONObject json = new JSONObject(test.getBody());
                    System.out.println(json.toString(2)); // indent 2 spaces
                } catch (Exception e) {
                    // if not valid JSON, print original content
                    System.out.println(test.getBody());
                }
            } else {
                System.out.println("(No request body)");
            }
            
            System.out.println("\n----- Test Parameters -----");
            System.out.println("Concurrent users: " + params.getUsers());
            System.out.println("Warmup time (seconds): " + params.getRampUp());
            System.out.println("Repetitions: " + params.getRepetitions());
            System.out.println("Expected status code: " + test.getExpStatus());
            System.out.println("Wait time (seconds): " + test.getWaitTime());
            System.out.println("===================================");

            // Do not use static setters as they don't work across processes
            // DynamicJavaSimulation.setParameters(test, params, endpoint);

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

            System.out.println("Starting Gatling test...");
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
            throw new RuntimeException("Failed to run Gatling test via Java API", e);
        }
    }
} 