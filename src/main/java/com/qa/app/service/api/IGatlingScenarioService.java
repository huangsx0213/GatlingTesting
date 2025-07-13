package com.qa.app.service.api;

import com.qa.app.model.Scenario;
import com.qa.app.model.ScenarioStep;
import com.qa.app.service.ServiceException;

import java.util.List;

public interface IGatlingScenarioService {
    void createScenario(Scenario sc, List<ScenarioStep> steps) throws ServiceException;
    void updateScenario(Scenario sc, List<ScenarioStep> steps) throws ServiceException;
    void deleteScenario(int scenarioId) throws ServiceException;
    Scenario duplicateScenario(int scenarioId) throws ServiceException;
    List<Scenario> findAllScenarios(Integer projectId) throws ServiceException;
    List<ScenarioStep> findStepsByScenarioId(int scenarioId) throws ServiceException;
    void updateOrder(List<Scenario> scenarios) throws ServiceException;

    void runScenario(int scenarioId) throws ServiceException;

    /**
     * Run multiple scenarios in one go. The implementation should ensure that each scenario is executed
     * with its own thread group configuration so that they can run concurrently, similar to multiple thread
     * groups in JMeter.
     *
     * @param scenarios the scenario list to run
     * @throws ServiceException if any underlying error occurs
     */
    void runScenarios(java.util.List<com.qa.app.model.Scenario> scenarios) throws ServiceException;

    void runScenarios(java.util.List<com.qa.app.model.Scenario> scenarios,
                      java.lang.Runnable onComplete) throws ServiceException;

    void upsertSchedule(int scenarioId, String cronExpr, boolean enabled) throws ServiceException;
    com.qa.app.model.ScenarioSchedule getSchedule(int scenarioId) throws ServiceException;
} 