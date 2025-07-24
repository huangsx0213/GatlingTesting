package com.qa.app.dao.impl;

import org.json.JSONObject;

import com.qa.app.dao.api.IGatlingTestDao;
import com.qa.app.dao.util.DBUtil;
import com.qa.app.model.GatlingTest;

import org.json.JSONException;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GatlingTestDaoImpl implements IGatlingTestDao {

    @Override
    public void addTest(GatlingTest test) throws SQLException {
        String sql = "INSERT INTO gatling_tests(is_enabled, suite, tcid, descriptions, conditions, response_checks, " +
                "endpoint_name, tags, wait_time, body_template_id, headers_template_id, project_id, " +
                "body_dynamic_variables, headers_dynamic_variables, endpoint_dynamic_variables, display_order) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                    pstmt.setBoolean(1, test.isEnabled());
                    pstmt.setString(2, test.getSuite());
                    pstmt.setString(3, test.getTcid());
                    pstmt.setString(4, test.getDescriptions());
                    pstmt.setString(5, test.getConditions());
            pstmt.setString(6, test.getResponseChecks());
            pstmt.setString(7, test.getEndpointName());
            pstmt.setString(8, test.getTags());
            pstmt.setInt(9, test.getWaitTime());
                    pstmt.setInt(10, test.getBodyTemplateId());
            pstmt.setInt(11, test.getHeadersTemplateId());
                    if (test.getProjectId() != null) {
                pstmt.setInt(12, test.getProjectId());
                    } else {
                pstmt.setNull(12, java.sql.Types.INTEGER);
                    }
            pstmt.setString(13, convertMapToJson(test.getBodyDynamicVariables()));
            pstmt.setString(14, convertMapToJson(test.getHeadersDynamicVariables()));
            pstmt.setString(15, convertMapToJson(test.getEndpointDynamicVariables()));
            pstmt.setInt(16, test.getDisplayOrder());

                    pstmt.executeUpdate();

                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            test.setId(generatedKeys.getInt(1));
                        }
            }
        }
    }

    @Override
    public GatlingTest getTestById(int id) throws SQLException {
        String sql = "SELECT * FROM gatling_tests WHERE id = ?";
        GatlingTest test = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    test = createTestFromResultSet(rs);
                }
            }
        }
        return test;
    }

    @Override
    public GatlingTest getTestByTcid(String tcid) throws SQLException {
        String sql = "SELECT * FROM gatling_tests WHERE tcid = ?";
        GatlingTest test = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tcid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    test = createTestFromResultSet(rs);
                }
            }
        }
        return test;
    }

    @Override
    public List<GatlingTest> getAllTests() throws SQLException {
        String sql = "SELECT * FROM gatling_tests ORDER BY display_order";
        List<GatlingTest> tests = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tests.add(createTestFromResultSet(rs));
            }
        }
        return tests;
    }

    @Override
    public List<GatlingTest> getTestsBySuite(String suite) throws SQLException {
        String sql = "SELECT * FROM gatling_tests WHERE suite = ? ORDER BY display_order";
        List<GatlingTest> tests = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, suite);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tests.add(createTestFromResultSet(rs));
                }
            }
        }
        return tests;
    }

    @Override
    public void updateTest(GatlingTest test) throws SQLException {
        String sql = "UPDATE gatling_tests SET is_enabled = ?, suite = ?, tcid = ?, descriptions = ?, " +
                "conditions = ?, response_checks = ?, endpoint_name = ?, " +
                "tags = ?, wait_time = ?, body_template_id = ?, headers_template_id = ?, project_id = ?, " +
                "body_dynamic_variables = ?, headers_dynamic_variables = ?, endpoint_dynamic_variables = ?, " +
                "display_order = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, test.isEnabled());
            pstmt.setString(2, test.getSuite());
            pstmt.setString(3, test.getTcid());
            pstmt.setString(4, test.getDescriptions());
            pstmt.setString(5, test.getConditions());
            pstmt.setString(6, test.getResponseChecks());
            pstmt.setString(7, test.getEndpointName());
            pstmt.setString(8, test.getTags());
            pstmt.setInt(9, test.getWaitTime());
            pstmt.setInt(10, test.getBodyTemplateId());
            pstmt.setInt(11, test.getHeadersTemplateId());
            if (test.getProjectId() != null) {
                pstmt.setInt(12, test.getProjectId());
            } else {
                pstmt.setNull(12, java.sql.Types.INTEGER);
            }
            pstmt.setString(13, convertMapToJson(test.getBodyDynamicVariables()));
            pstmt.setString(14, convertMapToJson(test.getHeadersDynamicVariables()));
            pstmt.setString(15, convertMapToJson(test.getEndpointDynamicVariables()));
            pstmt.setInt(16, test.getDisplayOrder());
            pstmt.setInt(17, test.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteTest(int id) throws SQLException {
        String sql = "DELETE FROM gatling_tests WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateTestRunStatus(int id, boolean isEnabled) throws SQLException {
        String sql = "UPDATE gatling_tests SET is_enabled = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isEnabled);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public List<GatlingTest> getTestsByProjectId(Integer projectId) throws SQLException {
        String sql = "SELECT * FROM gatling_tests WHERE project_id = ? ORDER BY display_order";
        List<GatlingTest> tests = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tests.add(createTestFromResultSet(rs));
                }
            }
        }
        return tests;
    }

    private GatlingTest createTestFromResultSet(ResultSet rs) throws SQLException {
        GatlingTest test = new GatlingTest();
        test.setId(rs.getInt("id"));
        test.setEnabled(rs.getBoolean("is_enabled"));
        test.setSuite(rs.getString("suite"));
        test.setTcid(rs.getString("tcid"));
        test.setDescriptions(rs.getString("descriptions"));
        test.setConditions(rs.getString("conditions"));
        test.setResponseChecks(rs.getString("response_checks"));
        test.setEndpointName(rs.getString("endpoint_name"));
        test.setTags(rs.getString("tags"));
        test.setWaitTime(rs.getInt("wait_time"));
        test.setBodyTemplateId(rs.getInt("body_template_id"));
        test.setHeadersTemplateId(rs.getInt("headers_template_id"));
        test.setDynamicVariables(convertJsonToMap(rs.getString("body_dynamic_variables")));
        test.setHeadersDynamicVariables(convertJsonToMap(rs.getString("headers_dynamic_variables")));
        test.setEndpointDynamicVariables(convertJsonToMap(rs.getString("endpoint_dynamic_variables")));
        Object pid = rs.getObject("project_id");
        if (pid != null) {
            test.setProjectId((Integer)pid);
        }
        test.setReportPath(rs.getString("report_path"));
        boolean lastRunPassed = rs.getBoolean("last_run_passed");
        if (!rs.wasNull()) {
            test.setLastRunPassed(lastRunPassed);
        } else {
            test.setLastRunPassed(null);
        }
        test.setDisplayOrder(rs.getInt("display_order"));
        return test;
    }

    private String convertMapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new JSONObject(map).toString();
    }

    private Map<String, String> convertJsonToMap(String jsonString) {
        Map<String, String> map = new HashMap<>();
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return map;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            for (String key : jsonObject.keySet()) {
                map.put(key, jsonObject.getString(key));
            }
        } catch (JSONException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            // Optionally, return an empty map or re-throw a custom exception
        }
        return map;
    }

    @Override
    public void updateOrder(List<GatlingTest> tests) throws SQLException {
        String sql = "UPDATE gatling_tests SET display_order = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (GatlingTest test : tests) {
                    ps.setInt(1, test.getDisplayOrder());
                    ps.setInt(2, test.getId());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}