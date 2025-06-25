package com.qa.app.dao.api;

import com.qa.app.model.GroovyVariable;

import java.sql.SQLException;
import java.util.List;

public interface IGroovyVariableDao {
    void add(GroovyVariable variable) throws SQLException;
    void update(GroovyVariable variable) throws SQLException;
    void delete(int id) throws SQLException;
    void deleteAll() throws SQLException;
    GroovyVariable getById(int id) throws SQLException;
    List<GroovyVariable> getByProjectId(Integer projectId) throws SQLException;
    List<GroovyVariable> getAll() throws SQLException;
} 