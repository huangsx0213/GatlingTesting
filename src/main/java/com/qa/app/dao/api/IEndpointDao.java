package com.qa.app.dao.api;

import java.sql.SQLException;
import java.util.List;

import com.qa.app.model.Endpoint;

public interface IEndpointDao {
    void addEndpoint(Endpoint endpoint) throws SQLException;
    void updateEndpoint(Endpoint endpoint) throws SQLException;
    void deleteEndpoint(int id) throws SQLException;
    Endpoint getEndpointById(int id) throws SQLException;
    Endpoint getEndpointByName(String name) throws SQLException;
    List<Endpoint> getAllEndpoints() throws SQLException;
} 