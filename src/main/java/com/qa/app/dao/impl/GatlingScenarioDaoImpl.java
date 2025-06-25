package com.qa.app.dao.impl;

import com.qa.app.dao.api.IGatlingScenarioDao;
import com.qa.app.model.Scenario;
import com.qa.app.model.ScenarioStep;
import com.qa.app.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GatlingScenarioDaoImpl implements IGatlingScenarioDao {

    @Override
    public void addScenario(Scenario scenario) throws SQLException {
        String sql = "INSERT INTO scenario(name, desc, thread_group_json, schedule_json) VALUES(?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, scenario.getName());
            ps.setString(2, scenario.getDescription());
            ps.setString(3, scenario.getThreadGroupJson());
            ps.setString(4, scenario.getScheduleJson());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) scenario.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public void updateScenario(Scenario scenario) throws SQLException {
        String sql = "UPDATE scenario SET name=?, desc=?, thread_group_json=?, schedule_json=? WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scenario.getName());
            ps.setString(2, scenario.getDescription());
            ps.setString(3, scenario.getThreadGroupJson());
            ps.setString(4, scenario.getScheduleJson());
            ps.setInt(5, scenario.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteScenario(int scenarioId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delSteps = conn.prepareStatement("DELETE FROM scenario_step WHERE scenario_id=?")) {
                delSteps.setInt(1, scenarioId);
                delSteps.executeUpdate();
            }
            try (PreparedStatement delSchedules = conn.prepareStatement("DELETE FROM scenario_schedule WHERE scenario_id=?")) {
                delSchedules.setInt(1, scenarioId);
                delSchedules.executeUpdate();
            }
            try (PreparedStatement delScenario = conn.prepareStatement("DELETE FROM scenario WHERE id=?")) {
                delScenario.setInt(1, scenarioId);
                delScenario.executeUpdate();
            }
            conn.commit();
        }
    }

    @Override
    public Scenario getScenarioById(int id) throws SQLException {
        String sql = "SELECT id, name, desc, thread_group_json, schedule_json FROM scenario WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Scenario sc = new Scenario();
                    sc.setId(rs.getInt("id"));
                    sc.setName(rs.getString("name"));
                    sc.setDescription(rs.getString("desc"));
                    sc.setThreadGroupJson(rs.getString("thread_group_json"));
                    sc.setScheduleJson(rs.getString("schedule_json"));
                    return sc;
                }
            }
        }
        return null;
    }

    @Override
    public List<Scenario> getAllScenarios() throws SQLException {
        String sql = "SELECT id, name, desc, thread_group_json, schedule_json FROM scenario ORDER BY id DESC";
        List<Scenario> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Scenario sc = new Scenario();
                sc.setId(rs.getInt("id"));
                sc.setName(rs.getString("name"));
                sc.setDescription(rs.getString("desc"));
                sc.setThreadGroupJson(rs.getString("thread_group_json"));
                sc.setScheduleJson(rs.getString("schedule_json"));
                list.add(sc);
            }
        }
        return list;
    }

    @Override
    public void deleteStepsByScenarioId(int scenarioId) throws SQLException {
        String sql = "DELETE FROM scenario_step WHERE scenario_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scenarioId);
            ps.executeUpdate();
        }
    }

    @Override
    public void addStep(int scenarioId, ScenarioStep step) throws SQLException {
        String sql = "INSERT INTO scenario_step(scenario_id, order_index, test_tcid, wait_time, tags) VALUES(?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scenarioId);
            ps.setInt(2, step.getOrder());
            ps.setString(3, step.getTestTcid());
            ps.setInt(4, step.getWaitTime());
            ps.setString(5, step.getTags());
            ps.executeUpdate();
        }
    }

    @Override
    public List<ScenarioStep> getStepsByScenarioId(int scenarioId) throws SQLException {
        String sql = "SELECT order_index, test_tcid, wait_time, tags FROM scenario_step WHERE scenario_id=? ORDER BY order_index ASC";
        List<ScenarioStep> steps = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scenarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ScenarioStep step = new ScenarioStep();
                    step.setOrder(rs.getInt("order_index"));
                    step.setTestTcid(rs.getString("test_tcid"));
                    step.setWaitTime(rs.getInt("wait_time"));
                    step.setTags(rs.getString("tags"));
                    steps.add(step);
                }
            }
        }
        return steps;
    }

    @Override
    public void upsertSchedule(int scenarioId, String cronExpr, boolean enabled) throws SQLException {
        String sql = "INSERT INTO scenario_schedule(scenario_id, cron_expr, enabled) VALUES(?,?,?) " +
                     "ON CONFLICT(scenario_id) DO UPDATE SET cron_expr=excluded.cron_expr, enabled=excluded.enabled";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scenarioId);
            ps.setString(2, cronExpr);
            ps.setBoolean(3, enabled);
            ps.executeUpdate();
        }
    }

    @Override
    public com.qa.app.model.ScenarioSchedule getSchedule(int scenarioId) throws SQLException {
        String sql = "SELECT scenario_id, cron_expr, next_run_at, enabled FROM scenario_schedule WHERE scenario_id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scenarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    com.qa.app.model.ScenarioSchedule s = new com.qa.app.model.ScenarioSchedule();
                    s.setScenarioId(rs.getInt("scenario_id"));
                    s.setCronExpr(rs.getString("cron_expr"));
                    s.setNextRunAt(rs.getString("next_run_at"));
                    s.setEnabled(rs.getBoolean("enabled"));
                    return s;
                }
            }
        }
        return null;
    }
} 