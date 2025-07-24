package com.qa.app.dao.api;

import com.qa.app.model.VariableTransformMethod;

import java.sql.SQLException;
import java.util.List;

/**
 * DAO abstraction for {@link VariableTransformMethod} CRUD operations.
 */
public interface IVariableTransformMethodDao {

    void addMethod(VariableTransformMethod method) throws SQLException;

    void updateMethod(VariableTransformMethod method) throws SQLException;

    void deleteMethod(int id) throws SQLException;

    VariableTransformMethod getMethodById(int id) throws SQLException;

    VariableTransformMethod getMethodByName(String name) throws SQLException;

    List<VariableTransformMethod> getAllMethods() throws SQLException;

    List<VariableTransformMethod> getEnabledMethods() throws SQLException;
} 