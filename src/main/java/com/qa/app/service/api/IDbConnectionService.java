package com.qa.app.service.api;

import com.qa.app.model.DbConnection;

import java.util.List;

public interface IDbConnectionService {
    List<DbConnection> getAll();
    List<String> getAllAliases();
    DbConnection findById(Long id);
    DbConnection findByAlias(String alias);
    DbConnection findByAliasAndEnv(String alias, Integer environmentId);
    void save(DbConnection dbConnection);
    void addConnection(DbConnection connection);
    void updateConnection(DbConnection connection);
    void deleteConnection(DbConnection connection);
} 