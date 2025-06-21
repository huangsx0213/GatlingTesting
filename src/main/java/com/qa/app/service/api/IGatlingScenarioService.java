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
    List<Scenario> findAllScenarios() throws ServiceException;
    List<ScenarioStep> findStepsByScenarioId(int scenarioId) throws ServiceException;

    void runScenario(int scenarioId) throws ServiceException;

    void upsertSchedule(int scenarioId, String cronExpr, boolean enabled) throws ServiceException;
    com.qa.app.model.ScenarioSchedule getSchedule(int scenarioId) throws ServiceException;
} 