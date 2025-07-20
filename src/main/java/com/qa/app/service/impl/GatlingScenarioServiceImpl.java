package com.qa.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.dao.api.IGatlingScenarioDao;
import com.qa.app.dao.impl.GatlingScenarioDaoImpl;
import com.qa.app.model.*;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGatlingScenarioService;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.api.IEndpointService;
import com.qa.app.service.EnvironmentContext;
import com.qa.app.service.runner.GatlingRunnerUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class GatlingScenarioServiceImpl implements IGatlingScenarioService {

    private final IGatlingScenarioDao scenarioDao = new GatlingScenarioDaoImpl();
    private final IGatlingTestService testService = new GatlingTestServiceImpl();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void createScenario(Scenario sc, List<ScenarioStep> steps) throws ServiceException {
        try {
            scenarioDao.addScenario(sc);
            for (ScenarioStep step : steps) {
                scenarioDao.addStep(sc.getId(), step);
            }
        } catch (Exception e) {
            throw new ServiceException("Failed to create scenario: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateScenario(Scenario sc, List<ScenarioStep> steps) throws ServiceException {
        try {
            // make sure project id is set
            if (sc.getProjectId() == 0 && sc.getId() > 0) {
                Scenario existing = scenarioDao.getScenarioById(sc.getId());
                if (existing != null) {
                    sc.setProjectId(existing.getProjectId());
                }
            }
            scenarioDao.updateScenario(sc);
            scenarioDao.deleteStepsByScenarioId(sc.getId());
            for (ScenarioStep s : steps) {
                scenarioDao.addStep(sc.getId(), s);
            }
        } catch (Exception e) {
            throw new ServiceException("Failed to update scenario: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteScenario(int scenarioId) throws ServiceException {
        try {
            scenarioDao.deleteScenario(scenarioId);
        } catch (Exception e) {
            throw new ServiceException("Failed to delete scenario", e);
        }
    }

    @Override
    public Scenario duplicateScenario(int scenarioId) throws ServiceException {
        try {
            Scenario src = scenarioDao.getScenarioById(scenarioId);
            if (src == null) throw new ServiceException("Scenario not found");
            Scenario copy = new Scenario();
            copy.setName(src.getName() + "_copy");
            copy.setDescription(src.getDescription());
            copy.setThreadGroupJson(src.getThreadGroupJson());
            copy.setScheduleJson(src.getScheduleJson());
            scenarioDao.addScenario(copy);
            List<ScenarioStep> steps = scenarioDao.getStepsByScenarioId(scenarioId);
            for (ScenarioStep step : steps) {
                scenarioDao.addStep(copy.getId(), step);
            }
            return copy;
        } catch (Exception e) {
            throw new ServiceException("Failed to duplicate scenario", e);
        }
    }

    @Override
    public List<Scenario> findAllScenarios(Integer projectId) throws ServiceException {
        try {
            return scenarioDao.getAllScenarios(projectId);
        } catch (Exception e) {
            throw new ServiceException("Failed to query scenarios", e);
        }
    }

    @Override
    public List<ScenarioStep> findStepsByScenarioId(int scenarioId) throws ServiceException {
        try {
            return scenarioDao.getStepsByScenarioId(scenarioId);
        } catch (Exception e) {
            throw new ServiceException("Failed to load steps", e);
        }
    }

    @Override
    public void updateOrder(List<Scenario> scenarios) throws ServiceException {
        try {
            if (scenarios == null || scenarios.isEmpty()) {
                return; // Nothing to update
            }
            scenarioDao.updateOrder(scenarios);
        } catch (Exception e) {
            throw new ServiceException("Database error while updating scenario order: " + e.getMessage(), e);
        }
    }

    @Override
    public void runScenarios(java.util.List<com.qa.app.model.Scenario> scenarios, java.lang.Runnable onComplete) throws ServiceException {
        if (scenarios == null || scenarios.isEmpty()) {
            if (onComplete != null) {
                if (javafx.application.Platform.isFxApplicationThread()) {
                    onComplete.run();
                } else {
                    javafx.application.Platform.runLater(onComplete);
                }
            }
            return;
        }

        try {
            // ===== 1. 准备数据 =====
            java.util.List<ScenarioRunItem> runItems = new java.util.ArrayList<>();

            IEndpointService endpointService = new EndpointServiceImpl();

            for (com.qa.app.model.Scenario sc : scenarios) {
                GatlingLoadParameters params = objectMapper.readValue(sc.getThreadGroupJson(), GatlingLoadParameters.class);

                java.util.List<ScenarioStep> steps = scenarioDao.getStepsByScenarioId(sc.getId());
                if (steps == null || steps.isEmpty()) {
                    throw new ServiceException("Scenario '" + sc.getName() + "' has no steps defined – cannot run.");
                }
                
                // This list will contain all tests in the correct order: setup, main, teardown
                List<GatlingTest> allTestsWithDeps = new java.util.ArrayList<>();
                for (ScenarioStep step : steps) {
                    GatlingTest mainTest = testService.findTestByTcid(step.getTestTcid());
                    if (mainTest == null) {
                        throw new ServiceException("Test not found for tcid: " + step.getTestTcid());
                    }
                    mainTest.setWaitTime(step.getWaitTime());

                    Map<String, List<String>> condMap = parseConditionString(mainTest.getConditions());

                    // 1) Add Setup tests
                    List<String> setups = condMap.getOrDefault("Setup", java.util.Collections.emptyList());
                    for (String tcid : setups) {
                        GatlingTest setupTest = testService.findTestByTcid(tcid);
                        if (setupTest == null) {
                            throw new ServiceException("Setup test not found: " + tcid + " (required by " + mainTest.getTcid() + ")");
                        }
                        allTestsWithDeps.add(setupTest);
                    }

                    // 2) Add Main test
                    allTestsWithDeps.add(mainTest);

                    // 3) Add Teardown tests
                    List<String> teardowns = condMap.getOrDefault("Teardown", java.util.Collections.emptyList());
                    for (String tcid : teardowns) {
                        GatlingTest teardownTest = testService.findTestByTcid(tcid);
                        if (teardownTest == null) {
                            throw new ServiceException("Teardown test not found: " + tcid + " (required by " + mainTest.getTcid() + ")");
                        }
                        allTestsWithDeps.add(teardownTest);
                    }
                }

                java.util.List<java.util.Map<String, Object>> batchItems = new java.util.ArrayList<>();
                for (GatlingTest gt : allTestsWithDeps) {
                    // enrich templates (same logic as GatlingTestServiceImpl)
                    try {
                        if ((gt.getBody() == null || gt.getBody().isEmpty()) && gt.getBodyTemplateId() > 0) {
                            com.qa.app.model.BodyTemplate bt = new com.qa.app.service.impl.BodyTemplateServiceImpl().findBodyTemplateById(gt.getBodyTemplateId());
                            if (bt != null) gt.setBody(bt.getContent());
                        }
                    } catch (Exception ignored) { }

                    try {
                        if ((gt.getHeaders() == null || gt.getHeaders().isEmpty()) && gt.getHeadersTemplateId() > 0) {
                            com.qa.app.model.HeadersTemplate ht = new com.qa.app.service.impl.HeadersTemplateServiceImpl().getHeadersTemplateById(gt.getHeadersTemplateId());
                            if (ht != null) gt.setHeaders(ht.getContent());
                        }
                    } catch (Exception ignored) { }

                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("test", gt);
                    // Resolve endpoint by name and current environment
                    com.qa.app.model.Endpoint endpoint = null;
                    try {
                        Integer envId = EnvironmentContext.getCurrentEnvironmentId();
                        endpoint = endpointService.getEndpointByNameAndEnv(gt.getEndpointName(), envId);

                    } catch (Exception ex) {
                        throw new ServiceException("Error retrieving endpoint for test: " + gt.getTcid() + ": " + ex.getMessage(), ex);
                    }

                    if (endpoint == null) {
                        throw new ServiceException("Endpoint '" + gt.getEndpointName() + "' not found in current environment for test: " + gt.getTcid());
                    }

                    map.put("endpoint", endpoint);
                    batchItems.add(map);
                }
                if (batchItems.isEmpty()) {
                    throw new ServiceException("Scenario '" + sc.getName() + "' has no executable steps (tests/endpoints not found).");
                }
                runItems.add(new ScenarioRunItem(sc, params, batchItems));
            }

            // ===== 2. 序列化到临时文件 =====
            java.io.File multiFile = java.io.File.createTempFile("gatling_multiscenario_", ".json");
            multiFile.deleteOnExit();
            if (runItems.isEmpty()) {
                throw new ServiceException("No valid scenario steps found to execute.");
            }
            new com.fasterxml.jackson.databind.ObjectMapper().writeValue(multiFile, runItems);

            // ===== 3. 启动 Gatling =====
            String resultsPath = Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();
            Map<String, String> sysProps = new HashMap<>();
            sysProps.put("gatling.multiscenario.file", multiFile.getAbsolutePath());
            List<String> command = GatlingRunnerUtils.buildGatlingCommand(com.qa.app.service.runner.GatlingScenarioSimulation.class.getName(), sysProps, resultsPath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();

            // 在独立线程中启动并等待 Gatling 进程，避免阻塞调用线程
            new Thread(() -> {
                try {
                    int exitCode;
                    java.lang.Process p = null;
                    try {
                        p = pb.start();
                    } catch (Exception ex) {
                        System.err.println("Failed to start Gatling process: " + ex.getMessage());
                        throw ex;
                    }
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Running " + scenarios.size() + " Gatling scenario(s)", com.qa.app.ui.vm.MainViewModel.StatusType.INFO);

                    exitCode = p.waitFor();
                    if (exitCode != 0) {
                        System.err.println("Gatling scenario(s) Failed, exit code: " + exitCode);
                        com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Gatling scenario(s) Failed, exit code: " + exitCode, com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                    } else {
                        System.out.println("Gatling scenario(s) Completed.");
                        com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Gatling Scenario(s) Completed", com.qa.app.ui.vm.MainViewModel.StatusType.SUCCESS);
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to execute Gatling Scenario(s): " + ex.getMessage());
                    ex.printStackTrace();
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Gatling Scenario(s) Exception: " + ex.getMessage(), com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                } finally {
                    if (onComplete != null) {
                        if (javafx.application.Platform.isFxApplicationThread()) {
                            onComplete.run();
                        } else {
                            javafx.application.Platform.runLater(onComplete);
                        }
                    }
                }
            }, "scenario-runner").start();

        } catch(ServiceException se){
            throw se;
        } catch(Exception e){
            throw new ServiceException("Failed to run Gatling scenario(s)", e);
        }
    }


    @Override
    public void upsertSchedule(int scenarioId, String cronExpr, boolean enabled) throws ServiceException {
        try {
            scenarioDao.upsertSchedule(scenarioId, cronExpr, enabled);
        } catch (Exception e) {
            throw new ServiceException("Failed to update schedule", e);
        }
    }

    @Override
    public com.qa.app.model.ScenarioSchedule getSchedule(int scenarioId) throws ServiceException {
        try {
            return scenarioDao.getSchedule(scenarioId);
        } catch (Exception e) {
            throw new ServiceException("Failed to load schedule", e);
        }
    }

    /**
     * Parse the Condition string, e.g. "[Setup]TC001,TC002;[Teardown]TC003" -> Map
     * Copied from GatlingTestServiceImpl for standalone use.
     */
    private Map<String, java.util.List<String>> parseConditionString(String cond) {
        java.util.Map<String, java.util.List<String>> map = new java.util.HashMap<>();
        if (cond == null || cond.isBlank()) return map;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[(\\w+)\\]([^\\[]*)");
        java.util.regex.Matcher m = p.matcher(cond);
        while (m.find()) {
            String prefix = m.group(1);
            String body = m.group(2).trim().replace(";", ""); // remove stray semicolons
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