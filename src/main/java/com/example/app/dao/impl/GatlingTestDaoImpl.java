package com.example.app.dao.impl;

import com.example.app.dao.api.IGatlingTestDao;
import com.example.app.model.GatlingTest;
import com.example.app.util.DBUtil;
import org.json.JSONObject;
import org.json.JSONException;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GatlingTestDaoImpl implements IGatlingTestDao {

    @Override
    public void addTest(GatlingTest test) throws SQLException {
        String sql = "INSERT INTO gatling_tests (is_run, suite, tcid, descriptions, conditions, " +
                    "exp_status, exp_result, save_fields, endpoint, http_method, headers, " +
                    "body, tags, wait_time, body_template_name, dynamic_variables, headers_dynamic_variables) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setBoolean(1, test.isRun());
            pstmt.setString(2, test.getSuite());
            pstmt.setString(3, test.getTcid());
            pstmt.setString(4, test.getDescriptions());
            pstmt.setString(5, test.getConditions());
            pstmt.setString(6, test.getExpStatus());
            pstmt.setString(7, test.getExpResult());
            pstmt.setString(8, test.getSaveFields());
            pstmt.setString(9, test.getEndpoint());
            pstmt.setString(10, test.getHttpMethod());
            pstmt.setString(11, test.getHeaders());
            pstmt.setString(12, test.getBody());
            pstmt.setString(13, test.getTags());
            pstmt.setInt(14, test.getWaitTime());
            pstmt.setString(15, test.getBodyTemplateName());
            pstmt.setString(16, convertMapToJson(test.getDynamicVariables()));
            pstmt.setString(17, convertMapToJson(test.getHeadersDynamicVariables()));
            pstmt.executeUpdate();

            // Get the generated ID and set it to the test object
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
        String sql = "SELECT * FROM gatling_tests ORDER BY id";
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
        String sql = "SELECT * FROM gatling_tests WHERE suite = ? ORDER BY id";
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
        String sql = "UPDATE gatling_tests SET is_run = ?, suite = ?, tcid = ?, descriptions = ?, " +
                    "conditions = ?, exp_status = ?, exp_result = ?, save_fields = ?, " +
                    "endpoint = ?, http_method = ?, headers = ?, body = ?, tags = ?, wait_time = ?, headers_template_name = ?, " +
                    "body_template_name = ?, dynamic_variables = ?, headers_dynamic_variables = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, test.isRun());
            pstmt.setString(2, test.getSuite());
            pstmt.setString(3, test.getTcid());
            pstmt.setString(4, test.getDescriptions());
            pstmt.setString(5, test.getConditions());
            pstmt.setString(6, test.getExpStatus());
            pstmt.setString(7, test.getExpResult());
            pstmt.setString(8, test.getSaveFields());
            pstmt.setString(9, test.getEndpoint());
            pstmt.setString(10, test.getHttpMethod());
            pstmt.setString(11, test.getHeaders());
            pstmt.setString(12, test.getBody());
            pstmt.setString(13, test.getTags());
            pstmt.setInt(14, test.getWaitTime());
            pstmt.setString(15, test.getHeadersTemplateName());
            pstmt.setString(16, test.getBodyTemplateName());
            pstmt.setString(17, convertMapToJson(test.getDynamicVariables()));
            pstmt.setString(18, convertMapToJson(test.getHeadersDynamicVariables()));
            pstmt.setInt(19, test.getId());
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
    public void updateTestRunStatus(int id, boolean isRun) throws SQLException {
        String sql = "UPDATE gatling_tests SET is_run = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isRun);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    private GatlingTest createTestFromResultSet(ResultSet rs) throws SQLException {
        GatlingTest test = new GatlingTest();
        test.setId(rs.getInt("id"));
        test.setRun(rs.getBoolean("is_run"));
        test.setSuite(rs.getString("suite"));
        test.setTcid(rs.getString("tcid"));
        test.setDescriptions(rs.getString("descriptions"));
        test.setConditions(rs.getString("conditions"));
        test.setExpStatus(rs.getString("exp_status"));
        test.setExpResult(rs.getString("exp_result"));
        test.setSaveFields(rs.getString("save_fields"));
        test.setEndpoint(rs.getString("endpoint"));
        test.setHttpMethod(rs.getString("http_method"));
        test.setHeaders(rs.getString("headers"));
        test.setBody(rs.getString("body") == null ? "" : rs.getString("body"));
        test.setTags(rs.getString("tags"));
        test.setWaitTime(rs.getInt("wait_time"));
        test.setBodyTemplateName(rs.getString("body_template_name") == null ? "" : rs.getString("body_template_name"));
        test.setHeadersTemplateName(rs.getString("headers_template_name") == null ? "" : rs.getString("headers_template_name"));    
        test.setDynamicVariables(convertJsonToMap(rs.getString("dynamic_variables")));
        test.setHeadersDynamicVariables(convertJsonToMap(rs.getString("headers_dynamic_variables")));
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
}