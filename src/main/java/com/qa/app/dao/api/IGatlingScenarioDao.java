package com.qa.app.dao.api;

import com.qa.app.model.Scenario;
import com.qa.app.model.ScenarioStep;

import java.sql.SQLException;
import java.util.List;

public interface IGatlingScenarioDao {
    // Scenario CRUD
    void addScenario(Scenario scenario) throws SQLException;
    void updateScenario(Scenario scenario) throws SQLException;
    void deleteScenario(int scenarioId) throws SQLException;
    Scenario getScenarioById(int id) throws SQLException;
    List<Scenario> getAllScenarios() throws SQLException;

    // Scenario steps
    void deleteStepsByScenarioId(int scenarioId) throws SQLException;
    void addStep(int scenarioId, ScenarioStep step) throws SQLException;
    List<ScenarioStep> getStepsByScenarioId(int scenarioId) throws SQLException;

    // Schedule
    void upsertSchedule(int scenarioId, String cronExpr, boolean enabled) throws SQLException;
    com.qa.app.model.ScenarioSchedule getSchedule(int scenarioId) throws SQLException;
} 