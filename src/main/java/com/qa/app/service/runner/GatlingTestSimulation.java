package com.qa.app.service.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.GatlingTest;
import com.qa.app.model.ResponseCheck;
import com.qa.app.model.CheckType;
import com.qa.app.model.DbConnection;
import com.qa.app.model.reports.*;
import com.qa.app.model.threadgroups.*;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import com.qa.app.dao.impl.GatlingTestDaoImpl;
import com.qa.app.dao.impl.EndpointDaoImpl;
import com.qa.app.service.impl.BodyTemplateServiceImpl;
import com.qa.app.service.impl.HeadersTemplateServiceImpl;
import com.qa.app.model.BodyTemplate;
import com.qa.app.model.HeadersTemplate;
import com.qa.app.service.api.IDbConnectionService;
import com.qa.app.service.impl.DbConnectionServiceImpl;
import com.qa.app.model.Operator;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;


public class GatlingTestSimulation extends Simulation {

    // Static holder for results, accessible from within the same JVM
    public static final Map<String, List<ResponseCheck>> lastRunResults = new ConcurrentHashMap<>();

    private final GatlingLoadParameters params;
    private final List<BatchItem> batchItems;
    private final boolean isBatchMode;
    private final Map<String, CaseReport> caseReports = new ConcurrentHashMap<>();
    private final IDbConnectionService dbConnectionService = new DbConnectionServiceImpl();
    private static final String VARIABLES_PREFIX = "TEST_VARIABLES:";
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
        public String origin; // Main test TCID
        public String mode;   // SETUP | MAIN | TEARDOWN

        public TestMode getTestMode() {
            if (mode == null || mode.isBlank()) {
                return TestMode.MAIN;
            }
            try {
                return TestMode.valueOf(mode.toUpperCase());
            } catch (IllegalArgumentException e) {
                return TestMode.MAIN; // Fallback to MAIN for unknown
            }
        }
    }

    {
        // Clear results at the beginning of a simulation run.
        lastRunResults.clear();
        
        // Clear test run context
        TestRunContext.clear();
        
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

        // ===================== NEW: Re-resolve endpoint for SETUP / TEARDOWN =====================
        if (item.getTestMode() == com.qa.app.model.reports.TestMode.SETUP ||
            item.getTestMode() == com.qa.app.model.reports.TestMode.TEARDOWN) {
            try {
                // Re-fetch by name & current environment to ensure we get the correct URL template
                if (test.getEndpointName() != null && !test.getEndpointName().isBlank()) {
                    Integer envId = com.qa.app.service.EnvironmentContext.getCurrentEnvironmentId();
                    EndpointDaoImpl epDao = new EndpointDaoImpl();
                    Endpoint resolvedEp = epDao.getEndpointByNameAndEnv(test.getEndpointName(), envId);
                    if (resolvedEp != null) {
                        endpoint = resolvedEp;
                    }
                }
            } catch (Exception e) {
                System.err.println("[WARN] Failed to resolve endpoint for SETUP/TEARDOWN: " + e.getMessage());
            }
        }
        // =========================================================================================

        // Build composite key to ensure uniqueness across different origins.
        // For SETUP and TEARDOWN steps that may be shared by multiple main TCIDs,
        // include the origin TCID in the key: origin|tcid|mode. This prevents collisions
        // where the same setup/teardown test is executed for multiple parents and
        // guarantees the reports are attributed to the correct origin.
        String reportKey;
        if (item.getTestMode() == com.qa.app.model.reports.TestMode.SETUP ||
            item.getTestMode() == com.qa.app.model.reports.TestMode.TEARDOWN) {
            String origin = (item.origin != null && !item.origin.isBlank()) ? item.origin : test.getTcid();
            reportKey = origin + "|" + test.getTcid() + "|" + item.getTestMode().name();
        } else {
            // Original behaviour for MAIN and other modes
            reportKey = test.getTcid() + "|" + item.getTestMode().name();
        }

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

        // Process URL with test variables
        String processedUrl = TestRunContext.processVariableReferences(endpoint.getUrl());
        // Then render user-defined @{var} placeholders
        processedUrl = RuntimeTemplateProcessor.render(processedUrl, test.getEndpointDynamicVariables());

        // Capture the fully resolved URL once for lambdas to avoid non-final variable issue
        final String resolvedUrl = processedUrl;

        switch (method) {
            case "POST":
                request = http(requestName).post(processedUrl);
                break;
            case "PUT":
                request = http(requestName).put(processedUrl);
                break;
            case "DELETE":
                request = http(requestName).delete(processedUrl);
                break;
            case "GET":
            default:
                request = http(requestName).get(processedUrl);
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
        // Collect DIFF checks for special processing (before/after reference)
        List<ResponseCheck> diffChecks = new ArrayList<>();
        // Collect PRE_CHECK and PST_CHECK checks for special processing
        List<ResponseCheck> preChecks = new ArrayList<>();
        List<ResponseCheck> pstChecks = new ArrayList<>();

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

                                    boolean checkPassed = evaluateOperator(currentCheck.getOperator(), actualStatus, currentCheck.getExpect());
                                    checkReport.setPassed(checkPassed);

                                    if (!checkPassed) {
                                        System.out.println(String.format("CHECK_FAIL|%s|%s|%s|expected:%s|actual:%s",
                                                test.getTcid(), "STATUS", currentCheck.getOperator().toString(), currentCheck.getExpect(), actualStatus));
                                    } else {
                                         System.out.println(String.format("CHECK_PASS|%s|%s|%s|expected:%s|actual:%s",
                                                test.getTcid(), "STATUS", currentCheck.getOperator().toString(), currentCheck.getExpect(), actualStatus));
                                    }

                                    // Add to session for reporting
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
                                    actualValue = convertToString(session.get(saveAsKey));
                                } else {
                                    actualValue = "Path not found";
                                }
                                checkReport.setActual(actualValue);
                                
                                boolean checkPassed = evaluateOperator(currentCheck.getOperator(), actualValue, currentCheck.getExpect());
                                checkReport.setPassed(checkPassed);

                                if (!checkPassed) {
                                    System.out.println(String.format("CHECK_FAIL|%s|%s|%s|expected:%s|actual:%s",
                                            test.getTcid(), currentCheck.getExpression(), currentCheck.getOperator().toString(), currentCheck.getExpect(), actualValue));
                                } else {
                                    System.out.println(String.format("CHECK_PASS|%s|%s|%s|expected:%s|actual:%s",
                                            test.getTcid(), currentCheck.getExpression(), currentCheck.getOperator().toString(), currentCheck.getExpect(), actualValue));
                                }

                                // Add to session for reporting
                                session.getList(CHECK_REPORTS_KEY).add(checkReport);

                                // 如果设置了saveAs，则将值保存到TestRunContext中
                                if (currentCheck.getSaveAs() != null && !currentCheck.getSaveAs().isBlank()) {
                                    TestRunContext.saveVariable(test.getTcid(), currentCheck.getSaveAs(), actualValue);
                                }

                                if (saveAsKey.startsWith("temp_check_var_")) {
                                    return session.remove(saveAsKey);
                                }
                                return session;
                            }));
                            break;
                        }
                        case DIFF: {
                            // Defer DIFF processing until after reference requests
                            diffChecks.add(currentCheck);
                            break;
                        }
                        case PRE_CHECK: {
                            // Defer PRE_CHECK processing until after reference requests
                            preChecks.add(currentCheck);
                            break;
                        }
                        case PST_CHECK: {
                            // Defer PST_CHECK processing until after reference requests
                            pstChecks.add(currentCheck);
                            break;
                        }
                        case DB: {
                            // Handle database check
                            loggingActions.add(exec(session -> {
                                CheckReport checkReport = new CheckReport();
                                checkReport.setType(currentCheck.getType());
                                checkReport.setOperator(currentCheck.getOperator());
                                checkReport.setExpect(currentCheck.getExpect());
                                
                                String actualValue = null;
                                boolean checkPassed = false;
                                
                                try {
                                    String alias, sql, column;

                                    // Prioritize new dedicated fields, but fall back to parsing the old expression field
                                    if (currentCheck.getDbAlias() != null && !currentCheck.getDbAlias().isBlank()) {
                                        alias = currentCheck.getDbAlias();
                                        sql = currentCheck.getDbSql();
                                        column = currentCheck.getDbColumn();
                                    } else {
                                        // Backward compatibility for old format stored in the expression field
                                        GatlingScenarioSimulation.DbCheckInfo checkInfo = new ObjectMapper().readValue(currentCheck.getExpression(), GatlingScenarioSimulation.DbCheckInfo.class);
                                        alias = checkInfo.getAlias();
                                        sql = checkInfo.getSql();
                                        column = checkInfo.getColumn();
                                    }

                                    // For reporting purposes, create a unified JSON expression from the DB details.
                                    Map<String, String> dbInfoForReport = new HashMap<>();
                                    dbInfoForReport.put("alias", alias);
                                    dbInfoForReport.put("sql", sql);
                                    dbInfoForReport.put("column", column);
                                    checkReport.setExpression(new ObjectMapper().writeValueAsString(dbInfoForReport));

                                    Integer envIdForConn = com.qa.app.service.EnvironmentContext.getCurrentEnvironmentId();
                                    DbConnection connConfig = dbConnectionService.findByAliasAndEnv(alias, envIdForConn);
                                    if (connConfig == null) { // Fallback to env-agnostic for backward compatibility
                                        connConfig = dbConnectionService.findByAlias(alias);
                                    }
                                    if (connConfig == null) {
                                        throw new RuntimeException("DB Connection alias not found: " + alias);
                                    }

                                    // Process variables in SQL
                                    String processedSql = TestRunContext.processVariableReferences(sql);
                                    Map<String, String> allDynamicVars = new HashMap<>();
                                    if (test.getEndpointDynamicVariables() != null) allDynamicVars.putAll(test.getEndpointDynamicVariables());
                                    if (test.getHeadersDynamicVariables() != null) allDynamicVars.putAll(test.getHeadersDynamicVariables());
                                    if (test.getBodyDynamicVariables() != null) allDynamicVars.putAll(test.getBodyDynamicVariables());
                                    String finalSql = RuntimeTemplateProcessor.render(processedSql, allDynamicVars);

                                    DataSource ds = DataSourceRegistry.get(connConfig);

                                    try (Connection conn = ds.getConnection();
                                         PreparedStatement ps = conn.prepareStatement(finalSql)) {
                                        ResultSet rs = ps.executeQuery();
                                        if (rs.next()) {
                                            actualValue = rs.getString(column);
                                        }
                                    }

                                    checkPassed = evaluateOperator(currentCheck.getOperator(), actualValue, currentCheck.getExpect());
                                    
                                    if (!checkPassed) {
                                        if (!currentCheck.isOptional()) {
                                             System.out.println(String.format("CHECK_FAIL|%s|DB|%s|expected:%s|actual:%s",
                                                test.getTcid(), finalSql, currentCheck.getExpect(), actualValue));
                                        }
                                    } else {
                                        System.out.println(String.format("CHECK_PASS|%s|DB|%s|expected:%s|actual:%s",
                                                test.getTcid(), finalSql, currentCheck.getExpect(), actualValue));
                                    }
                                    
                                    // Save result if specified
                                    if (currentCheck.getSaveAs() != null && !currentCheck.getSaveAs().isBlank()) {
                                        TestRunContext.saveVariable(test.getTcid(), currentCheck.getSaveAs(), actualValue);
                                    }
                                } catch (Exception e) {
                                    System.err.println("[ERROR] Exception in DB check: " + e.getMessage());
                                    actualValue = "ERROR: " + e.getMessage();
                                    checkPassed = false;
                                }

                                checkReport.setActual(actualValue);
                                checkReport.setPassed(checkPassed);
                                
                                // Add to session for reporting
                                session.getList(CHECK_REPORTS_KEY).add(checkReport);
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

        // Apply all checks to the request builder (excluding DIFF)
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
            requestInfo.setUrl(resolvedUrl);

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

        // Main request chain
        ChainBuilder mainChain = exec(request).exec(loggingActions).exec(reportingChain);

        // If no DIFF checks, return main chain directly
        if (diffChecks.isEmpty() && preChecks.isEmpty() && pstChecks.isEmpty()) {
            return mainChain;
        }

        // =====================
        // Build reference BEFORE request(s) for DIFF checks
        // =====================
        ChainBuilder beforeChain = exec(session -> session); // no-op starter

        // Group diff checks by reference TCID to avoid duplicate BEFORE requests
        java.util.Map<String, java.util.List<ResponseCheck>> diffGroupsBefore = diffChecks.stream()
                .filter(dc -> dc.getExpression() != null && dc.getExpression().contains("."))
                .collect(Collectors.groupingBy(
                        dc -> dc.getExpression().substring(0, dc.getExpression().indexOf('.')),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()));

        for (java.util.Map.Entry<String, java.util.List<ResponseCheck>> entry : diffGroupsBefore.entrySet()) {
            String refTcid = entry.getKey();
            java.util.List<ResponseCheck> dcList = entry.getValue();

            // Locate reference endpoint (try batch first, then DB)
            Endpoint refEndpoint = null;
            for (BatchItem bi : batchItems) {
                if (bi.test != null && refTcid.equals(bi.test.getTcid())) {
                    refEndpoint = bi.endpoint;
                    break;
                }
            }
            final GatlingTest refTest = findRefTestByTcid(refTcid);
            if (refTest != null) {
                enrichRefTestTemplates(refTest);
            }
            if (refTest != null) {
                refEndpoint = findEndpointForTest(refTest, endpoint);
            }
            if (refEndpoint == null) continue; // skip if endpoint not found

            String refName = test.getTcid() + "." + refTcid + "_PRE";
            String refMethod = refEndpoint.getMethod() == null ? "GET" : refEndpoint.getMethod().toUpperCase();
            java.util.Map<String,String> dynVars = (refTest != null && refTest.getEndpointDynamicVariables() != null)
                    ? refTest.getEndpointDynamicVariables()
                    : java.util.Collections.emptyMap();
            String refUrl = RuntimeTemplateProcessor.render(
                    TestRunContext.processVariableReferences(refEndpoint.getUrl()),
                    dynVars);

            HttpRequestActionBuilder refReq;
            switch (refMethod) {
                case "POST" -> refReq = http(refName).post(refUrl);
                case "PUT" -> refReq = http(refName).put(refUrl);
                case "DELETE" -> refReq = http(refName).delete(refUrl);
                default -> refReq = http(refName).get(refUrl);
            }

            // Add headers and body from refTest (if available)
            if (refTest != null) {
                java.util.Map<String, String> headersMapBefore = parseHeaders(refTest.getHeaders());
                for (java.util.Map.Entry<String, String> headerEntry : headersMapBefore.entrySet()) {
                    refReq = refReq.header(headerEntry.getKey(), session ->
                            RuntimeTemplateProcessor.render(headerEntry.getValue(), refTest.getHeadersDynamicVariables()));
                }
                if (refTest.getBody() != null && !refTest.getBody().trim().isEmpty()) {
                    refReq = refReq.body(StringBody(session ->
                            RuntimeTemplateProcessor.render(refTest.getBody(), refTest.getBodyDynamicVariables())));
                }
            }

            // Add jsonPath checks for each diff check in this group
            for (ResponseCheck dc : dcList) {
                String path = dc.getExpression().substring(dc.getExpression().indexOf('.') + 1);
                String saveAsKey = "diff_" + dc.hashCode() + "_PRE";
                refReq = refReq.check(jsonPath(path).saveAs(saveAsKey));
            }

            // Add original checks from reference test
            final java.util.List<CheckReport> refBeforeChecks = new java.util.ArrayList<>();
            if (refTest != null && refTest.getResponseChecks() != null && !refTest.getResponseChecks().isBlank()) {
                try {
                    java.util.List<ResponseCheck> origChecks = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(refTest.getResponseChecks(), 
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ResponseCheck>>() {});
                    
                    for (ResponseCheck rc : origChecks) {
                        final String saveKey = "ref_check_" + rc.hashCode();
                        switch (rc.getType()) {
                            case STATUS:
                                refReq = refReq.check(status().saveAs(saveKey));
                                break;
                            case JSON_PATH:
                                refReq = refReq.check(jsonPath(rc.getExpression()).saveAs(saveKey));
                                break;
                            case XPATH:
                                refReq = refReq.check(xpath(rc.getExpression()).saveAs(saveKey));
                                break;
                            case REGEX:
                                refReq = refReq.check(regex(rc.getExpression()).saveAs(saveKey));
                                break;
                            default:
                                continue;
                        }
                        
                        // Create a check report with all data except actual value (filled later)
                        CheckReport cr = new CheckReport();
                        cr.setType(rc.getType());
                        cr.setExpression(rc.getExpression());
                        cr.setOperator(rc.getOperator());
                        cr.setExpect(rc.getExpect());
                        // Store the session key with the report so we can get the actual value later
                        cr.setActual(saveKey); // Temporarily store the key here
                        refBeforeChecks.add(cr);
                    }
                } catch (Exception ignored) {}
            }

            // Always capture status and basic metrics once per request
            refReq = refReq.check(
                    status().is(200),
                    bodyString().saveAs("responseBody"),
                    responseTimeInMillis().saveAs("latencyMs"),
                    bodyLength().saveAs("sizeBytes")
            );
            for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                String sessionKey = "respHeader_" + headerName.replace("-", "_");
                refReq = refReq.check(header(headerName).optional().saveAs(sessionKey));
            }

            // Reporting & variable saving
            ChainBuilder refReporting = exec(s -> {
                String mainTcid = test.getTcid();
                String refRequestReportKey = mainTcid + "|" + refTcid + "|DIFF_PRE";

                // Create a new CaseReport for this reference request
                CaseReport refCaseReport = caseReports.computeIfAbsent(refRequestReportKey, k -> {
                    CaseReport cr = new CaseReport();
                    cr.setTcid(refTcid);
                    cr.setItems(Collections.synchronizedList(new ArrayList<>()));
                    return cr;
                });

                RequestReport rpt = new RequestReport();
                rpt.setRequestName(refName);

                RequestInfo reqInfo = new RequestInfo();
                reqInfo.setMethod(refMethod);
                reqInfo.setUrl(refUrl);
                java.util.Map<String, String> finalHeadersMap = new java.util.HashMap<>();
                if (refTest != null && refTest.getHeaders() != null && !refTest.getHeaders().isEmpty()) {
                    String finalHeaders = RuntimeTemplateProcessor.render(refTest.getHeaders(), refTest.getHeadersDynamicVariables());
                    finalHeadersMap = parseHeaders(finalHeaders);
                }
                reqInfo.setHeaders(finalHeadersMap);
                if (refTest != null && refTest.getBody() != null && !refTest.getBody().trim().isEmpty()) {
                    String finalBody = RuntimeTemplateProcessor.render(refTest.getBody(), refTest.getBodyDynamicVariables());
                    reqInfo.setBody(finalBody);
                } else {
                    reqInfo.setBody("{}");
                }
                rpt.setRequest(reqInfo);

                ResponseInfo respInfo = new ResponseInfo();
                if (s.contains("responseBody")) {
                    respInfo.setBodySample(s.getString("responseBody"));
                }
                if (s.contains("latencyMs")) {
                    respInfo.setLatencyMs(s.getLong("latencyMs"));
                }
                if (s.contains("sizeBytes")) {
                    respInfo.setSizeBytes((long) s.getInt("sizeBytes"));
                }
                java.util.Map<String, String> capturedHeaders = new java.util.HashMap<>();
                for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                    String key = "respHeader_" + headerName.replace("-", "_");
                    if (s.contains(key)) {
                        capturedHeaders.put(headerName, s.getString(key));
                    }
                }
                respInfo.setHeaders(capturedHeaders);
                respInfo.setStatus(200);
                rpt.setResponse(respInfo);
                
                // Process the check reports and fill in actual values
                java.util.List<CheckReport> finalChecks = new java.util.ArrayList<>();
                for (CheckReport cr : refBeforeChecks) {
                    String saveKey = cr.getActual(); // The key was stored in the actual field
                    String actualValue = "<NOT FOUND>";
                    if (s.contains(saveKey)) {
                        actualValue = convertToString(s.get(saveKey));
                        // Remove the key from session
                        s = s.remove(saveKey);
                    }
                    
                    cr.setActual(actualValue);
                    boolean passed = evaluateOperator(cr.getOperator(), actualValue, cr.getExpect());
                    cr.setPassed(passed);
                    finalChecks.add(cr);
                }
                
                rpt.setChecks(finalChecks);
                rpt.setPassed(true);
                rpt.setStatus("200");
                refCaseReport.getItems().add(rpt);

                // Save extracted values to TestRunContext
                for (ResponseCheck dcInner : dcList) {
                    String key = "diff_" + dcInner.hashCode() + "_PRE";
                    if (s.contains(key)) {
                        Object val = s.get(key);
                        TestRunContext.saveBefore(dcInner.getExpression(), String.valueOf(val));
                        s = s.remove(key);
                    }
                }
                
                // Clean up session
                s = s.remove("responseBody").remove("latencyMs").remove("sizeBytes");
                for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                    s = s.remove("respHeader_" + headerName.replace("-", "_"));
                }

                return s;
            });

            beforeChain = beforeChain.exec(refReq).exec(refReporting);
        }

        // =====================
        // Build PRE_CHECK reference request(s)
        // =====================
        // Group PRE_CHECK checks by reference TCID to avoid duplicate reference requests
        java.util.Map<String, java.util.List<ResponseCheck>> preCheckGroups = preChecks.stream()
                .filter(pc -> pc.getExpression() != null && pc.getExpression().contains("."))
                .collect(Collectors.groupingBy(
                        pc -> pc.getExpression().substring(0, pc.getExpression().indexOf('.')),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()));

        for (java.util.Map.Entry<String, java.util.List<ResponseCheck>> entry : preCheckGroups.entrySet()) {
            String refTcid = entry.getKey();
            java.util.List<ResponseCheck> pcList = entry.getValue();

            // Locate reference endpoint (try batch first, then DB)
            Endpoint refEndpoint = null;
            for (BatchItem bi : batchItems) {
                if (bi.test != null && refTcid.equals(bi.test.getTcid())) {
                    refEndpoint = bi.endpoint;
                    break;
                }
            }
            final GatlingTest refTest = findRefTestByTcid(refTcid);
            if (refTest != null) {
                enrichRefTestTemplates(refTest);
            }
            if (refTest != null) {
                refEndpoint = findEndpointForTest(refTest, endpoint);
            }
            if (refEndpoint == null) continue; // skip if endpoint not found

            String refName = test.getTcid() + "." + refTcid + "_PRE_CHECK";
            String refMethod = refEndpoint.getMethod() == null ? "GET" : refEndpoint.getMethod().toUpperCase();
            java.util.Map<String,String> dynVarsPre = (refTest != null && refTest.getEndpointDynamicVariables() != null)
                    ? refTest.getEndpointDynamicVariables()
                    : java.util.Collections.emptyMap();
            String refUrl = RuntimeTemplateProcessor.render(
                    TestRunContext.processVariableReferences(refEndpoint.getUrl()),
                    dynVarsPre);

            HttpRequestActionBuilder refReq;
            switch (refMethod) {
                case "POST" -> refReq = http(refName).post(refUrl);
                case "PUT" -> refReq = http(refName).put(refUrl);
                case "DELETE" -> refReq = http(refName).delete(refUrl);
                default -> refReq = http(refName).get(refUrl);
            }

            // Add headers and body from refTest (if available)
            if (refTest != null) {
                java.util.Map<String, String> headersMap = parseHeaders(refTest.getHeaders());
                for (java.util.Map.Entry<String, String> headerEntry : headersMap.entrySet()) {
                    refReq = refReq.header(headerEntry.getKey(), session ->
                            RuntimeTemplateProcessor.render(headerEntry.getValue(), refTest.getHeadersDynamicVariables()));
                }
                if (refTest.getBody() != null && !refTest.getBody().trim().isEmpty()) {
                    refReq = refReq.body(StringBody(session ->
                            RuntimeTemplateProcessor.render(refTest.getBody(), refTest.getBodyDynamicVariables())));
                }
            }

            // Add jsonPath checks for each PRE_CHECK in this group
            for (ResponseCheck pc : pcList) {
                String path = pc.getExpression().substring(pc.getExpression().indexOf('.') + 1);
                String saveAsKey = "pre_check_" + pc.hashCode();
                refReq = refReq.check(jsonPath(path).saveAs(saveAsKey));
            }

            // Add original checks from reference test
            final java.util.List<CheckReport> refPreChecks = new java.util.ArrayList<>();
            if (refTest != null && refTest.getResponseChecks() != null && !refTest.getResponseChecks().isBlank()) {
                try {
                    java.util.List<ResponseCheck> origChecks = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(refTest.getResponseChecks(),
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ResponseCheck>>() {});

                    for (ResponseCheck rc : origChecks) {
                        final String saveKey = "ref_pre_check_orig_" + rc.hashCode();
                        switch (rc.getType()) {
                            case STATUS:
                                refReq = refReq.check(status().saveAs(saveKey));
                                break;
                            case JSON_PATH:
                                refReq = refReq.check(jsonPath(rc.getExpression()).saveAs(saveKey));
                                break;
                            case XPATH:
                                refReq = refReq.check(xpath(rc.getExpression()).saveAs(saveKey));
                                break;
                            case REGEX:
                                refReq = refReq.check(regex(rc.getExpression()).saveAs(saveKey));
                                break;
                            default:
                                continue;
                        }

                        CheckReport cr = new CheckReport();
                        cr.setType(rc.getType());
                        cr.setExpression(rc.getExpression());
                        cr.setOperator(rc.getOperator());
                        cr.setExpect(rc.getExpect());
                        cr.setActual(saveKey); // Temporarily store the key
                        refPreChecks.add(cr);
                    }
                } catch (Exception ignored) {}
            }

            // Always capture status and basic metrics
            refReq = refReq.check(
                    status().is(200),
                    bodyString().saveAs("responseBody"),
                    responseTimeInMillis().saveAs("latencyMs"),
                    bodyLength().saveAs("sizeBytes")
            );
            for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                String sessionKey = "respHeader_" + headerName.replace("-", "_");
                refReq = refReq.check(header(headerName).optional().saveAs(sessionKey));
            }

            // Reporting & variable saving
            ChainBuilder refReporting = exec(s -> {
                String mainTcid = test.getTcid();
                String refRequestReportKey = mainTcid + "|" + refTcid + "|PRE_CHECK";

                // Create a new CaseReport for this reference request
                CaseReport refCaseReport = caseReports.computeIfAbsent(refRequestReportKey, k -> {
                    CaseReport cr = new CaseReport();
                    cr.setTcid(refTcid);
                    cr.setItems(Collections.synchronizedList(new ArrayList<>()));
                    return cr;
                });
                
                RequestReport rpt = new RequestReport();
                rpt.setRequestName(refName);

                RequestInfo reqInfo = new RequestInfo();
                reqInfo.setMethod(refMethod);
                reqInfo.setUrl(refUrl);
                java.util.Map<String, String> finalHeadersMap = new java.util.HashMap<>();
                if (refTest != null && refTest.getHeaders() != null && !refTest.getHeaders().isEmpty()) {
                    String finalHeaders = RuntimeTemplateProcessor.render(refTest.getHeaders(), refTest.getHeadersDynamicVariables());
                    finalHeadersMap = parseHeaders(finalHeaders);
                }
                reqInfo.setHeaders(finalHeadersMap);
                if (refTest != null && refTest.getBody() != null && !refTest.getBody().trim().isEmpty()) {
                    String finalBody = RuntimeTemplateProcessor.render(refTest.getBody(), refTest.getBodyDynamicVariables());
                    reqInfo.setBody(finalBody);
                } else {
                    reqInfo.setBody("{}");
                }
                rpt.setRequest(reqInfo);

                ResponseInfo respInfo = new ResponseInfo();
                if (s.contains("responseBody")) {
                    respInfo.setBodySample(s.getString("responseBody"));
                }
                if (s.contains("latencyMs")) {
                    respInfo.setLatencyMs(s.getLong("latencyMs"));
                }
                if (s.contains("sizeBytes")) {
                    respInfo.setSizeBytes((long) s.getInt("sizeBytes"));
                }
                java.util.Map<String, String> capturedHeaders = new java.util.HashMap<>();
                for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                    String key = "respHeader_" + headerName.replace("-", "_");
                    if (s.contains(key)) {
                        capturedHeaders.put(headerName, s.getString(key));
                    }
                }
                respInfo.setHeaders(capturedHeaders);
                respInfo.setStatus(200); // Default status
                rpt.setResponse(respInfo);

                // Process the check reports and fill in actual values
                java.util.List<CheckReport> finalChecks = new java.util.ArrayList<>();
                for (CheckReport cr : refPreChecks) {
                    String saveKey = cr.getActual(); // The key was stored in the actual field
                    String actualValue = "<NOT FOUND>";
                    if (s.contains(saveKey)) {
                        actualValue = convertToString(s.get(saveKey));
                        s = s.remove(saveKey);
                    }

                    cr.setActual(actualValue);
                    boolean passed = evaluateOperator(cr.getOperator(), actualValue, cr.getExpect());
                    cr.setPassed(passed);
                    finalChecks.add(cr);
                }
                rpt.setChecks(finalChecks);

                // Update overall passed status and response status from checks
                boolean allPassed = finalChecks.stream().allMatch(CheckReport::isPassed);
                rpt.setPassed(allPassed);
                finalChecks.stream()
                        .filter(r -> r.getType() == CheckType.STATUS && r.getActual() != null)
                        .findFirst()
                        .ifPresent(r -> {
                            try {
                                respInfo.setStatus(Integer.parseInt(r.getActual()));
                                rpt.setStatus(r.getActual());
                            } catch (NumberFormatException e) {
                                rpt.setStatus(String.valueOf(respInfo.getStatus()));
                            }
                        });
                if (rpt.getStatus() == null) rpt.setStatus(String.valueOf(respInfo.getStatus()));

                refCaseReport.getItems().add(rpt);

                // Save extracted values to TestRunContext for PRE_CHECK
                for (ResponseCheck pcInner : pcList) {
                    String key = "pre_check_" + pcInner.hashCode();
                    if (s.contains(key)) {
                        Object val = s.get(key);
                        String stringVal = String.valueOf(val);
                        TestRunContext.savePreCheck(pcInner.getExpression(), stringVal);
                        s = s.remove(key);
                    }
                }

                // Clean up session
                s = s.remove("responseBody").remove("latencyMs").remove("sizeBytes");
                for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                    s = s.remove("respHeader_" + headerName.replace("-", "_"));
                }

                return s;
            });

            beforeChain = beforeChain.exec(refReq).exec(refReporting);
        }

        // =====================
        // Build reference AFTER request(s) for DIFF checks
        // =====================
        ChainBuilder afterChain = exec(session -> session);

        java.util.Map<String, java.util.List<ResponseCheck>> diffGroupsAfter = diffGroupsBefore; // same grouping
        for (java.util.Map.Entry<String, java.util.List<ResponseCheck>> entry : diffGroupsAfter.entrySet()) {
            String refTcid = entry.getKey();
            java.util.List<ResponseCheck> dcList = entry.getValue();

            Endpoint refEndpoint = null;
            for (BatchItem bi : batchItems) {
                if (bi.test != null && refTcid.equals(bi.test.getTcid())) { refEndpoint = bi.endpoint; break; }
            }
            final GatlingTest refTestAfter = findRefTestByTcid(refTcid);
            if (refTestAfter != null) {
                enrichRefTestTemplates(refTestAfter);
            }
            if (refTestAfter != null) {
                refEndpoint = findEndpointForTest(refTestAfter, endpoint);
            }
            if (refEndpoint == null) continue;

            String refName = test.getTcid() + "." + refTcid + "_PST";
            String refMethod = refEndpoint.getMethod() == null ? "GET" : refEndpoint.getMethod().toUpperCase();
            java.util.Map<String,String> dynVarsAfter = (refTestAfter != null && refTestAfter.getEndpointDynamicVariables() != null)
                    ? refTestAfter.getEndpointDynamicVariables()
                    : java.util.Collections.emptyMap();
            String refUrl = RuntimeTemplateProcessor.render(
                    TestRunContext.processVariableReferences(refEndpoint.getUrl()),
                    dynVarsAfter);

            HttpRequestActionBuilder refReq;
            switch (refMethod) {
                case "POST" -> refReq = http(refName).post(refUrl);
                case "PUT" -> refReq = http(refName).put(refUrl);
                case "DELETE" -> refReq = http(refName).delete(refUrl);
                default -> refReq = http(refName).get(refUrl);
            }

            // Headers & body
            if (refTestAfter != null) {
                java.util.Map<String, String> headersMapAfter = parseHeaders(refTestAfter.getHeaders());
                for (java.util.Map.Entry<String, String> headerEntry : headersMapAfter.entrySet()) {
                    refReq = refReq.header(headerEntry.getKey(), session ->
                            RuntimeTemplateProcessor.render(headerEntry.getValue(), refTestAfter.getHeadersDynamicVariables()));
                }
                if (refTestAfter.getBody() != null && !refTestAfter.getBody().trim().isEmpty()) {
                    refReq = refReq.body(StringBody(session ->
                            RuntimeTemplateProcessor.render(refTestAfter.getBody(), refTestAfter.getBodyDynamicVariables())));
                }
            }

            // jsonPath checks for each diff check
            for (ResponseCheck dc : dcList) {
                String path = dc.getExpression().substring(dc.getExpression().indexOf('.') + 1);
                String saveAsKey = "diff_" + dc.hashCode() + "_PST";
                refReq = refReq.check(jsonPath(path).saveAs(saveAsKey));
            }

            // Add original checks from reference test
            final java.util.List<CheckReport> refAfterChecks = new java.util.ArrayList<>();
            if (refTestAfter != null && refTestAfter.getResponseChecks() != null && !refTestAfter.getResponseChecks().isBlank()) {
                try {
                    java.util.List<ResponseCheck> origChecks = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(refTestAfter.getResponseChecks(), 
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ResponseCheck>>() {});
                    
                    for (ResponseCheck rc : origChecks) {
                        final String saveKey = "ref_check_" + rc.hashCode();
                        switch (rc.getType()) {
                            case STATUS:
                                refReq = refReq.check(status().saveAs(saveKey));
                                break;
                            case JSON_PATH:
                                refReq = refReq.check(jsonPath(rc.getExpression()).saveAs(saveKey));
                                break;
                            case XPATH:
                                refReq = refReq.check(xpath(rc.getExpression()).saveAs(saveKey));
                                break;
                            case REGEX:
                                refReq = refReq.check(regex(rc.getExpression()).saveAs(saveKey));
                                break;
                            default:
                                continue;
                        }
                        
                        // Create a check report with all data except actual value (filled later)
                        CheckReport cr = new CheckReport();
                        cr.setType(rc.getType());
                        cr.setExpression(rc.getExpression());
                        cr.setOperator(rc.getOperator());
                        cr.setExpect(rc.getExpect());
                        // Store the session key with the report so we can get the actual value later
                        cr.setActual(saveKey); // Temporarily store the key here
                        refAfterChecks.add(cr);
                    }
                } catch (Exception ignored) {}
            }

            refReq = refReq.check(
                    status().is(200),
                    bodyString().saveAs("responseBody"),
                    responseTimeInMillis().saveAs("latencyMs"),
                    bodyLength().saveAs("sizeBytes")
            );
            for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                String sessionKey = "respHeader_" + headerName.replace("-", "_");
                refReq = refReq.check(header(headerName).optional().saveAs(sessionKey));
            }

            ChainBuilder refReporting = exec(s -> {
                String mainTcid = test.getTcid();
                String refRequestReportKey = mainTcid + "|" + refTcid + "|DIFF_PST";

                // Create a new CaseReport for this reference request
                CaseReport refCaseReport = caseReports.computeIfAbsent(refRequestReportKey, k -> {
                    CaseReport cr = new CaseReport();
                    cr.setTcid(refTcid);
                    cr.setItems(Collections.synchronizedList(new ArrayList<>()));
                    return cr;
                });

                RequestReport rpt = new RequestReport();
                rpt.setRequestName(refName);

                RequestInfo reqInfo = new RequestInfo();
                reqInfo.setMethod(refMethod);
                reqInfo.setUrl(refUrl);
                java.util.Map<String, String> finalHeadersMap = new java.util.HashMap<>();
                if (refTestAfter != null && refTestAfter.getHeaders() != null && !refTestAfter.getHeaders().isEmpty()) {
                    String finalHeaders = RuntimeTemplateProcessor.render(refTestAfter.getHeaders(), refTestAfter.getHeadersDynamicVariables());
                    finalHeadersMap = parseHeaders(finalHeaders);
                }
                reqInfo.setHeaders(finalHeadersMap);
                if (refTestAfter != null && refTestAfter.getBody() != null && !refTestAfter.getBody().trim().isEmpty()) {
                    String finalBody = RuntimeTemplateProcessor.render(refTestAfter.getBody(), refTestAfter.getBodyDynamicVariables());
                    reqInfo.setBody(finalBody);
                } else {
                    reqInfo.setBody("{}");
                }
                rpt.setRequest(reqInfo);

                ResponseInfo respInfo = new ResponseInfo();
                if (s.contains("responseBody")) {
                    respInfo.setBodySample(s.getString("responseBody"));
                }
                if (s.contains("latencyMs")) {
                    respInfo.setLatencyMs(s.getLong("latencyMs"));
                }
                if (s.contains("sizeBytes")) {
                    respInfo.setSizeBytes((long) s.getInt("sizeBytes"));
                }
                java.util.Map<String, String> capturedHeaders = new java.util.HashMap<>();
                for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                    String key = "respHeader_" + headerName.replace("-", "_");
                    if (s.contains(key)) {
                        capturedHeaders.put(headerName, s.getString(key));
                    }
                }
                respInfo.setHeaders(capturedHeaders);
                respInfo.setStatus(200);
                rpt.setResponse(respInfo);
                
                // Process the check reports and fill in actual values
                java.util.List<CheckReport> finalChecksAfter = new java.util.ArrayList<>();
                for (CheckReport cr : refAfterChecks) {
                    String saveKey = cr.getActual(); // The key was stored in the actual field
                    String actualValue = "<NOT FOUND>";
                    if (s.contains(saveKey)) {
                        actualValue = convertToString(s.get(saveKey));
                        // Remove the key from session
                        s = s.remove(saveKey);
                    }
                    
                    cr.setActual(actualValue);
                    boolean passed = evaluateOperator(cr.getOperator(), actualValue, cr.getExpect());
                    cr.setPassed(passed);
                    finalChecksAfter.add(cr);
                }
                
                rpt.setChecks(finalChecksAfter);
                rpt.setPassed(true);
                rpt.setStatus("200");
                refCaseReport.getItems().add(rpt);

                // Save extracted values
                for (ResponseCheck dcInner : dcList) {
                    String key = "diff_" + dcInner.hashCode() + "_PST";
                    if (s.contains(key)) {
                        Object val = s.get(key);
                        TestRunContext.saveAfter(dcInner.getExpression(), String.valueOf(val));
                        s = s.remove(key);
                    }
                }

                // Clean up session
                s = s.remove("responseBody").remove("latencyMs").remove("sizeBytes");
                for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                    s = s.remove("respHeader_" + headerName.replace("-", "_"));
                }

                return s;
            });

            afterChain = afterChain.exec(refReq).exec(refReporting);
        }

        // =====================
        // Build PST_CHECK reference request(s)
        // =====================
        // Group PST_CHECK checks by reference TCID to avoid duplicate reference requests
        java.util.Map<String, java.util.List<ResponseCheck>> pstCheckGroups = pstChecks.stream()
                .filter(pc -> pc.getExpression() != null && pc.getExpression().contains("."))
                .collect(Collectors.groupingBy(
                        pc -> pc.getExpression().substring(0, pc.getExpression().indexOf('.')),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()));

        for (java.util.Map.Entry<String, java.util.List<ResponseCheck>> entry : pstCheckGroups.entrySet()) {
            String refTcid = entry.getKey();
            java.util.List<ResponseCheck> pcList = entry.getValue();

            // Locate reference endpoint (try batch first, then DB)
            Endpoint refEndpoint = null;
            for (BatchItem bi : batchItems) {
                if (bi.test != null && refTcid.equals(bi.test.getTcid())) {
                    refEndpoint = bi.endpoint;
                    break;
                }
            }
            final GatlingTest refTest = findRefTestByTcid(refTcid);
            if (refTest != null) {
                enrichRefTestTemplates(refTest);
            }
            if (refTest != null) {
                refEndpoint = findEndpointForTest(refTest, endpoint);
            }
            if (refEndpoint == null) continue; // skip if endpoint not found

            String refName = test.getTcid() + "." + refTcid + "_PST_CHECK";
            String refMethod = refEndpoint.getMethod() == null ? "GET" : refEndpoint.getMethod().toUpperCase();
            java.util.Map<String,String> dynVarsPst = (refTest != null && refTest.getEndpointDynamicVariables() != null)
                    ? refTest.getEndpointDynamicVariables()
                    : java.util.Collections.emptyMap();
            String refUrl = RuntimeTemplateProcessor.render(
                    TestRunContext.processVariableReferences(refEndpoint.getUrl()),
                    dynVarsPst);

            HttpRequestActionBuilder refReq;
            switch (refMethod) {
                case "POST" -> refReq = http(refName).post(refUrl);
                case "PUT" -> refReq = http(refName).put(refUrl);
                case "DELETE" -> refReq = http(refName).delete(refUrl);
                default -> refReq = http(refName).get(refUrl);
            }

            // Add headers and body from refTest (if available)
            if (refTest != null) {
                java.util.Map<String, String> headersMap = parseHeaders(refTest.getHeaders());
                for (java.util.Map.Entry<String, String> headerEntry : headersMap.entrySet()) {
                    refReq = refReq.header(headerEntry.getKey(), session ->
                            RuntimeTemplateProcessor.render(headerEntry.getValue(), refTest.getHeadersDynamicVariables()));
                }
                if (refTest.getBody() != null && !refTest.getBody().trim().isEmpty()) {
                    refReq = refReq.body(StringBody(session ->
                            RuntimeTemplateProcessor.render(refTest.getBody(), refTest.getBodyDynamicVariables())));
                }
            }

            // Add jsonPath checks for each PST_CHECK in this group
            for (ResponseCheck pc : pcList) {
                String path = pc.getExpression().substring(pc.getExpression().indexOf('.') + 1);
                String saveAsKey = "pst_check_" + pc.hashCode();
                refReq = refReq.check(jsonPath(path).saveAs(saveAsKey));
            }

            // Add original checks from reference test
            final java.util.List<CheckReport> refPstChecks = new java.util.ArrayList<>();
            if (refTest != null && refTest.getResponseChecks() != null && !refTest.getResponseChecks().isBlank()) {
                try {
                    java.util.List<ResponseCheck> origChecks = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(refTest.getResponseChecks(),
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ResponseCheck>>() {});

                    for (ResponseCheck rc : origChecks) {
                        final String saveKey = "ref_pst_check_orig_" + rc.hashCode();
                        switch (rc.getType()) {
                            case STATUS:
                                refReq = refReq.check(status().saveAs(saveKey));
                                break;
                            case JSON_PATH:
                                refReq = refReq.check(jsonPath(rc.getExpression()).saveAs(saveKey));
                                break;
                            case XPATH:
                                refReq = refReq.check(xpath(rc.getExpression()).saveAs(saveKey));
                                break;
                            case REGEX:
                                refReq = refReq.check(regex(rc.getExpression()).saveAs(saveKey));
                                break;
                            default:
                                continue;
                        }

                        CheckReport cr = new CheckReport();
                        cr.setType(rc.getType());
                        cr.setExpression(rc.getExpression());
                        cr.setOperator(rc.getOperator());
                        cr.setExpect(rc.getExpect());
                        cr.setActual(saveKey); // Temporarily store the key
                        refPstChecks.add(cr);
                    }
                } catch (Exception ignored) {}
            }

            // Always capture status and basic metrics
            refReq = refReq.check(
                    status().is(200),
                    bodyString().saveAs("responseBody"),
                    responseTimeInMillis().saveAs("latencyMs"),
                    bodyLength().saveAs("sizeBytes")
            );
            for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                String sessionKey = "respHeader_" + headerName.replace("-", "_");
                refReq = refReq.check(header(headerName).optional().saveAs(sessionKey));
            }

            // Reporting & variable saving
            ChainBuilder refReporting = exec(s -> {
                String mainTcid = test.getTcid();
                String refRequestReportKey = mainTcid + "|" + refTcid + "|PST_CHECK";

                // Create a new CaseReport for this reference request
                CaseReport refCaseReport = caseReports.computeIfAbsent(refRequestReportKey, k -> {
                    CaseReport cr = new CaseReport();
                    cr.setTcid(refTcid);
                    cr.setItems(Collections.synchronizedList(new ArrayList<>()));
                    return cr;
                });

                RequestReport rpt = new RequestReport();
                rpt.setRequestName(refName);

                RequestInfo reqInfo = new RequestInfo();
                reqInfo.setMethod(refMethod);
                reqInfo.setUrl(refUrl);
                java.util.Map<String, String> finalHeadersMap = new java.util.HashMap<>();
                if (refTest != null && refTest.getHeaders() != null && !refTest.getHeaders().isEmpty()) {
                    String finalHeaders = RuntimeTemplateProcessor.render(refTest.getHeaders(), refTest.getHeadersDynamicVariables());
                    finalHeadersMap = parseHeaders(finalHeaders);
                }
                reqInfo.setHeaders(finalHeadersMap);
                if (refTest != null && refTest.getBody() != null && !refTest.getBody().trim().isEmpty()) {
                    String finalBody = RuntimeTemplateProcessor.render(refTest.getBody(), refTest.getBodyDynamicVariables());
                    reqInfo.setBody(finalBody);
                } else {
                    reqInfo.setBody("{}");
                }
                rpt.setRequest(reqInfo);

                ResponseInfo respInfo = new ResponseInfo();
                if (s.contains("responseBody")) {
                    respInfo.setBodySample(s.getString("responseBody"));
                }
                if (s.contains("latencyMs")) {
                    respInfo.setLatencyMs(s.getLong("latencyMs"));
                }
                if (s.contains("sizeBytes")) {
                    respInfo.setSizeBytes((long) s.getInt("sizeBytes"));
                }
                java.util.Map<String, String> capturedHeaders = new java.util.HashMap<>();
                for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                    String key = "respHeader_" + headerName.replace("-", "_");
                    if (s.contains(key)) {
                        capturedHeaders.put(headerName, s.getString(key));
                    }
                }
                respInfo.setHeaders(capturedHeaders);
                respInfo.setStatus(200); // Default status
                rpt.setResponse(respInfo);

                // Process the check reports and fill in actual values
                java.util.List<CheckReport> finalChecks = new java.util.ArrayList<>();
                for (CheckReport cr : refPstChecks) {
                    String saveKey = cr.getActual(); // The key was stored in the actual field
                    String actualValue = "<NOT FOUND>";
                    if (s.contains(saveKey)) {
                        actualValue = convertToString(s.get(saveKey));
                        s = s.remove(saveKey);
                    }

                    cr.setActual(actualValue);
                    boolean passed = evaluateOperator(cr.getOperator(), actualValue, cr.getExpect());
                    cr.setPassed(passed);
                    finalChecks.add(cr);
                }
                rpt.setChecks(finalChecks);

                // Update overall passed status and response status from checks
                boolean allPassed = finalChecks.stream().allMatch(CheckReport::isPassed);
                rpt.setPassed(allPassed);
                finalChecks.stream()
                        .filter(r -> r.getType() == CheckType.STATUS && r.getActual() != null)
                        .findFirst()
                        .ifPresent(r -> {
                            try {
                                respInfo.setStatus(Integer.parseInt(r.getActual()));
                                rpt.setStatus(r.getActual());
                            } catch (NumberFormatException e) {
                                rpt.setStatus(String.valueOf(respInfo.getStatus()));
                            }
                        });
                if (rpt.getStatus() == null) rpt.setStatus(String.valueOf(respInfo.getStatus()));

                refCaseReport.getItems().add(rpt);

                // Save extracted values to TestRunContext
                for (ResponseCheck pcInner : pcList) {
                    String key = "pst_check_" + pcInner.hashCode();
                    if (s.contains(key)) {
                        Object val = s.get(key);
                        String stringVal = String.valueOf(val);
                        TestRunContext.savePstCheck(pcInner.getExpression(), stringVal);
                        s = s.remove(key);
                    }
                }

                // Clean up session
                s = s.remove("responseBody").remove("latencyMs").remove("sizeBytes");
                for (String headerName : RESPONSE_HEADERS_TO_CAPTURE) {
                    s = s.remove("respHeader_" + headerName.replace("-", "_"));
                }

                return s;
            });

            afterChain = afterChain.exec(refReq).exec(refReporting);
        }

        // =====================
        // Evaluation chain for DIFF, PRE_CHECK and PST_CHECK
        // =====================
        ChainBuilder evalChain = exec(s -> {
            RequestReport mainRpt = null;
            List<RequestReport> items = caseReports.get(reportKey).getItems();
            for (RequestReport rr : items) {
                if (rr.getRequestName().equals(requestName)) {
                    mainRpt = rr; break;
                }
            }
            if (mainRpt == null) return s;

            if (mainRpt.getChecks() == null) {
                mainRpt.setChecks(new java.util.ArrayList<>());
            }

            // Process DIFF checks
            for (ResponseCheck dc : diffChecks) {
                String expr = dc.getExpression();
                Double diffVal = TestRunContext.calcDiff(expr);
                CheckReport cr = new CheckReport();
                cr.setType(CheckType.DIFF);
                cr.setExpression(expr);
                cr.setOperator(dc.getOperator());
                
                // Format expected value with +/- sign if it's numeric
                String expectValue = dc.getExpect();
                try {
                    // Parse as number regardless of existing sign
                    double expectNum = Double.parseDouble(expectValue.replaceFirst("^\\+", ""));
                    // Format with correct sign
                    expectValue = (expectNum >= 0 ? "+" : "-") + Math.abs(expectNum);
                } catch (NumberFormatException ignored) {
                    // Keep original if not numeric
                }
                cr.setExpect(expectValue);
                
                // Format actual value with +/- sign
                String actualValue = diffVal == null ? "N/A" : 
                    (diffVal >= 0 ? "+" : "") + String.valueOf(diffVal);
                cr.setActual(actualValue);
                
                boolean passed = false;
                if (diffVal != null) {
                    passed = evaluateOperator(dc.getOperator(), actualValue, expectValue);
                }
                cr.setPassed(passed);

                System.out.println(String.format("%s|%s|%s|expected:%s|actual:%s",
                        test.getTcid(), expr, dc.getOperator().toString(), expectValue, actualValue));

                mainRpt.getChecks().add(cr);
            }
            
            // Process PRE_CHECK checks
            for (ResponseCheck pc : preChecks) {
                String expr = pc.getExpression();
                String preValue = TestRunContext.getPreCheckValue(expr);
                CheckReport cr = new CheckReport();
                cr.setType(CheckType.PRE_CHECK);
                cr.setExpression(expr);
                cr.setOperator(pc.getOperator());
                cr.setExpect(pc.getExpect());
                
                String actualValue = preValue != null ? preValue : "N/A";
                cr.setActual(actualValue);
                
                boolean passed = false;
                if (preValue != null) {
                    passed = evaluateOperator(pc.getOperator(), actualValue, pc.getExpect());
                }
                cr.setPassed(passed);

                System.out.println(String.format("PRE_CHECK|%s|%s|%s|expected:%s|actual:%s",
                        test.getTcid(), expr, pc.getOperator().toString(), pc.getExpect(), actualValue));

                mainRpt.getChecks().add(cr);
            }
            
            // Process PST_CHECK checks
            for (ResponseCheck pc : pstChecks) {
                String expr = pc.getExpression();
                String pstValue = TestRunContext.getPstCheckValue(expr);
                CheckReport cr = new CheckReport();
                cr.setType(CheckType.PST_CHECK);
                cr.setExpression(expr);
                cr.setOperator(pc.getOperator());
                cr.setExpect(pc.getExpect());
                
                String actualValue = pstValue != null ? pstValue : "N/A";
                cr.setActual(actualValue);
                
                boolean passed = false;
                if (pstValue != null) {
                    passed = evaluateOperator(pc.getOperator(), actualValue, pc.getExpect());
                }
                cr.setPassed(passed);

                System.out.println(String.format("PST_CHECK|%s|%s|%s|expected:%s|actual:%s",
                        test.getTcid(), expr, pc.getOperator().toString(), pc.getExpect(), actualValue));

                mainRpt.getChecks().add(cr);
            }

            // Recompute pass status
            boolean allOk = mainRpt.getChecks().isEmpty() || mainRpt.getChecks().stream().allMatch(CheckReport::isPassed);
            mainRpt.setPassed(allOk);
            return s;
        });

        // Final combined chain
        return exec(beforeChain).exec(mainChain).exec(afterChain).exec(evalChain);
    }

    private Map<String, String> parseHeaders(String headersString) {
        if (headersString == null || headersString.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // Primary strategy: attempt to parse the entire string as JSON first
            Map<String, String> headers = new ObjectMapper().readValue(headersString, new TypeReference<Map<String, String>>() {});
            // Remove any leading/trailing single or double quotes around each value
            headers.replaceAll((k, v) -> v==null ? null: v.replaceAll("^([\"'])(.*)\\1$", "$2"));
            return headers;
        } catch (IOException e) {
            // Fallback to key: value line-by-line parsing
            Map<String, String> headers = new java.util.HashMap<>();
            String[] lines = headersString.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        // Strip surrounding quotes if present
                        headers.put(key, value.replaceAll("^([\"'])(.*)\\1$", "$2"));   
                    }
                }
            }
            return headers;
        }
    }

    @Override
    public void after() {
        try {
            // 输出测试变量信息
            System.out.println(VARIABLES_PREFIX + new ObjectMapper().writeValueAsString(TestRunContext.getAllVariables()));

            ObjectMapper mapper = new ObjectMapper();

            // Prepare writer for NDJSON streaming if file path is provided
            String reportFilePath = System.getProperty("gatling.report.file");
            java.io.BufferedWriter ndjsonWriter = null;
            if (reportFilePath != null && !reportFilePath.isBlank()) {
                try {
                    ndjsonWriter = new java.io.BufferedWriter(new java.io.FileWriter(reportFilePath, true));
                } catch (Exception e) {
                    System.err.println("[WARN] Unable to open report file for writing: " + e.getMessage());
                }
            }

            for (Map.Entry<String, CaseReport> entry : caseReports.entrySet()) {
                String reportKey = entry.getKey();
                CaseReport report = entry.getValue();

                String[] keyParts = reportKey.split("\\|");
                String originTcid;
                String reportTcid;
                String mode;

                if (keyParts.length == 2) { // Format: tcid|mode
                    reportTcid = keyParts[0];
                    mode = keyParts[1];

                    // For SETUP and TEARDOWN, use BatchItem.origin when available
                    if (mode.equals("SETUP") || mode.equals("TEARDOWN")) {
                        String mappedOrigin = batchItems.stream()
                                .filter(bi -> bi.test != null && reportTcid.equals(bi.test.getTcid()))
                                .map(bi -> (bi.origin != null && !bi.origin.isBlank()) ? bi.origin : bi.test.getTcid())
                                .findFirst().orElse(reportTcid);
                        originTcid = mappedOrigin;
                    } else {
                        // MAIN keeps itself as origin
                        originTcid = reportTcid;
                    }
                } else if (keyParts.length == 3) { // originTcid|reportTcid|mode
                    originTcid = keyParts[0];
                    reportTcid = keyParts[1];
                    mode = keyParts[2];
                } else {
                    continue; // malformed key
                }

                // Finalize the case report's overall status
                boolean allPassed = report.getItems().stream()
                        .allMatch(RequestReport::isPassed);
                report.setPassed(allPassed);

                    Map<String, Object> summaryEntry = new HashMap<>();
                summaryEntry.put("origin", originTcid);
                summaryEntry.put("tcid", reportTcid);
                summaryEntry.put("mode", mode);
                    summaryEntry.put("report", report);

                    // Stream this entry immediately to file if possible
                    if (ndjsonWriter != null) {
                        try {
                            ndjsonWriter.write(mapper.writeValueAsString(summaryEntry));
                            ndjsonWriter.newLine();
                            ndjsonWriter.flush();
                        } catch (Exception we) {
                            System.err.println("[WARN] Failed to write report entry: " + we.getMessage());
                        }
                    }
            }

            // Close writer if opened
            if (ndjsonWriter != null) {
                try { ndjsonWriter.close(); } catch (Exception ignored) {}
            }

        } catch (Exception ex) {
            System.err.println("Failed to output response check results: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Fallback lookup: use DAO to fetch GatlingTest & its endpoint when the reference TCID is not present in the batch.
     * Limits search to same project / environment as originEndpoint when possible.
     */
    private GatlingTest findRefTestByTcid(String tcid) {
        try {
            GatlingTestDaoImpl testDao = new GatlingTestDaoImpl();
            return testDao.getTestByTcid(tcid);
        } catch (Exception ex) {
            System.err.println("[WARN] Failed to lookup test for TCID " + tcid + ": " + ex.getMessage());
            return null;
        }
    }

    private Endpoint findEndpointForTest(GatlingTest refTest, Endpoint originEndpoint) {
        try {
            EndpointDaoImpl epDao = new EndpointDaoImpl();
            Endpoint ep = null;
            if (refTest.getEndpointName() != null) {
                Integer envId = com.qa.app.service.EnvironmentContext.getCurrentEnvironmentId();
                ep = epDao.getEndpointByNameAndEnv(refTest.getEndpointName(), envId);
            }

            // Ensure environment / project matches when both sides have a value
            if (ep != null && originEndpoint != null) {
                if (originEndpoint.getEnvironmentId() != null && ep.getEnvironmentId() != null
                        && !originEndpoint.getEnvironmentId().equals(ep.getEnvironmentId())) {
                    return null; // env mismatch
                }
                if (originEndpoint.getProjectId() != null && ep.getProjectId() != null
                        && !originEndpoint.getProjectId().equals(ep.getProjectId())) {
                    return null; // project mismatch
                }
            }
            return ep;
        } catch (Exception ex) {
            System.err.println("[WARN] Failed to lookup endpoint for test: " + ex.getMessage());
            return null;
        }
    }

    // ============= Helper to enrich templates for reference tests ==================
    private void enrichRefTestTemplates(GatlingTest t) {
        if (t == null) return;
        try {
            if ((t.getBody() == null || t.getBody().isEmpty()) && t.getBodyTemplateId() > 0) {
                BodyTemplate bt = new BodyTemplateServiceImpl().findBodyTemplateById(t.getBodyTemplateId());
                if (bt != null) t.setBody(bt.getContent());
            }
        } catch (Exception ignored) {}

        try {
            if ((t.getHeaders() == null || t.getHeaders().isEmpty()) && t.getHeadersTemplateId() > 0) {
                HeadersTemplate ht = new HeadersTemplateServiceImpl().getHeadersTemplateById(t.getHeadersTemplateId());
                if (ht != null) t.setHeaders(ht.getContent());
            }
        } catch (Exception ignored) {}
    }

    private static String convertToString(Object rawValue) {
        if (rawValue instanceof String) {
            return (String) rawValue;
        } else if (rawValue instanceof char[]) {
            return new String((char[]) rawValue);
        } else if (rawValue instanceof String[] arr) {
            return arr.length > 0 ? arr[0] : "";
        } else {
            return String.valueOf(rawValue);
        }
    }

    private static boolean evaluateOperator(Operator op, String actual, String expect) {
        return switch (op) {
            case CONTAINS -> actual.contains(expect);
            case MATCHES -> {
                try {
                    yield actual.matches(expect);
                } catch (Exception e) {
                    yield false;
                }
            }
            case IS -> actual.equals(expect);
            default -> false;
        };
    }
} 