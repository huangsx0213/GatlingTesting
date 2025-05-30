package com.example.app.dao;

import com.example.app.model.GatlingTest;
import com.example.app.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GatlingTestDaoImpl implements IGatlingTestDao {

    @Override
    public void addTest(GatlingTest test) throws SQLException {
        String sql = "INSERT INTO gatling_tests (is_run, suite, tcid, descriptions, conditions, " +
                    "body_override, exp_status, exp_result, save_fields, endpoint, headers, " +
                    "body_template, body_default, tags, wait_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setBoolean(1, test.isRun());
            pstmt.setString(2, test.getSuite());
            pstmt.setString(3, test.getTcid());
            pstmt.setString(4, test.getDescriptions());
            pstmt.setString(5, test.getConditions());
            pstmt.setString(6, test.getBodyOverride());
            pstmt.setString(7, test.getExpStatus());
            pstmt.setString(8, test.getExpResult());
            pstmt.setString(9, test.getSaveFields());
            pstmt.setString(10, test.getEndpoint());
            pstmt.setString(11, test.getHeaders());
            pstmt.setString(12, test.getBodyTemplate());
            pstmt.setString(13, test.getBodyDefault());
            pstmt.setString(14, test.getTags());
            pstmt.setInt(15, test.getWaitTime());
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
                    "conditions = ?, body_override = ?, exp_status = ?, exp_result = ?, save_fields = ?, " +
                    "endpoint = ?, headers = ?, body_template = ?, body_default = ?, tags = ?, wait_time = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, test.isRun());
            pstmt.setString(2, test.getSuite());
            pstmt.setString(3, test.getTcid());
            pstmt.setString(4, test.getDescriptions());
            pstmt.setString(5, test.getConditions());
            pstmt.setString(6, test.getBodyOverride());
            pstmt.setString(7, test.getExpStatus());
            pstmt.setString(8, test.getExpResult());
            pstmt.setString(9, test.getSaveFields());
            pstmt.setString(10, test.getEndpoint());
            pstmt.setString(11, test.getHeaders());
            pstmt.setString(12, test.getBodyTemplate());
            pstmt.setString(13, test.getBodyDefault());
            pstmt.setString(14, test.getTags());
            pstmt.setInt(15, test.getWaitTime());
            pstmt.setInt(16, test.getId());
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
        test.setBodyOverride(rs.getString("body_override"));
        test.setExpStatus(rs.getString("exp_status"));
        test.setExpResult(rs.getString("exp_result"));
        test.setSaveFields(rs.getString("save_fields"));
        test.setEndpoint(rs.getString("endpoint"));
        test.setHeaders(rs.getString("headers"));
        test.setBodyTemplate(rs.getString("body_template"));
        test.setBodyDefault(rs.getString("body_default"));
        test.setTags(rs.getString("tags"));
        test.setWaitTime(rs.getInt("wait_time"));
        return test;
    }
}