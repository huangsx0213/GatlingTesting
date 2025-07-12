package com.qa.app.dao.api;

import com.qa.app.model.DbConnection;

import java.util.List;

public interface IDbConnectionDao {
    void add(DbConnection connection);
    void update(DbConnection connection);
    void delete(DbConnection connection);
    DbConnection get(Long id);
    List<DbConnection> getByProject(Integer projectId);
    DbConnection getByAlias(String alias);
    List<DbConnection> findAll();
} 