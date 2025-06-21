package com.qa.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.dao.api.IGatlingScenarioDao;
import com.qa.app.dao.impl.GatlingScenarioDaoImpl;
import com.qa.app.model.*;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGatlingScenarioService;
import com.qa.app.service.api.IGatlingTestService;

import java.util.ArrayList;
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
            List<ScenarioStep> steps = scenarioDao.getStepsByScenarioId(scenarioId);
            // convert steps to tests
            List<GatlingTest> tests = new ArrayList<>();
            for (ScenarioStep step : steps) {
                GatlingTest gt = testService.findTestByTcid(step.getTestTcid());
                if (gt == null) {
                    throw new ServiceException("Test not found for tcid: " + step.getTestTcid());
                }
                // inject waitTime to GatlingTest
                gt.setWaitTime(step.getWaitTime());
                tests.add(gt);
            }
            // parse thread group
            GatlingLoadParameters params = objectMapper.readValue(sc.getThreadGroupJson(), GatlingLoadParameters.class);
            // run tests by testService
            testService.runTests(tests, params);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Failed to run scenario", e);
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
} 