package com.qa.app.service.runner;

import com.qa.app.dao.api.IGatlingScenarioDao;
import com.qa.app.dao.impl.GatlingScenarioDaoImpl;
import com.qa.app.model.*;
import com.qa.app.model.threadgroups.StandardThreadGroup;
import com.qa.app.model.threadgroups.ThreadGroupType;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IEndpointService;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.impl.EndpointServiceImpl;
import com.qa.app.service.impl.GatlingTestServiceImpl;
import com.qa.app.service.EnvironmentContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Execute a Scenario in “functional test” mode: single virtual user, single loop, sequential steps.
 * Re-uses existing GatlingTestRunner infrastructure to obtain JSON/NDJSON reports.
 */
public class FunctionalScenarioRunner {

    private FunctionalScenarioRunner() { /* util class */ }

    public static void run(Scenario scenario, Runnable onComplete) throws ServiceException {
        if (scenario == null) throw new IllegalArgumentException("scenario is null");

        IGatlingScenarioDao scenarioDao = new GatlingScenarioDaoImpl();
        IGatlingTestService testService = new GatlingTestServiceImpl();
        IEndpointService endpointService = new EndpointServiceImpl();

        List<GatlingTest> tests = new ArrayList<>();
        List<Endpoint> endpoints = new ArrayList<>();
        List<String> origins = new ArrayList<>();
        List<String> modes = new ArrayList<>();

        // 1. Resolve steps → tests / endpoints
        List<ScenarioStep> steps;
        try {
            steps = scenarioDao.getStepsByScenarioId(scenario.getId());
        } catch (Exception e) {
            throw new ServiceException("Failed to load scenario steps", e);
        }

        if (steps == null || steps.isEmpty()) {
            throw new ServiceException("Scenario '" + scenario.getName() + "' has no steps defined");
        }

        for (ScenarioStep step : steps) {
            GatlingTest mainTest = testService.findTestByTcid(step.getTestTcid());
            if (mainTest == null) {
                throw new ServiceException("GatlingTest not found for tcid: " + step.getTestTcid());
            }
            mainTest.setWaitTime(step.getWaitTime());

            // Gather dependency tests (Setup/Teardown) based on conditions field
            java.util.Map<String, java.util.List<String>> condMap = parseConditionString(mainTest.getConditions());

            // Helper lambda to add test & endpoint & meta to lists
            java.util.function.BiConsumer<java.util.Map.Entry<GatlingTest,String>, Endpoint> addItem = (entry, ept) -> {
                tests.add(entry.getKey());
                endpoints.add(ept);
                origins.add(entry.getValue().split("\\|",2)[0]); // origin stored before '|'
                modes.add(entry.getValue().split("\\|",2)[1]);
            };

            // ---- 1. Setups ----
            java.util.List<String> setups = condMap.getOrDefault("Setup", java.util.Collections.emptyList());
            for (String tcid : setups) {
                GatlingTest su = testService.findTestByTcid(tcid);
                if (su == null) throw new ServiceException("Setup test not found: " + tcid);
                enrichTemplates(su);
                Endpoint e = resolveEndpoint(su, endpointService);
                addItem.accept(Map.entry(su, mainTest.getTcid()+"|SETUP"), e);
            }

            // ---- 2. Main ----
            enrichTemplates(mainTest);
            Endpoint mainEp = resolveEndpoint(mainTest, endpointService);
            addItem.accept(Map.entry(mainTest, mainTest.getTcid()+"|MAIN"), mainEp);

            // ---- 3. Teardowns ----
            java.util.List<String> teardowns = condMap.getOrDefault("Teardown", java.util.Collections.emptyList());
            for (String tcid : teardowns) {
                GatlingTest td = testService.findTestByTcid(tcid);
                if (td == null) throw new ServiceException("Teardown test not found: " + tcid);
                enrichTemplates(td);
                Endpoint e = resolveEndpoint(td, endpointService);
                addItem.accept(Map.entry(td, mainTest.getTcid()+"|TEARDOWN"), e);
            }
        }

        // 2. Build single-user load parameters
        GatlingLoadParameters params = new GatlingLoadParameters();
        params.setType(ThreadGroupType.STANDARD);
        StandardThreadGroup tg = new StandardThreadGroup();
        tg.setNumThreads(1);
        tg.setLoops(1);
        tg.setScheduler(false);
        params.setStandardThreadGroup(tg);

        // 3. Execute asynchronously using existing runner
        Runnable wrapped = () -> {
            // Show global status after completion
            com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Functional Scenario Completed", com.qa.app.ui.vm.MainViewModel.StatusType.SUCCESS);
            if (onComplete != null) onComplete.run();
        };

        GatlingTestRunner.executeGatlingTests(tests, params, endpoints, origins, modes, wrapped);
    }

    // ---------------- Helper Methods -----------------
    private static void enrichTemplates(GatlingTest gt) {
        try {
            if ((gt.getBody() == null || gt.getBody().isEmpty()) && gt.getBodyTemplateId() > 0) {
                com.qa.app.model.BodyTemplate bt = new com.qa.app.service.impl.BodyTemplateServiceImpl().findBodyTemplateById(gt.getBodyTemplateId());
                if (bt != null) gt.setBody(bt.getContent());
            }
        } catch (Exception ignored) {}

        try {
            if ((gt.getHeaders() == null || gt.getHeaders().isEmpty()) && gt.getHeadersTemplateId() > 0) {
                com.qa.app.model.HeadersTemplate ht = new com.qa.app.service.impl.HeadersTemplateServiceImpl().getHeadersTemplateById(gt.getHeadersTemplateId());
                if (ht != null) gt.setHeaders(ht.getContent());
            }
        } catch (Exception ignored) {}
    }

    private static Endpoint resolveEndpoint(GatlingTest test, IEndpointService endpointService) throws ServiceException {
        try {
            Integer envId = EnvironmentContext.getCurrentEnvironmentId();
            Endpoint ep = endpointService.getEndpointByNameAndEnv(test.getEndpointName(), envId);
            if (ep == null) {
                throw new ServiceException("Endpoint '" + test.getEndpointName() + "' not found for test: " + test.getTcid());
            }
            return ep;
        } catch (ServiceException se) {
            throw se;
        } catch (Exception ex) {
            throw new ServiceException("Error retrieving endpoint for test: " + test.getTcid(), ex);
        }
    }

    private static java.util.Map<String, java.util.List<String>> parseConditionString(String cond) {
        java.util.Map<String, java.util.List<String>> map = new java.util.HashMap<>();
        if (cond == null || cond.isBlank()) return map;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[(\\w+)\\]([^\\[]*)");
        java.util.regex.Matcher m = p.matcher(cond);
        while (m.find()) {
            String prefix = m.group(1);
            String body = m.group(2).trim().replace(";", "");
            if (body.isBlank()) continue;
            java.util.List<String> tcids = java.util.Arrays.stream(body.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            map.put(prefix, tcids);
        }
        return map;
    }
} 