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

    private static String assembleClasspath() {
        // Prefer classpath provided by Maven build plugin (read from a file to avoid command line length limits)
        String classpathFile = System.getProperty("gatling.classpath.file");
        if (classpathFile != null) {
            try {
                String dependencyClasspath = new String(Files.readAllBytes(Paths.get(classpathFile)));
                String projectClassesPath = Paths.get(System.getProperty("user.dir"), "target", "classes").toString();
                // Prepend the project's own classes to the classpath
                return projectClassesPath + java.io.File.pathSeparator + dependencyClasspath;
            } catch (IOException e) {
                System.err.println("WARN: Failed to read classpath from " + classpathFile + ", falling back to default.");
            }
        }

        // Fallback to original method for other environments (e.g., IDE, fat JAR)
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

    private static List<String> buildGatlingCommand(String testsFilePath, String paramsFilePath) throws java.net.URISyntaxException {
        String javaHome = System.getProperty("java.home");
        String javaBin = java.nio.file.Paths.get(javaHome, "bin", "java").toString();
        String classpath = assembleClasspath();
        String gatlingMain = "io.gatling.app.Gatling";
        String simulationClass = DynamicJavaSimulation.class.getName();
        String resultsPath = java.nio.file.Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();

        URL logbackUrl = GatlingTestExecutor.class.getClassLoader().getResource("logback.xml");
        String logbackPath = (logbackUrl != null) ? new File(logbackUrl.toURI()).getAbsolutePath() : null;

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("--add-opens");
        command.add("java.base/java.lang=ALL-UNNAMED");
        command.add("-cp");
        command.add(classpath);
        if (logbackPath != null) {
            command.add("-Dlogback.configurationFile=" + logbackPath);
        }
        command.add("-Dgatling.tests.file=" + testsFilePath);
        command.add("-Dgatling.params.file=" + paramsFilePath);
        command.add(gatlingMain);
        command.add("-s");
        command.add(simulationClass);
        command.add("-rf");
        command.add(resultsPath);
        return command;
    }

    // Async batch execution with completion callback
    public static void executeBatch(java.util.List<GatlingTest> tests, GatlingLoadParameters params,
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

                List<String> command = buildGatlingCommand(batchFile.getAbsolutePath(), paramsFile.getAbsolutePath());

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
                    if (jsonResult != null) {
                        processAndSaveReports(jsonResult, tests);
                    }
                } else {
                    System.out.println("Gatling batch execution failed, exit code: " + exitCode);
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test(s) Failed, exit code: " + exitCode, com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                }

            } catch (Exception e) {
                System.err.println("Failed to start Gatling batch process: " + e.getMessage());
                e.printStackTrace();
                com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Test(s) Exception: " + e.getMessage(), com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
            } finally {
                if (onComplete != null) {
                    if (javafx.application.Platform.isFxApplicationThread()) {
                        onComplete.run();
                    } else {
                        javafx.application.Platform.runLater(onComplete);
                    }
                }
            }
        }, "gatling-batch-runner").start();
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

            // Collect all final reports for batch aggregation
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
                aggregatedReports.add(finalReport);
            }

            // Always persist an aggregated report
            Path batchReportPath = null;
            if (!aggregatedReports.isEmpty()) {
                String batchTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String batchFileName = String.format("functional_batch_%s.json", batchTimestamp);
                Path reportDir = Paths.get(System.getProperty("user.dir"), "target", "gatling", "reports");
                Files.createDirectories(reportDir);
                batchReportPath = reportDir.resolve(batchFileName);

                try (Writer writer = new BufferedWriter(new FileWriter(batchReportPath.toFile()))) {
                    mapper.writeValue(writer, aggregatedReports);
                    System.out.println("Aggregated report saved to: " + batchReportPath);
                }
            }

            // Update each test in the database with its status and the path to the batch report if it exists.
            for (FunctionalTestReport report : aggregatedReports) {
                final String originTcid = report.getOriginTcid();

                final boolean overallPassed = report.getGroups().stream()
                        .flatMap(mg -> mg.getCases().stream())
                        .allMatch(CaseReport::isPassed);

                final Path finalBatchReportPath = batchReportPath;
                executedTests.stream()
                        .filter(t -> t.getTcid().equals(originTcid))
                        .findFirst()
                        .ifPresent(testToUpdate -> {
                            try {
                                if (finalBatchReportPath != null) {
                                    testToUpdate.setReportPath(finalBatchReportPath.toString());
                                }
                                testToUpdate.setLastRunPassed(overallPassed);
                                testDao.updateTest(testToUpdate);
                                System.out.println("Updated test '" + originTcid + "' with pass_status=" + overallPassed);
                            } catch (Exception e) {
                                System.err.println("Failed to update test in DB: " + originTcid);
                                e.printStackTrace();
                            }
                        });
            }

        } catch (Exception e) {
            System.err.println("Failed to parse or process functional test report from JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

} 