package com.qa.app.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.GatlingTest;
import com.qa.app.model.ResponseCheck;
import com.qa.app.model.CheckType;
import com.qa.app.model.reports.*;
import com.qa.app.model.threadgroups.*;
import io.gatling.http.response.Response;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;


public class DynamicJavaSimulation extends Simulation {

    // Static holder for results, accessible from within the same JVM
    public static final Map<String, List<ResponseCheck>> lastRunResults = new ConcurrentHashMap<>();

    private final GatlingLoadParameters params;
    private final List<BatchItem> batchItems;
    private final boolean isBatchMode;
    private final Map<String, List<ResponseCheck>> checkResults = new ConcurrentHashMap<>();
    private final Map<String, CaseReport> caseReports = new ConcurrentHashMap<>();
    private static final String REPORT_PREFIX = "REPORT_JSON:";
    private static final String CHECK_REPORTS_KEY = "checkReports";
    private static final List<String> RESPONSE_HEADERS_TO_CAPTURE = java.util.Arrays.asList(
            "Content-Type",
            "Set-Cookie",
            "Location",
            "Content-Length",
            "Server"
    );


    private static class BatchItem {
        public GatlingTest test;
        public Endpoint endpoint;
        // Optional dependency metadata
        public String origin; // 主用例 TCID
        public String mode;   // SETUP | MAIN | TEARDOWN

        public TestMode getTestMode() {
            if (mode == null || mode.isBlank()) {
                return TestMode.MAIN;
            }
            try {
                return TestMode.valueOf(mode.toUpperCase());
            } catch (IllegalArgumentException e) {
                return TestMode.CONDITION; // Fallback for unknown modes
            }
        }
    }

    {
        // Clear results at the beginning of a simulation run.
        lastRunResults.clear();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.params = objectMapper.readValue(new File(System.getProperty("gatling.params.file")), GatlingLoadParameters.class);

            String batchFilePath = System.getProperty("gatling.tests.file");
            if (batchFilePath != null && !batchFilePath.isEmpty()) {
                // Batch mode
                this.batchItems = objectMapper.readValue(new File(batchFilePath), new TypeReference<List<BatchItem>>(){});
                this.isBatchMode = true;
            } else {
                // Single test mode (backward compatibility)
                BatchItem item = new BatchItem();
                item.test = objectMapper.readValue(new File(System.getProperty("gatling.test.file")), GatlingTest.class);
                item.endpoint = objectMapper.readValue(new File(System.getProperty("gatling.endpoint.file")), Endpoint.class);
                this.batchItems = java.util.Collections.singletonList(item);
                this.isBatchMode = false;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize simulation parameters from file", e);
        }

        HttpProtocolBuilder httpProtocol = createHttpProtocol(batchItems.get(0).endpoint);
        ScenarioBuilder scn = createScenario();

        if (!isBatchMode) {
            System.out.println("DynamicJavaSimulation: Single test mode for TCID: " + batchItems.get(0).test.getTcid());
        } else {
            System.out.println("DynamicJavaSimulation: Batch mode with " + batchItems.size() + " test(s)");
            for (int i = 0; i < batchItems.size(); i++) {
                BatchItem bi = batchItems.get(i);
                System.out.println(String.format("  %d) %s [ %s %s ]", i + 1,
                        bi.test.getTcid(),
                        bi.endpoint.getMethod(),
                        bi.endpoint.getUrl()));
            }
        }

        PopulationBuilder populationBuilder = buildInjectionProfile(scn);
        SetUp setup = setUp(populationBuilder).protocols(httpProtocol);

        if (params.getType() == ThreadGroupType.STANDARD && params.getStandardThreadGroup().isScheduler()) {
            StandardThreadGroup standardConfig = params.getStandardThreadGroup();
            setup.maxDuration(Duration.ofSeconds(standardConfig.getDuration() + standardConfig.getDelay()));
        } else if (params.getType() == ThreadGroupType.STEPPING) {
            SteppingThreadGroup steppingConfig = params.getSteppingThreadGroup();

            // Based on JMeter Stepping TG semantics: initialDelay + total rampUp duration + holdLoad
            long totalRampUp = 0;

            // Initial user ramp time (only counted if startUsers > 0)
            if (steppingConfig.getStartUsers() > 0) {
                totalRampUp += steppingConfig.getIncrementTime();
            }

            // Calculate the number of subsequent increment batches
            int remainingUsers = steppingConfig.getNumThreads() - steppingConfig.getStartUsers();
            if (remainingUsers > 0 && steppingConfig.getIncrementUsers() > 0) {
                int numberOfSteps = (int) Math.ceil((double) remainingUsers / steppingConfig.getIncrementUsers());
                totalRampUp += (long) numberOfSteps * steppingConfig.getIncrementTime();
            }

            long totalDuration = (long) steppingConfig.getInitialDelay() + totalRampUp + steppingConfig.getHoldLoad();

            setup.maxDuration(Duration.ofSeconds(totalDuration));
        } else if (params.getType() == ThreadGroupType.ULTIMATE) {
            long maxDuration = 0;
            for (UltimateThreadGroupStep step : params.getUltimateThreadGroup().getSteps()) {
                long stepEndTime = (long) step.getStartTime() + step.getStartupTime() + step.getHoldTime() + step.getShutdownTime();
                if (stepEndTime > maxDuration) {
                    maxDuration = stepEndTime;
                }
            }
            setup.maxDuration(Duration.ofSeconds(maxDuration));
        }

        // No inline after hook here; see overridden after() method at class bottom.
    }

    private PopulationBuilder buildInjectionProfile(ScenarioBuilder scn) {
        switch (params.getType()) {
            case STEPPING:
                SteppingThreadGroup steppingConfig = params.getSteppingThreadGroup();
                System.out.println("- Load Profile: STEPPING");
                System.out.println("  - Total Threads: " + steppingConfig.getNumThreads());
                System.out.println("  - Initial Delay: " + steppingConfig.getInitialDelay() + "s");
                System.out.println("  - Initial Users: " + steppingConfig.getStartUsers());
                System.out.println("  - Increment Users: " + steppingConfig.getIncrementUsers());
                System.out.println("  - Increment Interval: " + steppingConfig.getIncrementTime() + "s");
                System.out.println("  - Hold Load For: " + steppingConfig.getHoldLoad() + "s");

                List<OpenInjectionStep> steps = new ArrayList<>();

                // 1. Initial delay
                if (steppingConfig.getInitialDelay() > 0) {
                    steps.add(nothingFor(Duration.ofSeconds(steppingConfig.getInitialDelay())));
                }

                // 2. Ramp-up initial users
                int rampDurationSec = Math.max(1, steppingConfig.getIncrementTime());
                steps.add(rampUsers(steppingConfig.getStartUsers()).during(Duration.ofSeconds(rampDurationSec)));

                // 3. Subsequent increments – Each batch of users is gradually ramped up in a linear ramp-up process
                int remainingUsers = steppingConfig.getNumThreads() - steppingConfig.getStartUsers();
                if (remainingUsers > 0 && steppingConfig.getIncrementUsers() > 0) {
                    int numberOfSteps = (int) Math.ceil((double) remainingUsers / steppingConfig.getIncrementUsers());
                    for (int i = 0; i < numberOfSteps; i++) {
                        int usersInThisStep = Math.min(steppingConfig.getIncrementUsers(), remainingUsers - (i * steppingConfig.getIncrementUsers()));
                        if (usersInThisStep > 0) {
                           steps.add(rampUsers(usersInThisStep).during(Duration.ofSeconds(rampDurationSec)));
                        }
                    }
                }
                
                // 4. Hold load
                // The hold is managed by the scenario lifetime (threadLifetime)
                // The injection profile only defines how users arrive.
                
                return scn.injectOpen(steps.toArray(new OpenInjectionStep[0]));


            case ULTIMATE:
                UltimateThreadGroup ultimateConfig = params.getUltimateThreadGroup();
                System.out.println("- Load Profile: ULTIMATE");
                if (ultimateConfig.getSteps().isEmpty()) {
                    System.out.println("  - No steps defined, running with 1 user for 1 second.");
                    return scn.injectOpen(atOnceUsers(1));
                }

                // Sort steps by start time
                List<UltimateThreadGroupStep> sortedSteps = new ArrayList<>(ultimateConfig.getSteps());
                sortedSteps.sort(java.util.Comparator.comparingInt(UltimateThreadGroupStep::getStartTime));
                
                List<OpenInjectionStep> injectionSteps = new ArrayList<>();
                long lastStepEndTime = 0;

                for (UltimateThreadGroupStep step : sortedSteps) {
                    System.out.println(String.format("  - Step: start at %ds, initial %d users, startup %ds, hold %ds, shutdown %ds",
                            step.getStartTime(), step.getInitialLoad(), step.getStartupTime(), step.getHoldTime(), step.getShutdownTime()));
                    
                    // 1. Calculate delay from last step's end
                    long delay = step.getStartTime() - lastStepEndTime;
                    if (delay > 0) {
                        injectionSteps.add(nothingFor(Duration.ofSeconds(delay)));
                    }

                    // 2. Add the ramp-up phase
                    injectionSteps.add(rampUsers(step.getInitialLoad()).during(Duration.ofSeconds(step.getStartupTime())));
                    
                    // 3. Add hold time
                    // In Gatling, users keep running. The hold is implicit until the next phase or test end.
                    // We can add a pause if we want to model sequential phases rather than overlapping ones.
                    // For now, we assume phases can overlap. The total duration will be controlled by maxDuration.

                    // Update the end time for the next step's calculation
                    lastStepEndTime = step.getStartTime() + step.getStartupTime();
                }
                return scn.injectOpen(injectionSteps.toArray(new OpenInjectionStep[0]));


            case STANDARD:
            default:
                StandardThreadGroup standardConfig = params.getStandardThreadGroup();
                System.out.println("- Load Profile: STANDARD");
                System.out.println("  - Threads: " + standardConfig.getNumThreads());
                System.out.println("  - Ramp-up: " + standardConfig.getRampUp() + "s");

                if (standardConfig.isScheduler()) {
                    System.out.println("  - Scheduler Enabled");
                    System.out.println("  - Startup Delay: " + standardConfig.getDelay() + "s");
                    System.out.println("  - Duration: " + standardConfig.getDuration() + "s");
                    return scn.injectOpen(
                            nothingFor(Duration.ofSeconds(standardConfig.getDelay())),
                            rampUsers(standardConfig.getNumThreads()).during(Duration.ofSeconds(standardConfig.getRampUp()))
                    );
                } else {
                    System.out.println("  - Loops: " + standardConfig.getLoops());
                     return scn.injectOpen(
                        rampUsers(standardConfig.getNumThreads()).during(Duration.ofSeconds(standardConfig.getRampUp()))
                     );
                }
        }
    }

    private HttpProtocolBuilder createHttpProtocol(Endpoint endpoint) {
        return http
                .baseUrl(endpoint.getUrl())
                .acceptHeader("application/json, text/plain, */*")
                .acceptEncodingHeader("gzip, deflate, br")
                .acceptLanguageHeader("en-US,en;q=0.9");
    }

    private ScenarioBuilder createScenario() {
        ChainBuilder chain = createHttpChain();

        // Use fixed name for debugging
        String scenarioName = "TempScenario";

        StandardThreadGroup standardConfig = null;
        if (params.getType() == ThreadGroupType.STANDARD) {
            standardConfig = params.getStandardThreadGroup();
        }

        if (standardConfig != null && !standardConfig.isScheduler() && standardConfig.getLoops() > 0) {
            return scenario(scenarioName).repeat(standardConfig.getLoops()).on(
                exec(chain)
            );
        } else {
            return scenario(scenarioName).exec(chain);
        }
    }

    private ChainBuilder createHttpChain() {
        if (!isBatchMode) {
            // Single test mode
            return createRequestBuilder(batchItems.get(0));
        } else {
            // Batch mode: chain all request builders together
            ChainBuilder root = exec(session -> session); // Start with a no-op
            for (BatchItem item : batchItems) {
                root = root.exec(createRequestBuilder(item));
            }
            return root;
        }
    }

    private ChainBuilder createRequestBuilder(BatchItem item) {
        GatlingTest test = item.test;
        Endpoint endpoint = item.endpoint;
        RuntimeTemplateProcessor templateProcessor = new RuntimeTemplateProcessor();

        // Build composite key: tcid|mode to separate reports for different execution contexts
        String reportKey = test.getTcid() + "|" + item.getTestMode().name();

        // Ensure a CaseReport container exists for this key
        caseReports.computeIfAbsent(reportKey, k -> {
            CaseReport cr = new CaseReport();
            cr.setTcid(test.getTcid());
            cr.setItems(Collections.synchronizedList(new ArrayList<>()));
            return cr;
        });

        // Use simple TCID as request name (no suffix)
        final String requestName = test.getTcid();

        HttpRequestActionBuilder request;
        String method = endpoint.getMethod() == null ? "GET" : endpoint.getMethod().toUpperCase();

        switch (method) {
            case "POST":
                request = http(requestName).post(endpoint.getUrl());
                break;
            case "PUT":
                request = http(requestName).put(endpoint.getUrl());
                break;
            case "DELETE":
                request = http(requestName).delete(endpoint.getUrl());
                break;
            case "GET":
            default:
                request = http(requestName).get(endpoint.getUrl());
                break;
        }

        // Dynamically process headers for each request using a session function
        Map<String, String> headerTemplates = parseHeaders(test.getHeaders());
        for (Map.Entry<String, String> headerEntry : headerTemplates.entrySet()) {
            request = request.header(headerEntry.getKey(), session ->
                    RuntimeTemplateProcessor.render(headerEntry.getValue(), test.getHeadersDynamicVariables()));
        }

        // Dynamically process body for each request using a session function
        if (test.getBody() != null && !test.getBody().trim().isEmpty()) {
            request = request.body(StringBody(session ->
                    RuntimeTemplateProcessor.render(test.getBody(), test.getBodyDynamicVariables())));
        }

        List<CheckBuilder> checkBuilders = new ArrayList<>();
        List<ChainBuilder> loggingActions = new ArrayList<>();
        boolean statusCheckExists = false;

        String checksJson = test.getResponseChecks();
        if (checksJson != null && !checksJson.isEmpty()) {
            try {
                List<ResponseCheck> checks = new ObjectMapper().readValue(checksJson, new TypeReference<>() {});

                // Initialize check reports list in session for the first check
                loggingActions.add(exec(session -> session.set(CHECK_REPORTS_KEY, new ArrayList<CheckReport>())));

                for (ResponseCheck responseCheck : checks) {
                    // This is a deep copy. Critical for concurrent runs.
                    final ResponseCheck currentCheck = new ResponseCheck(responseCheck);

                    switch (currentCheck.getType()) {
                        case STATUS: {
                            statusCheckExists = true;
                            // Status check in Gatling is special, it doesn't use a regular extractor.
                            // We save the status and check it in a subsequent action.
                            String statusSaveKey = "response_status_" + currentCheck.hashCode();
                            checkBuilders.add(status().saveAs(statusSaveKey));

                            loggingActions.add(exec(session -> {
                                CheckReport checkReport = new CheckReport();
                                checkReport.setType(currentCheck.getType());
                                checkReport.setOperator(currentCheck.getOperator());
                                checkReport.setExpect(currentCheck.getExpect());

                                try {
                                    String actualStatus;
                                    if (session.contains(statusSaveKey)) {
                                        // Gatling 3.7+ saves status as Integer
                                        Object statusObj = session.get(statusSaveKey);
                                        actualStatus = String.valueOf(statusObj);
                                    } else {
                                        actualStatus = "<STATUS NOT FOUND>";
                                    }
                                    
                                    checkReport.setActual(actualStatus);

                                    boolean checkPassed = false;
                                    switch (currentCheck.getOperator()) {
                                        case CONTAINS:
                                            checkPassed = actualStatus.contains(currentCheck.getExpect());
                                            break;
                                        case IS:
                                        default:
                                            checkPassed = actualStatus.equals(currentCheck.getExpect());
                                            break;
                                    }
                                    checkReport.setPassed(checkPassed);

                                    if (!checkPassed) {
                                        // Logging to console is still useful for live debugging
                                        System.out.printf("\u001B[31mCHECK_FAIL|%s|...|%s|actual:%s\u001B[0m%n",
                                                test.getTcid(), currentCheck.getExpect(), actualStatus);
                                        // This is a non-fatal check for reporting, but we can still mark session as failed
                                        // return session.markAsFailed();
                                    }
                                    // Add to list in session
                                    session.getList(CHECK_REPORTS_KEY).add(checkReport);
                                    return session.remove(statusSaveKey);
                                } catch (Exception e) {
                                    System.err.println("[ERROR] Exception in STATUS check: " + e.getMessage());
                                    checkReport.setActual("ERROR: " + e.getMessage());
                                    checkReport.setPassed(false);
                                    session.getList(CHECK_REPORTS_KEY).add(checkReport);
                                    return session;
                                }
                            }));
                            break;
                        }
                        case JSON_PATH:
                        case XPATH:
                        case REGEX: {
                            String saveAsKey = (currentCheck.getSaveAs() != null && !currentCheck.getSaveAs().isBlank())
                                ? currentCheck.getSaveAs() : "temp_check_var_" + currentCheck.hashCode();

                            CheckBuilder extractor = switch (currentCheck.getType()) {
                                case JSON_PATH: yield jsonPath(currentCheck.getExpression()).saveAs(saveAsKey);
                                case XPATH: yield xpath(currentCheck.getExpression()).saveAs(saveAsKey);
                                case REGEX: yield regex(currentCheck.getExpression()).saveAs(saveAsKey);
                                default: throw new IllegalStateException("Unexpected value: " + currentCheck.getType());
                            };
                            checkBuilders.add(extractor);

                            loggingActions.add(exec(session -> {
                                CheckReport checkReport = new CheckReport();
                                checkReport.setType(currentCheck.getType());
                                checkReport.setExpression(currentCheck.getExpression());
                                checkReport.setOperator(currentCheck.getOperator());
                                checkReport.setExpect(currentCheck.getExpect());

                                String actualValue;
                                if (session.contains(saveAsKey)) {
                                    Object rawValue = session.get(saveAsKey);
                                    if (rawValue instanceof String) {
                                        actualValue = (String) rawValue;
                                    } else if (rawValue instanceof char[]) {
                                        actualValue = new String((char[]) rawValue);
                                    } else if (rawValue instanceof String[] arr) {
                                        actualValue = arr.length > 0 ? arr[0] : "";
                                    } else {
                                        actualValue = String.valueOf(rawValue);
                                    }
                                } else {
                                    actualValue = "<VALUE NOT FOUND>";
                                }
                                checkReport.setActual(actualValue);
                                    
                                    boolean checkPassed = false;
                                    switch (currentCheck.getOperator()) {
                                        case CONTAINS:
                                            checkPassed = actualValue.contains(currentCheck.getExpect());
                                            break;
                                        case IS:
                                        default:
                                            checkPassed = actualValue.equals(currentCheck.getExpect());
                                            break;
                                    }
                                checkReport.setPassed(checkPassed);

                                    if (!checkPassed) {
                                    System.out.printf("\u001B[31mCHECK_FAIL|%s|%s|...|expected:%s|actual:%s\u001B[0m%n",
                                            test.getTcid(), currentCheck.getExpression(), currentCheck.getExpect(), actualValue);
                                }

                                session.getList(CHECK_REPORTS_KEY).add(checkReport);

                                if (saveAsKey.startsWith("temp_check_var_")) {
                                    return session.remove(saveAsKey);
                                }
                                return session;
                            }));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to parse responseChecks: " + e.getMessage());
            }
        }

        // Apply all checks to the request builder
        if (!checkBuilders.isEmpty()) {
            request = request.check(checkBuilders.toArray(new CheckBuilder[0]));
        }

        // Add a default blocking check if no user-defined status check exists
        if (!statusCheckExists) {
            request = request.check(status().is(200));
        }

        // Add checks to save response metadata for reporting
        request = request.check(
            bodyString().saveAs("responseBody"),
            responseTimeInMillis().saveAs("latencyMs"),
            bodyLength().saveAs("sizeBytes")
        );

        // Capture selected response headers
        for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
            String sessionKey = "respHeader_" + headerName.replace("-", "_");
            request = request.check(header(headerName).optional().saveAs(sessionKey));
        }

        // ** REPORTING LOGIC **
        // This chain is executed AFTER the request is complete.
        ChainBuilder reportingChain = exec(session -> {
            RequestReport report = new RequestReport();
            report.setRequestName(requestName);

            // 1. Build RequestInfo (with resolved variables)
            RequestInfo requestInfo = new RequestInfo();
            requestInfo.setMethod(method);
            requestInfo.setUrl(endpoint.getUrl());

            // Re-process templates to get the resolved values for the report
            String finalHeaders = RuntimeTemplateProcessor.render(test.getHeaders(), test.getHeadersDynamicVariables());
            Map<String, String> finalHeadersMap = parseHeaders(finalHeaders);
            requestInfo.setHeaders(finalHeadersMap);

            if (test.getBody() != null && !test.getBody().trim().isEmpty()) {
                String finalBody = RuntimeTemplateProcessor.render(test.getBody(), test.getBodyDynamicVariables());
                requestInfo.setBody(finalBody);
            }
            report.setRequest(requestInfo);

            // 2. Build ResponseInfo
            ResponseInfo responseInfo = new ResponseInfo();
            if (session.contains("responseBody")) {
                responseInfo.setBodySample(session.getString("responseBody"));
            } else {
                responseInfo.setBodySample("Response body not captured.");
            }
            // Note: Gatling Response object is not directly available here.
            // We rely on checks to get status, and other metrics are not easily available post-request.
            // This is a limitation of this reporting approach.
            // We can get status from our check reports.
            responseInfo.setStatus(0); // Default, will be overwritten by status check
            if (session.contains("latencyMs")) {
                responseInfo.setLatencyMs(session.getLong("latencyMs"));
            }
            if (session.contains("sizeBytes")) {
                responseInfo.setSizeBytes((long) session.getInt("sizeBytes"));
            }
            // 由于技术限制，我们无法直接获取所有响应头
            Map<String, String> capturedHeaders = new HashMap<>();
            for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                String key = "respHeader_" + headerName.replace("-", "_");
                if (session.contains(key)) {
                    capturedHeaders.put(headerName, session.getString(key));
                }
            }
            responseInfo.setHeaders(capturedHeaders);

            // 3. Collect CheckReports
            if (session.contains(CHECK_REPORTS_KEY)) {
                List<CheckReport> reports = session.getList(CHECK_REPORTS_KEY);
                report.setChecks(reports);

                // Find status from checks to populate ResponseInfo
                reports.stream()
                        .filter(r -> r.getType() == CheckType.STATUS && r.getActual() != null)
                        .findFirst()
                        .ifPresent(r -> {
                            try {
                                responseInfo.setStatus(Integer.parseInt(r.getActual()));
                            } catch (NumberFormatException e) {
                                // Ignore if status is not a number
                            }
                        });
            } else {
                report.setChecks(new ArrayList<>());
            }
            report.setResponse(responseInfo);
            
            // 4. Set overall pass/fail status
            boolean allChecksPassed = report.getChecks().stream().allMatch(CheckReport::isPassed);
            report.setPassed(allChecksPassed);
            report.setStatus(String.valueOf(responseInfo.getStatus()));


            // 5. Add the completed report to the main case report
            caseReports.get(reportKey).getItems().add(report);

            // Clear the check reports for the next request in the chain
            io.gatling.javaapi.core.Session cleaned = session.remove(CHECK_REPORTS_KEY)
                    .remove("responseBody")
                    .remove("latencyMs")
                    .remove("sizeBytes");
            for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                cleaned = cleaned.remove("respHeader_" + headerName.replace("-", "_"));
            }
            return cleaned;
        });

        // Link the request and the reporting logic
        return exec(request).exec(loggingActions).exec(reportingChain);
    }

    private Map<String, String> parseHeaders(String headersString) {
        if (headersString == null || headersString.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // Attempt to parse as JSON first
            return new ObjectMapper().readValue(headersString, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            // Fallback to key: value line-by-line parsing
            Map<String, String> headers = new java.util.HashMap<>();
            String[] lines = headersString.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        headers.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
            return headers;
        }
    }

    @Override
    public void after() {
        try {
            // Finalize all case reports by calculating overall pass/fail status
            for (CaseReport caseReport : caseReports.values()) {
                boolean allPassed = caseReport.getItems().stream()
                        .allMatch(item -> item.getChecks().stream().allMatch(CheckReport::isPassed));
                caseReport.setPassed(allPassed);
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> summaryList = new ArrayList<>();

            for (BatchItem item : batchItems) {
                String reportKey = item.test.getTcid() + "|" + item.getTestMode().name();
                CaseReport report = caseReports.get(reportKey);
                if (report != null) {
                    Map<String, Object> summaryEntry = new HashMap<>();
                    summaryEntry.put("origin", item.origin != null ? item.origin : item.test.getTcid());
                    summaryEntry.put("tcid", item.test.getTcid());
                    summaryEntry.put("mode", item.getTestMode().name());
                    summaryEntry.put("report", report);
                    summaryList.add(summaryEntry);
                }
            }

            // Serialize to compact JSON (single line)
            String json = mapper.writeValueAsString(summaryList);

            // Use the new prefix for the report
            System.out.println(REPORT_PREFIX + json);

        } catch (Exception ex) {
            System.err.println("Failed to output response check results: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
} 