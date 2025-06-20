package com.qa.app.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.GatlingTest;
import com.qa.app.model.threadgroups.*;
import com.qa.app.util.RuntimeTemplateProcessor;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class DynamicJavaSimulation extends Simulation {

    private final GatlingLoadParameters params;
    private final List<BatchItem> batchItems;
    private final boolean isBatchMode;

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

            // 根据 JMeter Stepping TG 语义：initialDelay + 总 rampUp 时长 + holdLoad
            long totalRampUp = 0;

            // 初始用户 ramp 时间（只有当 startUsers > 0 时才计）
            if (steppingConfig.getStartUsers() > 0) {
                totalRampUp += steppingConfig.getIncrementTime();
            }

            // 计算后续增量批次数量
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

                // 3. Subsequent increments – 每个批次用户在线性 ramp-up 过程中被逐步拉起
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
        StandardThreadGroup standardConfig = null;
        if (params.getType() == ThreadGroupType.STANDARD) {
            standardConfig = params.getStandardThreadGroup();
        }

        // For time-based scenarios (Standard with scheduler, Stepping, Ultimate), loop forever until maxDuration
        if ((standardConfig != null && standardConfig.isScheduler()) 
            || params.getType() == ThreadGroupType.STEPPING
            || params.getType() == ThreadGroupType.ULTIMATE) {
            return scenario("Dynamic Test Scenario")
                    .forever().on(
                    chain
            );
        }

        // If standard config is not using scheduler and has a loop count, apply it.
        if (standardConfig != null && !standardConfig.isScheduler() && standardConfig.getLoops() != -1) {
            return scenario("Dynamic Test Scenario")
                    .exec(
                repeat(standardConfig.getLoops()).on(
                    chain
                )
            );
        }

        return scenario("Dynamic Test Scenario")
                .exec(chain);
    }

    private ChainBuilder createHttpChain() {
        ChainBuilder chain = null;
        for (int i = 0; i < batchItems.size(); i++) {
            BatchItem item = batchItems.get(i);
            HttpRequestActionBuilder requestBuilder = createRequestBuilder(item);

            if (chain == null) {
                chain = exec(requestBuilder);
            } else {
                chain = chain.exec(requestBuilder);
            }

            if (item.test.getWaitTime() > 0) {
                System.out.println(String.format("   -> Pause %ds after %s", item.test.getWaitTime(), item.test.getTcid()));
                chain = chain.pause(Duration.ofSeconds(item.test.getWaitTime()));
            }
        }
        return chain;
    }

    private HttpRequestActionBuilder createRequestBuilder(BatchItem item) {
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

        // ---------------- 处理 Headers ----------------
        // 先解析模板以获取所有 Header Key，随后为每一个 Key 设置一个动态 Expression
        java.util.Map<String, String> rawHeaderMap = parseHeaders(test.getHeaders());
        for (java.util.Map.Entry<String, String> entry : rawHeaderMap.entrySet()) {
            String headerKey = entry.getKey();
            String headerValueTemplate = entry.getValue();

            request = request.header(headerKey, session ->
                    RuntimeTemplateProcessor.render(headerValueTemplate, test.getHeadersDynamicVariables()));
        }

        return request.check(status().is(Integer.parseInt(test.getExpStatus())));
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
} 