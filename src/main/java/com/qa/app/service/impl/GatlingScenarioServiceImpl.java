package com.qa.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.dao.api.IGatlingScenarioDao;
import com.qa.app.dao.impl.GatlingScenarioDaoImpl;
import com.qa.app.model.*;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGatlingScenarioService;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.api.IEndpointService;

import java.util.List;

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
    public List<Scenario> findAllScenarios() throws ServiceException {
        try {
            return scenarioDao.getAllScenarios();
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
    public void runScenario(int scenarioId) throws ServiceException {
        try {
            Scenario sc = scenarioDao.getScenarioById(scenarioId);
            if (sc == null) throw new ServiceException("Scenario not found");

            // Use runScenarios(List) method even if there is only one element, to follow MultiScenarioSimulation logic
            java.util.List<Scenario> list = java.util.Collections.singletonList(sc);
            runScenarios(list);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Failed to run scenario", e);
        }
    }

    @Override
    public void runScenarios(java.util.List<Scenario> scenarios) throws ServiceException {
        if (scenarios == null || scenarios.isEmpty()) return;

        try {
            // ===== 1. 准备数据 =====
            class ScenarioRunItem {
                @SuppressWarnings("unused")
                public Scenario scenario;
                @SuppressWarnings("unused")
                public GatlingLoadParameters params;
                @SuppressWarnings("unused")
                public java.util.List<java.util.Map<String, Object>> items;
            }

            java.util.List<ScenarioRunItem> runItems = new java.util.ArrayList<>();

            IEndpointService endpointService = new EndpointServiceImpl();

            for (Scenario sc : scenarios) {
                ScenarioRunItem sri = new ScenarioRunItem();
                sri.scenario = sc;
                sri.params = objectMapper.readValue(sc.getThreadGroupJson(), GatlingLoadParameters.class);

                java.util.List<ScenarioStep> steps = scenarioDao.getStepsByScenarioId(sc.getId());
                java.util.List<java.util.Map<String, Object>> batchItems = new java.util.ArrayList<>();
                for (ScenarioStep step : steps) {
                    GatlingTest gt = testService.findTestByTcid(step.getTestTcid());
                    if (gt == null) {
                        throw new ServiceException("Test not found for tcid: " + step.getTestTcid());
                    }
                    gt.setWaitTime(step.getWaitTime());

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
                    map.put("endpoint", endpointService.getEndpointByName(gt.getEndpointName()));
                    batchItems.add(map);
                }
                sri.items = batchItems;
                runItems.add(sri);
            }

            // ===== 2. 序列化到临时文件 =====
            java.io.File multiFile = java.io.File.createTempFile("gatling_multiscenario_", ".json");
            multiFile.deleteOnExit();
            new ObjectMapper().writeValue(multiFile, runItems);

            // ===== 3. 启动 Gatling =====
            String javaHome = System.getProperty("java.home");
            String javaBin = java.nio.file.Paths.get(javaHome, "bin", "java").toString();
            String classpath = assembleClasspath();
            String gatlingMain = "io.gatling.app.Gatling";
            String simulationClass = com.qa.app.util.MultiScenarioSimulation.class.getName();
            String resultsPath = java.nio.file.Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();

            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(javaBin);
            command.add("--add-opens");
            command.add("java.base/java.lang=ALL-UNNAMED");
            command.add("-cp");
            command.add(classpath);
            command.add("-Dgatling.multiscenario.file=" + multiFile.getAbsolutePath());
            command.add(gatlingMain);
            command.add("-s");
            command.add(simulationClass);
            command.add("-rf");
            command.add(resultsPath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();

            // 在独立线程中启动并等待 Gatling 进程，避免阻塞调用线程
            new Thread(() -> {
                try {
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Multi-scenario Starting", com.qa.app.ui.vm.MainViewModel.StatusType.INFO);

                    int exitCode;
                    java.lang.Process p = pb.start();
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Multi-scenario Running", com.qa.app.ui.vm.MainViewModel.StatusType.INFO);

                    exitCode = p.waitFor();
                    if (exitCode != 0) {
                        System.err.println("Gatling multi-scenario execution failed, exit code: " + exitCode);
                        com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Multi-scenario Failed, exit code: " + exitCode, com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                    } else {
                        System.out.println("Gatling multi-scenario execution completed.");
                        com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Multi-scenario Completed", com.qa.app.ui.vm.MainViewModel.StatusType.SUCCESS);
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to execute Gatling multi-scenario: " + ex.getMessage());
                    ex.printStackTrace();
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Multi-scenario Exception: " + ex.getMessage(), com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                }
            }, "gatling-multi-scenario-runner").start();

        } catch(ServiceException se){
            throw se;
        } catch(Exception e){
            throw new ServiceException("Failed to run multi-scenario", e);
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

    private static String assembleClasspath() {
        String cp = System.getProperty("java.class.path");
        try {
            String selfPath = new java.io.File(GatlingScenarioServiceImpl.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getPath();
            if (!cp.contains(selfPath)) {
                cp += java.io.File.pathSeparator + selfPath;
            }
        } catch (Exception ignored) { }
        return cp;
    }
} 