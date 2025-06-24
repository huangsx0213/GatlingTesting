package com.qa.app.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.GatlingTest;
import com.qa.app.model.threadgroups.*;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import io.gatling.javaapi.core.CheckBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import com.qa.app.model.ResponseCheck;

public class DynamicJavaSimulation extends Simulation {

    private final GatlingLoadParameters params;
    private final List<BatchItem> batchItems;
    private final boolean isBatchMode;
    private final Map<String, List<ResponseCheck>> checkResults = new ConcurrentHashMap<>();

    private static class BatchItem {
        public GatlingTest test;
        public Endpoint endpoint;
    }

    {
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
                
                return scn.injectOpen(steps);


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
                return scn.injectOpen(injectionSteps);


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

        // For time-based scenarios (Standard with scheduler, Stepping, Ultimate), loop forever until maxDuration
        if ((standardConfig != null && standardConfig.isScheduler())
                || params.getType() == ThreadGroupType.STEPPING
                || params.getType() == ThreadGroupType.ULTIMATE) {
            return scenario(scenarioName)
                    .forever().on(chain);
        }

        // If standard config is not using scheduler and has a loop count, apply it.
        if (standardConfig != null && !standardConfig.isScheduler() && standardConfig.getLoops() != -1) {
            return scenario(scenarioName)
                    .exec(repeat(standardConfig.getLoops()).on(chain));
        }

        return scenario(scenarioName).exec(chain);
    }

    private ChainBuilder createHttpChain() {
        ChainBuilder chain = null;
        for (int i = 0; i < batchItems.size(); i++) {
            BatchItem item = batchItems.get(i);
            ChainBuilder requestChain = createRequestBuilder(item);

            if (chain == null) {
                chain = requestChain;
            } else {
                chain = chain.exec(requestChain);
            }

            if (item.test.getWaitTime() > 0) {
                System.out.println(String.format("   -> Pause %ds after %s", item.test.getWaitTime(), item.test.getTcid()));
                chain = chain.pause(Duration.ofSeconds(item.test.getWaitTime()));
            }
        }
        return chain;
    }

    private ChainBuilder createRequestBuilder(BatchItem item) {
        GatlingTest test = item.test;
        Endpoint endpoint = item.endpoint;
        String requestName = test.getTcid();

        HttpRequestActionBuilder request;

        String method = endpoint.getMethod() == null ? "GET" : endpoint.getMethod().toUpperCase();
        String bodyTemplate = test.getBody();

        switch (method) {
            case "POST":
                if (bodyTemplate == null || bodyTemplate.isEmpty()) {
                    request = http(requestName).post(endpoint.getUrl());
                } else {
                    request = http(requestName).post(endpoint.getUrl()).body(StringBody(session ->
                            RuntimeTemplateProcessor.render(bodyTemplate, test.getBodyDynamicVariables())));
                }
                break;
            case "PUT":
                if (bodyTemplate == null || bodyTemplate.isEmpty()) {
                    request = http(requestName).put(endpoint.getUrl());
                } else {
                    request = http(requestName).put(endpoint.getUrl()).body(StringBody(session ->
                            RuntimeTemplateProcessor.render(bodyTemplate, test.getBodyDynamicVariables())));
                }
                break;
            case "DELETE":
                request = http(requestName).delete(endpoint.getUrl());
                break;
            case "GET":
            default:
                request = http(requestName).get(endpoint.getUrl());
                break;
        }

        // ---------------- Handle Headers ----------------
        // First parse the template to get all Header Keys, then set a dynamic Expression for each Key
        java.util.Map<String, String> rawHeaderMap = parseHeaders(test.getHeaders());
        for (java.util.Map.Entry<String, String> entry : rawHeaderMap.entrySet()) {
            String headerKey = entry.getKey();
            String headerValueTemplate = entry.getValue();

            request = request.header(headerKey, session ->
                    RuntimeTemplateProcessor.render(headerValueTemplate, test.getHeadersDynamicVariables()));
        }

        // 1. Prepare lists for checks and for the logging actions that follow the request
        java.util.List<CheckBuilder> checkBuilders = new java.util.ArrayList<>();
        java.util.List<ChainBuilder> loggingActions = new java.util.ArrayList<>();
        boolean statusCheckExists = false;

        String checksJson = test.getResponseChecks();
        if (checksJson != null && !checksJson.isBlank()) {
            try {
                java.util.List<ResponseCheck> rcList = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(checksJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ResponseCheck>>() {});
                
                // Store the parsed list in the results map to be updated during the simulation
                checkResults.put(test.getTcid(), rcList);

                for (ResponseCheck rc : rcList) {
                    if (rc == null || rc.getType() == null) continue;

                    // This check object will be captured by the lambda
                    final ResponseCheck currentCheck = rc;

                    switch (currentCheck.getType()) {
                        case STATUS -> {
                            // For STATUS, we will do the check and save inside a logging action,
                            // but first we need to save the actual status code.
                            String statusSaveKey = "status_code_" + currentCheck.hashCode();
                            checkBuilders.add(status().saveAs(statusSaveKey));
                            statusCheckExists = true; // Mark that we are handling status

                            loggingActions.add(exec(session -> {
                                try {
                                    Object statusObj = session.get(statusSaveKey);
                                    String actualStatus;
                                    if (statusObj instanceof Integer) {
                                        actualStatus = String.valueOf(statusObj);
                                    } else if (statusObj instanceof char[]) {
                                        actualStatus = new String((char[]) statusObj);
                                    } else if (statusObj != null) {
                                        actualStatus = statusObj.toString();
                                    } else {
                                        actualStatus = "<VALUE NOT FOUND>";
                                    }
                                    
                                    currentCheck.setActual(actualStatus);

                                    // 根据操作符选择不同的比较方式
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

                                    // Perform the actual check here. It's now non-blocking for other checks.
                                    if (!checkPassed) {
                                        System.out.printf("\u001B[31mCHECK_FAIL|%s|%s|%s|%s|%s|%s\u001B[0m%n",
                                                test.getTcid(), 
                                                currentCheck.getType(),
                                                currentCheck.getExpression(), 
                                                currentCheck.getOperator(),
                                                currentCheck.getExpect(), 
                                                actualStatus);
                                        // IMPORTANT: We need to manually fail the session if the primary check fails
                                        return session.markAsFailed();
                                    }
                                    return session.remove(statusSaveKey);
                                } catch (Exception e) {
                                    System.err.println("[ERROR] Exception in STATUS check: " + e.getMessage());
                                    e.printStackTrace();
                                    currentCheck.setActual("ERROR: " + e.getMessage());
                                    return session;
                                }
                            }));
                        }
                        case JSON_PATH, XPATH, REGEX -> {
                            String saveAsKey = (currentCheck.getSaveAs() != null && !currentCheck.getSaveAs().isBlank())
                                ? currentCheck.getSaveAs() : "temp_check_var_" + currentCheck.hashCode();

                            CheckBuilder extractor = switch (currentCheck.getType()) {
                                case JSON_PATH -> jsonPath(currentCheck.getExpression()).saveAs(saveAsKey);
                                case XPATH -> xpath(currentCheck.getExpression()).saveAs(saveAsKey);
                                case REGEX -> regex(currentCheck.getExpression()).saveAs(saveAsKey);
                                default -> throw new IllegalStateException("Unexpected value: " + currentCheck.getType());
                            };
                            checkBuilders.add(extractor);

                            // Create the logging action and add it to a list to be processed later.
                            loggingActions.add(exec(session -> {
                                if (session.contains(saveAsKey)) {
                                    Object rawValue = session.get(saveAsKey);
                                    String actualValue;
                                    if (rawValue instanceof String) {
                                        actualValue = (String) rawValue;
                                    } else if (rawValue instanceof char[]) {
                                        actualValue = new String((char[]) rawValue);
                                    } else if (rawValue instanceof String[] arr) {
                                        actualValue = arr.length > 0 ? arr[0] : "";
                                    } else {
                                        actualValue = String.valueOf(rawValue);
                                    }
                                    
                                    // IMPORTANT: Set the actual value on the check object
                                    currentCheck.setActual(actualValue);

                                    // 根据操作符选择不同的比较方式
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

                                    if (!checkPassed) {
                                        System.out.printf("\u001B[31mCHECK_FAIL|%s|%s|%s|%s|%s|%s\u001B[0m%n",
                                                test.getTcid(), 
                                                currentCheck.getType(),
                                                currentCheck.getExpression(), 
                                                currentCheck.getOperator(),
                                                currentCheck.getExpect(), 
                                                actualValue);
                                    }
                                } else {
                                    // IMPORTANT: Set actual value when not found
                                    currentCheck.setActual("<VALUE NOT FOUND>");
                                    System.out.printf("\u001B[31mCHECK_FAIL|%s|%s|%s|%s|%s|%s\u001B[0m%n",
                                            test.getTcid(),
                                            currentCheck.getType(),
                                            currentCheck.getExpression(),
                                            currentCheck.getOperator(),
                                            currentCheck.getExpect(),
                                            "<VALUE NOT FOUND>");
                                }
                                if (saveAsKey.startsWith("temp_check_var_")) {
                                    return session.remove(saveAsKey);
                                }
                                return session;
                            }));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to parse responseChecks: " + e.getMessage());
            }
        }

        // 2. Apply all checks to the request builder *before* it's used.
        // The check() method returns a new builder instance, so we must reassign it.
        if (!checkBuilders.isEmpty()) {
            request = request.check(checkBuilders.toArray(new CheckBuilder[0]));
        }

        // IMPORTANT: If no status check was defined by the user, we must add a default
        // blocking check to ensure the request fails on non-200 responses.
        // This one does not save the actual value, it's just for fail-fast behavior.
        if (!statusCheckExists) {
            request = request.check(status().is(200));
        }

        // 3. Start the chain with the fully configured request.
        ChainBuilder finalChain = exec(request);

        // 4. Append all the logging actions to the chain.
        for (ChainBuilder loggingAction : loggingActions) {
            finalChain = finalChain.exec(loggingAction);
        }

        return finalChain;
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
            String outPath = System.getProperty("gatling.result.file", "response_checks_result.json");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            if (isBatchMode) {
                // In batch mode, we create a list of objects, each containing a tcid and its checks
                java.util.List<java.util.Map<String, Object>> summary = new java.util.ArrayList<>();
                for (java.util.Map.Entry<String, List<ResponseCheck>> entry : checkResults.entrySet()) {
                    java.util.Map<String, Object> resultEntry = new java.util.HashMap<>();
                    resultEntry.put("tcid", entry.getKey());
                    resultEntry.put("responseChecks", entry.getValue());
                    summary.add(resultEntry);
                }
                mapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(outPath), summary);
            } else {
                // For single mode, wrap the single test result into the same structure used in batch mode
                if (!checkResults.isEmpty()) {
                    java.util.Map<String, Object> entry = new java.util.HashMap<>();
                    String tcid = checkResults.keySet().iterator().next();
                    
                    entry.put("tcid", tcid);
                    entry.put("responseChecks", checkResults.get(tcid));
                    java.util.List<java.util.Map<String, Object>> summary = java.util.List.of(entry);
                    mapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(outPath), summary);
                } else {
                    System.err.println("No check results found to write.");
                }
            }
            System.out.println("[INFO] Response check results written to " + outPath);
        } catch (Exception ex) {
            System.err.println("Failed to write response check results: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
} 