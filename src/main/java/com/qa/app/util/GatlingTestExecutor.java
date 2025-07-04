package com.qa.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qa.app.dao.api.IGatlingTestDao;
import com.qa.app.dao.impl.GatlingTestDaoImpl;
import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.GatlingTest;
import com.qa.app.model.reports.CaseReport;
import com.qa.app.model.reports.FunctionalTestReport;
import com.qa.app.model.reports.ModeGroup;
import com.qa.app.model.reports.TestMode;
import com.qa.app.model.threadgroups.SteppingThreadGroup;
import com.qa.app.model.threadgroups.UltimateThreadGroupStep;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GatlingTestExecutor {

    /**
     * Prefix used by DynamicJavaSimulation to output the new report format to stdout.
     */
    private static final String REPORT_PREFIX = "REPORT_JSON:";
    private static final String RESULT_PREFIX = "CHECK_RESULTS_JSON:";

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
            // Expected status code will be determined from responseChecks at runtime
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

            // Find the logback.xml file in resources
            URL logbackUrl = GatlingTestExecutor.class.getClassLoader().getResource("logback.xml");
            String logbackPath = (logbackUrl != null) ? new File(logbackUrl.toURI()).getAbsolutePath() : null;

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
            if (logbackPath != null) {
                command.add("-Dlogback.configurationFile=" + logbackPath);
            }
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
                    processBuilder.redirectErrorStream(true); // Capture stderr along with stdout

                    Process process = processBuilder.start();

                    String jsonResult = null;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line); // Pass Gatling output to our console
                            if (line.startsWith(REPORT_PREFIX)) {
                                jsonResult = line.substring(REPORT_PREFIX.length());
                            }
                        }
                    }

                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        System.out.println("Gatling test execution completed.");
                        com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test " + test.getTcid() + " Completed", com.qa.app.ui.vm.MainViewModel.StatusType.SUCCESS);
                        if (jsonResult != null) {
                            processAndSaveReports(jsonResult, java.util.Collections.singletonList(test));
                        } else {
                            System.err.println("Could not find report results in Gatling output. DB results will not be updated.");
                        }
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

            // Find the logback.xml file in resources
            URL logbackUrl = GatlingTestExecutor.class.getClassLoader().getResource("logback.xml");
            String logbackPath = (logbackUrl != null) ? new File(logbackUrl.toURI()).getAbsolutePath() : null;

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
            if (logbackPath != null) {
                command.add("-Dlogback.configurationFile=" + logbackPath);
            }
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
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            String jsonResult = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (line.startsWith(REPORT_PREFIX)) {
                        jsonResult = line.substring(REPORT_PREFIX.length());
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Gatling synchronous execution completed.");
                com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test " + test.getTcid() + " Completed", com.qa.app.ui.vm.MainViewModel.StatusType.SUCCESS);
                if (jsonResult != null) {
                    processAndSaveReports(jsonResult, java.util.Collections.singletonList(test));
                } else {
                    System.err.println("Could not find report results in Gatling output. DB results will not be updated.");
                }
            } else {
                throw new RuntimeException("Gatling test execution failed, exit code: " + exitCode);
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

            // Find the logback.xml file in resources
            URL logbackUrl = GatlingTestExecutor.class.getClassLoader().getResource("logback.xml");
            String logbackPath = (logbackUrl != null) ? new File(logbackUrl.toURI()).getAbsolutePath() : null;

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
            if (logbackPath != null) {
                command.add("-Dlogback.configurationFile=" + logbackPath);
            }
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
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            String jsonResult = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (line.startsWith(REPORT_PREFIX)) {
                        jsonResult = line.substring(REPORT_PREFIX.length());
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Gatling batch execution completed.");
                if (jsonResult != null) {
                    processAndSaveReports(jsonResult, tests);
                } else {
                    System.err.println("Could not find check results in Gatling output. DB results will not be updated.");
                }
            } else {
                throw new RuntimeException("Gatling batch execution failed, exit code: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute Gatling batch: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // Async batch execution, avoid blocking the caller (e.g., JavaFX UI thread)
    public static void executeBatch(java.util.List<GatlingTest> tests, GatlingLoadParameters params,
                                    java.util.List<Endpoint> endpoints,
                                    java.util.List<String> origins,
                                    java.util.List<String> modes,
                                    Runnable onComplete) {
        // Wrap the entire heavy-lifting logic inside a background thread so the JavaFX UI thread stays responsive
        new Thread(() -> {
            try {
                com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Running " + tests.size() + " test(s).", com.qa.app.ui.vm.MainViewModel.StatusType.INFO);

                if (tests == null || endpoints == null || tests.size() != endpoints.size()) {
                    throw new IllegalArgumentException("Tests and Endpoints list size mismatch or null");
                }
                if (origins != null && modes != null && (origins.size() != tests.size() || modes.size() != tests.size())) {
                    throw new IllegalArgumentException("Origins/Modes list size mismatch");
                }

                String javaHome = System.getProperty("java.home");
                String javaBin = java.nio.file.Paths.get(javaHome, "bin", "java").toString();
                String classpath = assembleClasspath();
                String gatlingMain = "io.gatling.app.Gatling";
                String simulationClass = DynamicJavaSimulation.class.getName();
                String resultsPath = java.nio.file.Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();

                // Find the logback.xml file in resources
                URL logbackUrl = GatlingTestExecutor.class.getClassLoader().getResource("logback.xml");
                String logbackPath = (logbackUrl != null) ? new File(logbackUrl.toURI()).getAbsolutePath() : null;

                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

                // Build BatchItem list with extra meta
                java.util.List<java.util.Map<String, Object>> batchItems = new java.util.ArrayList<>();
                for (int i = 0; i < tests.size(); i++) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("test", tests.get(i));
                    map.put("endpoint", endpoints.get(i));
                    if (origins != null && modes != null) {
                        map.put("origin", origins.get(i));
                        map.put("mode", modes.get(i));
                    }
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
                if (logbackPath != null) {
                    command.add("-Dlogback.configurationFile=" + logbackPath);
                }
                command.add("-Dgatling.tests.file=" + batchFile.getAbsolutePath());
                command.add("-Dgatling.params.file=" + paramsFile.getAbsolutePath());
                command.add(gatlingMain);
                command.add("-s");
                command.add(simulationClass);
                command.add("-rf");
                command.add(resultsPath);

                System.out.println("========== Gatling Batch Execution (Async) ==========");
                for (int i = 0; i < tests.size(); i++) {
                    System.out.println(String.format("  %d) %s [ %s %s ]", i + 1,
                            tests.get(i).getTcid(),
                            endpoints.get(i).getMethod(),
                            endpoints.get(i).getUrl()));
                }
                System.out.println("Starting Gatling batch test in background (separate process)...");

                java.lang.ProcessBuilder processBuilder = new java.lang.ProcessBuilder(command);
                processBuilder.redirectErrorStream(true);

                java.lang.Process process = processBuilder.start();

                String jsonResult = null;
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        if (line.startsWith(REPORT_PREFIX)) {
                            jsonResult = line.substring(REPORT_PREFIX.length());
                        }
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    System.out.println("Gatling batch execution completed.");
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test(s) completed.", com.qa.app.ui.vm.MainViewModel.StatusType.SUCCESS);
                    if (jsonResult != null) {
                        processAndSaveReports(jsonResult, tests);
                    } else {
                        System.err.println("Could not find check results in Gatling output. DB results will not be updated.");
                    }
                    if (onComplete != null) {
                        // Ensure UI updates are executed on the JavaFX Application Thread
                        if (javafx.application.Platform.isFxApplicationThread()) {
                            onComplete.run();
                        } else {
                            javafx.application.Platform.runLater(onComplete);
                        }
                    }
                } else {
                    System.out.println("Gatling batch execution failed, exit code: " + exitCode);
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test(s) Failed, exit code: " + exitCode, com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                }
            } catch (Exception e) {
                System.err.println("Failed to start Gatling batch process: " + e.getMessage());
                e.printStackTrace();
                com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test(s) Exception: " + e.getMessage(), com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
            }
        }, "gatling-batch-runner").start();
    }

    // Backward compatible method (without dependency metadata)
    public static void executeBatch(java.util.List<GatlingTest> tests, GatlingLoadParameters params,
                                    java.util.List<Endpoint> endpoints, Runnable onComplete) {
        executeBatch(tests, params, endpoints, null, null, onComplete);
    }

    private static void processAndSaveReports(String jsonContent, List<GatlingTest> executedTests) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<>() {};
            List<Map<String, Object>> reportItems = mapper.readValue(jsonContent, typeRef);

            // Group reports by origin TCID
            Map<String, List<Map<String, Object>>> groupedByOrigin = reportItems.stream()
                    .collect(Collectors.groupingBy(item -> (String) item.get("origin")));

            IGatlingTestDao testDao = new GatlingTestDaoImpl();

            // NEW: collect all final reports for batch aggregation
            List<FunctionalTestReport> aggregatedReports = new ArrayList<>();

            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByOrigin.entrySet()) {
                String originTcid = entry.getKey();
                List<Map<String, Object>> itemsForOrigin = entry.getValue();

                FunctionalTestReport finalReport = new FunctionalTestReport();
                finalReport.setOriginTcid(originTcid);
                finalReport.setExecutedAt(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

                // Find the main test to get suite info
                executedTests.stream()
                        .filter(t -> t.getTcid().equals(originTcid))
                        .findFirst()
                        .ifPresent(mainTest -> finalReport.setSuite(mainTest.getSuite()));

                // Group by mode directly based on each entry to accurately separate SETUP / MAIN / TEARDOWN
                Map<TestMode, List<CaseReport>> casesByMode = new java.util.EnumMap<>(TestMode.class);
                for (Map<String, Object> itemMap : itemsForOrigin) {
                    String modeStr = (String) itemMap.getOrDefault("mode", "MAIN");
                    TestMode mode = TestMode.valueOf(modeStr);
                    CaseReport cr = mapper.convertValue(itemMap.get("report"), CaseReport.class);
                    casesByMode.computeIfAbsent(mode, k -> new java.util.ArrayList<>()).add(cr);
                }
                
                List<ModeGroup> modeGroups = new ArrayList<>();
                EnumSet.of(TestMode.SETUP, TestMode.MAIN, TestMode.TEARDOWN, TestMode.CONDITION).forEach(mode -> {
                    if (casesByMode.containsKey(mode)) {
                        ModeGroup group = new ModeGroup();
                        group.setMode(mode);
                        group.setCases(casesByMode.get(mode));
                        modeGroups.add(group);
                    }
                });

                finalReport.setGroups(modeGroups);

                // Determine overall pass status
                boolean overallPassed = modeGroups.stream()
                        .flatMap(mg -> mg.getCases().stream())
                        .allMatch(CaseReport::isPassed);

                // Save report to file
                String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String reportFileName = String.format("functional_%s_%s.json", originTcid, timestamp);

                Path reportDir = Paths.get(System.getProperty("user.dir"), "target", "gatling", "reports", "functional");
                Files.createDirectories(reportDir);
                Path reportPath = reportDir.resolve(reportFileName);

                try (Writer writer = new BufferedWriter(new FileWriter(reportPath.toFile()))) {
                    mapper.writeValue(writer, finalReport);
                }

                // Add to aggregate list
                aggregatedReports.add(finalReport);

                // Update the main test case in the database
                executedTests.stream()
                        .filter(t -> t.getTcid().equals(originTcid))
                        .findFirst()
                        .ifPresent(testToUpdate -> {
                            try {
                                testToUpdate.setReportPath(reportPath.toString());
                                testToUpdate.setLastRunPassed(overallPassed);
                                testDao.updateTest(testToUpdate);
                                System.out.println("Updated test '" + originTcid + "' with report path: " + reportPath);
                            } catch (Exception e) {
                                System.err.println("Failed to update test in DB: " + originTcid);
                                e.printStackTrace();
                            }
                        });
            }

            // === NEW ===
            // Persist an aggregated report containing all FunctionalTestReport objects if more than one exists
            if (aggregatedReports.size() > 1) {
                String batchTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String batchFileName = String.format("functional_batch_%s.json", batchTimestamp);
                Path reportDir = Paths.get(System.getProperty("user.dir"), "target", "gatling", "reports", "functional");
                Files.createDirectories(reportDir);
                Path batchReportPath = reportDir.resolve(batchFileName);

                try (Writer writer = new BufferedWriter(new FileWriter(batchReportPath.toFile()))) {
                    mapper.writeValue(writer, aggregatedReports);
                }
                System.out.println("Aggregated report saved to: " + batchReportPath);
            }

        } catch (Exception e) {
            System.err.println("Failed to parse or process functional test report from JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void updateTestsWithResultsFromJson(List<GatlingTest> executedTests, String jsonContent) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> results = mapper.readValue(jsonContent, new TypeReference<List<Map<String, Object>>>() {});

            IGatlingTestDao testDao = new GatlingTestDaoImpl();

            for (Map<String, Object> result : results) {
                String tcid = (String) result.get("tcid");
                Object checksObj = result.get("responseChecks");

                if (tcid == null || checksObj == null) continue;

                String updatedChecksJson = mapper.writeValueAsString(checksObj);

                // Find the corresponding test in the executed list and update it
                for (GatlingTest test : executedTests) {
                    if (tcid.equals(test.getTcid())) {
                        test.setResponseChecks(updatedChecksJson);
                        testDao.updateTest(test);
                        System.out.println("Updated response checks for test: " + tcid);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse or process response check results from JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 