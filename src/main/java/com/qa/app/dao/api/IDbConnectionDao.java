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
    /**
     * Fetch connection by alias within a specific environment.  If <code>environmentId</code> is null,
     * the DAO should return the record whose environment_id IS NULL.
     */
    DbConnection getByAliasAndEnv(String alias, Integer environmentId);
    List<DbConnection> findAll();
} 