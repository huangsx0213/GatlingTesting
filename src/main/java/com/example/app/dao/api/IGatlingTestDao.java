package com.example.app.dao.api;

import com.example.app.model.GatlingTest;

import java.sql.SQLException;
import java.util.List;

public interface IGatlingTestDao {
    void addTest(GatlingTest test) throws SQLException;
    GatlingTest getTestById(int id) throws SQLException;
    GatlingTest getTestByTcid(String tcid) throws SQLException;
    List<GatlingTest> getAllTests() throws SQLException;
    List<GatlingTest> getTestsBySuite(String suite) throws SQLException;
    void updateTest(GatlingTest test) throws SQLException;
    void deleteTest(int id) throws SQLException;
    void updateTestRunStatus(int id, boolean isEnabled) throws SQLException;
}