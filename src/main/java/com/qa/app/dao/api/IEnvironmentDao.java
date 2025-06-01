package com.qa.app.dao.api;

import com.qa.app.model.Environment;
import java.sql.SQLException;
import java.util.List;

public interface IEnvironmentDao {
    void addEnvironment(Environment environment) throws SQLException;
    void updateEnvironment(Environment environment) throws SQLException;
    void deleteEnvironment(int id) throws SQLException;
    Environment getEnvironmentById(int id) throws SQLException;
    Environment getEnvironmentByName(String name) throws SQLException;
    List<Environment> getAllEnvironments() throws SQLException;
} 