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
import com.qa.app.model.ResponseCheck;
import com.qa.app.model.CheckType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static io.gatling.javaapi.http.HttpDsl.status;

    /**
     * Dynamically generate Simulation for multiple scenarios and thread groups, for concurrent execution of multiple Scenarios in one process.
     * System property: -Dgatling.multiscenario.file=/path/to/json
     */
public class GatlingScenarioSimulation extends Simulation {

    private static final String VARIABLES_PREFIX = "TEST_VARIABLES:";

    private static class ScenarioRunItem {
        public com.qa.app.model.Scenario scenario;
        public GatlingLoadParameters params;
        public List<Map<String, Object>> items; // each map contains "test" and "endpoint"
    }

    private final List<ScenarioRunItem> runItems;

    public GatlingScenarioSimulation() {
        // Clear test run context at the beginning of a simulation run
        TestRunContext.clear();
        
        try {
            String filePath = System.getProperty("gatling.multiscenario.file");
            if (filePath == null || filePath.isBlank()) {
                throw new IllegalStateException("gatling.multiscenario.file system property not set");
            }
            ObjectMapper om = new ObjectMapper();
            this.runItems = om.readValue(new File(filePath), new TypeReference<List<ScenarioRunItem>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read multi-scenario json", e);
        }

        if (runItems == null || runItems.isEmpty()) {
            throw new RuntimeException("No scenarios found in JSON");
        }

        // Remove scenarios that have no step items to prevent runtime errors
        runItems.removeIf(item -> item.items == null || item.items.isEmpty());
        if (runItems.isEmpty()) {
            throw new RuntimeException("Selected scenarios contain no steps to execute");
        }

        List<PopulationBuilder> popBuilders = new ArrayList<>();
        long maxDurationSec = 0;

        for (ScenarioRunItem item : runItems) {
            ScenarioBuilder scnBuilder = buildScenario(item);
            PopulationBuilder pb = buildInjection(item.params, scnBuilder);
            popBuilders.add(pb);
            maxDurationSec = Math.max(maxDurationSec, estimateMaxDuration(item.params));
        }

        // ---- Resolve first endpoint for http protocol in a safe way ----
        Endpoint firstEp = null;
        com.fasterxml.jackson.databind.ObjectMapper omConv = new com.fasterxml.jackson.databind.ObjectMapper();
        outer:
        for (ScenarioRunItem ri : runItems) {
            if (ri.items == null) continue;
            for (Map<String, Object> map : ri.items) {
                Object epObj = map.get("endpoint");
                if (epObj != null) {
                    firstEp = (epObj instanceof Endpoint) ? (Endpoint) epObj : omConv.convertValue(epObj, Endpoint.class);
                    if (firstEp != null && firstEp.getUrl() != null && !firstEp.getUrl().isBlank()) {
                        break outer;
                    }
                }
            }
        }
        if (firstEp == null) {
            throw new RuntimeException("Unable to determine baseUrl – no endpoint found in scenario steps");
        }
        // The baseUrl should be static and not contain dynamic placeholders.
        // We will render the full URL for each request instead.
        HttpProtocolBuilder httpProtocol = http.baseUrl("/");

        SetUp s = setUp(popBuilders.toArray(new PopulationBuilder[0])).protocols(httpProtocol);
        if (maxDurationSec > 0) {
            s.maxDuration(Duration.ofSeconds(maxDurationSec));
        }
    }

    private ScenarioBuilder buildScenario(ScenarioRunItem sri) {
        ChainBuilder chain = null;
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        
        for (Map<String, Object> m : sri.items) {
            Object to = m.get("test");
            GatlingTest test = (to instanceof GatlingTest) ? (GatlingTest) to : om.convertValue(to, GatlingTest.class);
            Object eo = m.get("endpoint");
            Endpoint ep = (eo instanceof Endpoint) ? (Endpoint) eo : om.convertValue(eo, Endpoint.class);
            HttpRequestActionBuilder req = buildRequest(test, ep);
            
            // Create a chain that executes the request and then processes any saved variables
            final String tcid = test.getTcid();
            ChainBuilder requestChain = exec(req).exec(session -> {
                // Check for variables that need to be saved to TestRunContext
                try {
                    String json = test.getResponseChecks();
                    if (json != null && !json.isBlank()) {
                        List<ResponseCheck> checks = new ObjectMapper().readValue(json, new TypeReference<List<ResponseCheck>>() {});
                        for (ResponseCheck rc : checks) {
                            if (rc.getSaveAs() != null && !rc.getSaveAs().isBlank()) {
                                String saveAsKey = rc.getSaveAs();
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
                                    
                                    // Save to TestRunContext
                                    TestRunContext.saveVariable(tcid, saveAsKey, actualValue);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
                return session;
            });
            
            if (chain == null) {
                chain = requestChain;
            } else {
                chain = chain.exec(requestChain);
            }
            
            if (test.getWaitTime() > 0) {
                chain = chain.pause(Duration.ofSeconds(test.getWaitTime()));
            }
        }
        if (chain == null) chain = exec(pause(1));

        // determine loop strategy based on thread group type
        StandardThreadGroup stdCfg = null;
        if (sri.params.getType() == ThreadGroupType.STANDARD) {
            stdCfg = sri.params.getStandardThreadGroup();
        }

        ScenarioBuilder base = scenario(Optional.ofNullable(sri.scenario.getName()).orElse("Scenario" + sri.scenario.getId()));

        // for time-based thread groups (Scheduler/Stepping/Ultimate), use forever() to continuously send requests until maxDuration
        if ((stdCfg != null && stdCfg.isScheduler())
                || sri.params.getType() == ThreadGroupType.STEPPING
                || sri.params.getType() == ThreadGroupType.ULTIMATE) {
            return base.forever().on(chain);
        }

        // for Standard thread group with specified loop count
        if (stdCfg != null && !stdCfg.isScheduler() && stdCfg.getLoops() != -1) {
            return base.exec(repeat(stdCfg.getLoops()).on(chain));
        }

        // default: each virtual user only executes once
        return base.exec(chain);
    }

    private HttpRequestActionBuilder buildRequest(GatlingTest test, Endpoint ep) {
        String reqName = test.getTcid();
        String method = ep.getMethod() == null ? "GET" : ep.getMethod().toUpperCase();
        
        // First replace any ${VAR} placeholders from previous test results
        String processedUrl = TestRunContext.processVariableReferences(ep.getUrl());
        // Then render user-defined @{var} placeholders using the test's dynamic variables
        processedUrl = RuntimeTemplateProcessor.render(processedUrl, test.getEndpointDynamicVariables());
        
        HttpRequestActionBuilder req;
        switch(method){
            case "POST" -> {
                req = http(reqName).post(processedUrl);
                if (test.getBody()!=null && !test.getBody().isBlank()) {
                    req = req.body(StringBody(session -> RuntimeTemplateProcessor.render(test.getBody(), test.getBodyDynamicVariables())));
                }
            }
            case "PUT" -> {
                req = http(reqName).put(processedUrl);
                if (test.getBody()!=null && !test.getBody().isBlank()) {
                    req = req.body(StringBody(session -> RuntimeTemplateProcessor.render(test.getBody(), test.getBodyDynamicVariables())));
                }
            }
            case "DELETE" -> req = http(reqName).delete(processedUrl);
            default -> req = http(reqName).get(processedUrl);
        }

        // headers
        Map<String,String> headers = parseHeaders(test.getHeaders());
        for (Map.Entry<String,String> entry:headers.entrySet()) {
            req = req.header(entry.getKey(), session -> RuntimeTemplateProcessor.render(entry.getValue(), test.getHeadersDynamicVariables()));
        }

        // Add response checks and variable extraction
        List<CheckBuilder> checkBuilders = new ArrayList<>();
        
        try {
            String json = test.getResponseChecks();
            if(json!=null && !json.isBlank()){
                List<ResponseCheck> list = new ObjectMapper().readValue(json, new TypeReference<List<ResponseCheck>>(){});
                int expected = 200;
                
                for(ResponseCheck rc: list){
                    if(rc.getType() == CheckType.STATUS){
                        expected = Integer.parseInt(rc.getExpect());
                    }
                    
                    // Add extractors for variables to be saved
                    if (rc.getSaveAs() != null && !rc.getSaveAs().isBlank()) {
                        String saveAsKey = rc.getSaveAs();
                        
                        // Create the extractor based on check type
                        switch(rc.getType()) {
                            case JSON_PATH:
                                checkBuilders.add(jsonPath(rc.getExpression()).saveAs(saveAsKey));
                                break;
                            case XPATH:
                                checkBuilders.add(xpath(rc.getExpression()).saveAs(saveAsKey));
                                break;
                            case REGEX:
                                checkBuilders.add(regex(rc.getExpression()).saveAs(saveAsKey));
                                break;
                            default:
                                // Status check is handled separately
                                break;
                        }
                    }
                }
                
                // Add status check
                checkBuilders.add(status().is(expected));
            } else {
                // Default status check
                checkBuilders.add(status().is(200));
            }
        } catch(Exception e) {
            // Default status check on error
            checkBuilders.add(status().is(200));
        }
        
        // Apply all checks
        if (!checkBuilders.isEmpty()) {
            req = req.check(checkBuilders.toArray(new CheckBuilder[0]));
        }

        return req;
    }

    private Map<String,String> parseHeaders(String str){
        if(str==null||str.isBlank()) return Collections.emptyMap();
        try { return new ObjectMapper().readValue(str, new TypeReference<Map<String,String>>(){});}catch(Exception e){/*ignore*/}
        Map<String,String> map=new HashMap<>();
        for(String line:str.split("\\r?\\n")){
            if(line.contains(":")){
                String[] p=line.split(":",2);
                map.put(p[0].trim(),p[1].trim());
            }
        }
        return map;
    }

    private PopulationBuilder buildInjection(GatlingLoadParameters p, ScenarioBuilder scn){
        switch(p.getType()){
            case STEPPING -> {
                SteppingThreadGroup st=p.getSteppingThreadGroup();
                List<OpenInjectionStep> steps=new ArrayList<>();
                if(st.getInitialDelay()>0) steps.add(nothingFor(Duration.ofSeconds(st.getInitialDelay())));
                steps.add(rampUsers(st.getStartUsers()).during(Duration.ofSeconds(Math.max(1,st.getIncrementTime()))));
                int remaining=st.getNumThreads()-st.getStartUsers();
                if(remaining>0 && st.getIncrementUsers()>0){
                    int batches=(int)Math.ceil((double)remaining/st.getIncrementUsers());
                    for(int i=0;i<batches;i++){
                        int u=Math.min(st.getIncrementUsers(), remaining - i*st.getIncrementUsers());
                        steps.add(rampUsers(u).during(Duration.ofSeconds(Math.max(1,st.getIncrementTime()))));
                    }
                }
                return scn.injectOpen(steps.toArray(new OpenInjectionStep[0]));
            }
            case ULTIMATE -> {
                UltimateThreadGroup ut=p.getUltimateThreadGroup();
                List<UltimateThreadGroupStep> sorted=new ArrayList<>(ut.getSteps());
                sorted.sort(Comparator.comparingInt(UltimateThreadGroupStep::getStartTime));
                List<OpenInjectionStep> inj=new ArrayList<>();
                long lastEnd=0;
                for(UltimateThreadGroupStep step:sorted){
                    long delay=step.getStartTime()-lastEnd; if(delay>0) inj.add(nothingFor(Duration.ofSeconds(delay)));
                    inj.add(rampUsers(step.getInitialLoad()).during(Duration.ofSeconds(step.getStartupTime())));
                    lastEnd=step.getStartTime()+step.getStartupTime();
                }
                return scn.injectOpen(inj.toArray(new OpenInjectionStep[0]));
            }
            case STANDARD -> {
                StandardThreadGroup std=p.getStandardThreadGroup();
                if(std.isScheduler()){
                    return scn.injectOpen(
                            nothingFor(Duration.ofSeconds(std.getDelay())),
                            rampUsers(std.getNumThreads()).during(Duration.ofSeconds(std.getRampUp()))
                    );
                } else {
                    return scn.injectOpen(rampUsers(std.getNumThreads()).during(Duration.ofSeconds(std.getRampUp())));
                }
            }
        }
        return scn.injectOpen(atOnceUsers(1));
    }

    private long estimateMaxDuration(GatlingLoadParameters p){
        switch(p.getType()){
            case STANDARD -> {
                StandardThreadGroup std=p.getStandardThreadGroup();
                if(std.isScheduler()) return std.getDelay()+std.getDuration();
                else return std.getRampUp();
            }
            case STEPPING -> {
                SteppingThreadGroup st=p.getSteppingThreadGroup();
                long total=st.getInitialDelay()+st.getHoldLoad();
                int remaining=st.getNumThreads()-st.getStartUsers();
                int batches= st.getIncrementUsers()>0? (int)Math.ceil((double)remaining/st.getIncrementUsers()) : 0;
                total+= (1L+batches)*st.getIncrementTime();
                return total;
            }
            case ULTIMATE -> {
                long max=0;
                for(UltimateThreadGroupStep step:p.getUltimateThreadGroup().getSteps()){
                    long end= step.getStartTime()+step.getStartupTime()+step.getHoldTime()+step.getShutdownTime();
                    if(end>max) max=end;
                }
                return max;
            }
            default -> {return 0;}
        }
    }
    
    @Override
    public void after() {
        try {
            // Output test variables information
            System.out.println(VARIABLES_PREFIX + new ObjectMapper().writeValueAsString(TestRunContext.getAllVariables()));
        } catch (Exception ex) {
            System.err.println("Failed to output test variables: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
} 