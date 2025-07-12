package com.qa.app.service.api;

import com.qa.app.model.DbConnection;

import java.util.List;

public interface IDbConnectionService {
    DbConnection get(Long id);
    List<DbConnection> getConnectionsByProject(Integer projectId);
    DbConnection findByAlias(String alias);
    void addConnection(DbConnection connection);
    void updateConnection(DbConnection connection);
    void deleteConnection(DbConnection connection);
} 