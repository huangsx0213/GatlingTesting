package com.qa.app.service.runner;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.HashMap;

public class GatlingTestRunner {
    /**
     * Prefix used by GatlingTestSimulation to output test variables.
     */
    private static final String VARIABLES_PREFIX = "TEST_VARIABLES:";

    private static List<String> buildGatlingCommand(String testsFilePath, String paramsFilePath, String reportFilePath) throws Exception {
        Map<String, String> sysProps = new HashMap<>();
        sysProps.put("gatling.tests.file", testsFilePath);
        sysProps.put("gatling.params.file", paramsFilePath);
        sysProps.put("gatling.report.file", reportFilePath);
        String resultsPath = Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();
        return GatlingRunnerUtils.buildGatlingCommand(GatlingTestSimulation.class.getName(), sysProps, resultsPath);
    }

    // Async execution with completion callback
    public static void executeGatlingTests(java.util.List<GatlingTest> tests, GatlingLoadParameters params,
                                    java.util.List<Endpoint> endpoints,
                                    java.util.List<String> origins,
                                    java.util.List<String> modes,
                                    java.lang.Runnable onComplete) {
        new Thread(() -> {
            try {
                if (tests == null || endpoints == null || tests.size() != endpoints.size()) {
                    throw new IllegalArgumentException("Tests and Endpoints list size mismatch or null");
                }
                if (origins != null && modes != null && (origins.size() != tests.size() || modes.size() != tests.size())) {
                    throw new IllegalArgumentException("Origins/Modes list size mismatch");
                }

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
                    } else {
                        // Default to self-origin and MAIN mode if not specified
                        map.put("origin", tests.get(i).getTcid());
                        map.put("mode", "MAIN");
                    }
                    batchItems.add(map);
                }

                java.io.File batchFile = java.io.File.createTempFile("gatling_tests_", ".json");
                batchFile.deleteOnExit();
                try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(batchFile))) {
                    writer.write(objectMapper.writeValueAsString(batchItems));
                }

                java.io.File paramsFile = java.io.File.createTempFile("gatling_params_", ".json");
                paramsFile.deleteOnExit();
                try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(paramsFile))) {
                    writer.write(objectMapper.writeValueAsString(params));
                }

                // Create temporary NDJSON file for report streaming
                java.io.File reportFile = java.io.File.createTempFile("gatling_report_", ".ndjson");
                reportFile.deleteOnExit();

                List<String> command = buildGatlingCommand(batchFile.getAbsolutePath(), paramsFile.getAbsolutePath(), reportFile.getAbsolutePath());

                System.out.println("========== Gatling Test(s) Execution (Async) ==========");
                for (int i = 0; i < tests.size(); i++) {
                    System.out.println(String.format("  %d) %s [ %s %s ]", i + 1,
                            tests.get(i).getTcid(),
                            endpoints.get(i).getMethod(),
                            endpoints.get(i).getUrl()));
                }
                System.out.println("Starting Gatling test(s) in background (separate process)...");

                java.lang.ProcessBuilder processBuilder = new java.lang.ProcessBuilder(command);
                processBuilder.redirectErrorStream(true);

                java.lang.Process process = processBuilder.start();

                Map<String, String> testVariables = null;
                
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        if (line.startsWith(VARIABLES_PREFIX)) {
                            String variablesJson = line.substring(VARIABLES_PREFIX.length());
                            try {
                                testVariables = objectMapper.readValue(variablesJson, new TypeReference<Map<String, String>>() {});
                                System.out.println("Received " + testVariables.size() + " test variables from execution");
                            } catch (Exception e) {
                                System.err.println("Failed to parse test variables: " + e.getMessage());
                            }
                        }
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    System.out.println("Gatling test(s) execution completed.");
                    // Process NDJSON report file generated by Simulation
                    processAndSaveReportsFromFile(reportFile.getAbsolutePath(), tests);
                    if (testVariables != null && !testVariables.isEmpty()) {
                        // Load test variables into TestRunContext for subsequent use
                        TestRunContext.clear();
                        for (Map.Entry<String, String> entry : testVariables.entrySet()) {
                            String[] parts = entry.getKey().split("\\.", 2);
                            if (parts.length == 2) {
                                TestRunContext.saveVariable(parts[0], parts[1], entry.getValue());
                            }
                        }
                    }
                } else {
                    System.out.println("Gatling test(s) execution failed, exit code: " + exitCode);
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test(s) Failed, exit code: " + exitCode, com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                }

            } catch (Exception e) {
                System.err.println("Failed to start Gatling test(s) process: " + e.getMessage());
                e.printStackTrace();
                com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Gatling Test(s) Exception: " + e.getMessage(), com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
            } finally {
                if (onComplete != null) {
                    if (javafx.application.Platform.isFxApplicationThread()) {
                        onComplete.run();
                    } else {
                        javafx.application.Platform.runLater(onComplete);
                    }
                }
            }
        }, "test-runner").start();
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

            // Use LinkedHashMap to maintain execution order
            Map<String, FunctionalTestReport> reportMap = new LinkedHashMap<>();

            // Now process the grouped results
            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByOrigin.entrySet()) {
                String originTcid = entry.getKey();
                List<Map<String, Object>> itemsForOrigin = entry.getValue();
                
                // If this TCID is not in our report mapping (possibly a dependency), add it
                if (!reportMap.containsKey(originTcid)) {
                    FunctionalTestReport report = new FunctionalTestReport();
                    report.setOriginTcid(originTcid);
                    report.setExecutedAt(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    reportMap.put(originTcid, report);
                }
                
                FunctionalTestReport finalReport = reportMap.get(originTcid);

                // Group by mode directly based on each entry to accurately separate SETUP / MAIN / TEARDOWN
                Map<TestMode, List<CaseReport>> casesByMode = new java.util.EnumMap<>(TestMode.class);
                for (Map<String, Object> itemMap : itemsForOrigin) {
                    String modeStr = (String) itemMap.getOrDefault("mode", "MAIN");
                    TestMode mode;
                    try {
                        mode = TestMode.valueOf(modeStr);
                    } catch (IllegalArgumentException ex) {
                        // Treat unknown modes as MAIN
                        mode = TestMode.MAIN;
                    }
                    CaseReport cr = mapper.convertValue(itemMap.get("report"), CaseReport.class);
                    casesByMode.computeIfAbsent(mode, k -> new java.util.ArrayList<>()).add(cr);
                }

                List<ModeGroup> modeGroups = new ArrayList<>();
                // Define display order including new modes
                List<TestMode> displayOrder = java.util.Arrays.asList(
                        TestMode.SETUP,
                        TestMode.DIFF_PRE,
                        TestMode.PRE_CHECK,
                        TestMode.MAIN,
                        TestMode.PST_CHECK,
                        TestMode.DIFF_PST,
                        TestMode.TEARDOWN
                );
                displayOrder.forEach(mode -> {
                    if (casesByMode.containsKey(mode)) {
                        ModeGroup group = new ModeGroup();
                        group.setMode(mode);
                        group.setCases(casesByMode.get(mode));
                        modeGroups.add(group);
                    }
                });

                finalReport.setGroups(modeGroups);
                
                // Set the passed status of the report
                boolean allPassed = modeGroups.stream().allMatch(g -> g.getCases().stream().allMatch(CaseReport::isPassed));
                finalReport.setPassed(allPassed);

                // Update test status
                for (GatlingTest test : executedTests) {
                    if (test.getTcid().equals(originTcid)) {
                        try {
                            test.setLastRunPassed(allPassed);
                            testDao.updateTest(test);
                        } catch (Exception e) {
                            System.err.println("Failed to update test status for " + test.getTcid() + ": " + e.getMessage());
                        }
                        break;
                    }
                }
            }
            
            // Convert to list, maintain order
            List<FunctionalTestReport> aggregatedReports = new ArrayList<>(reportMap.values());

            // Generate an aggregated report
            if (!aggregatedReports.isEmpty()) {
                String batchTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String batchFileName = String.format("functional_report_%s.json", batchTimestamp);
                Path reportDir = Paths.get(System.getProperty("user.dir"), "target", "gatling", "reports");
                Files.createDirectories(reportDir);
                Path batchReportPath = reportDir.resolve(batchFileName);

                try (Writer writer = new BufferedWriter(new FileWriter(batchReportPath.toFile()))) {
                    mapper.writeValue(writer, aggregatedReports);
                    System.out.println("Aggregated report saved to: " + batchReportPath);
                }
                
                // Update each test, set the report path to the aggregated report
                for (FunctionalTestReport report : aggregatedReports) {
                    final String originTcid = report.getOriginTcid();
                    executedTests.stream()
                            .filter(t -> t.getTcid().equals(originTcid))
                            .findFirst()
                            .ifPresent(testToUpdate -> {
                                try {
                                    testToUpdate.setReportPath(batchReportPath.toString());
                                    testDao.updateTest(testToUpdate);
                                } catch (Exception e) {
                                    System.err.println("Failed to update report path for test " + originTcid + ": " + e.getMessage());
                                }
                            });
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to process reports: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // New helper: read NDJSON report file and reuse existing aggregation logic
    private static void processAndSaveReportsFromFile(String reportFilePath, List<GatlingTest> executedTests) {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(reportFilePath));
            if (lines.isEmpty()) {
                System.err.println("Report file is empty: " + reportFilePath);
                return;
            }
            // Concatenate lines into JSON array for reuse of existing logic
            String joined = "[" + String.join(",", lines) + "]";
            processAndSaveReports(joined, executedTests);
        } catch (Exception e) {
            System.err.println("Failed to read/process report file: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 