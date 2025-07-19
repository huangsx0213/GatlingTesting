package com.qa.app.service.impl;

import com.qa.app.dao.api.IDbConnectionDao;
import com.qa.app.dao.impl.DbConnectionDaoImpl;
import com.qa.app.model.DbConnection;
import com.qa.app.service.api.IDbConnectionService;

import java.util.List;
import java.util.stream.Collectors;

public class DbConnectionServiceImpl implements IDbConnectionService {

    private final IDbConnectionDao dbConnectionDao = new DbConnectionDaoImpl();

    @Override
    public List<DbConnection> getAll() {
        return dbConnectionDao.findAll();
    }
    
    @Override
    public List<String> getAllAliases() {
        return dbConnectionDao.findAll().stream()
                .map(DbConnection::getAlias)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public DbConnection findById(Long id) {
        return dbConnectionDao.get(id);
    }

    @Override
    public DbConnection findByAlias(String alias) {
        return dbConnectionDao.getByAlias(alias);
    }

    @Override
    public DbConnection findByAliasAndEnv(String alias, Integer environmentId) {
        return dbConnectionDao.getByAliasAndEnv(alias, environmentId);
    }

    @Override
    public void save(DbConnection dbConnection) {
        if (dbConnection.getId() == null) {
            dbConnectionDao.add(dbConnection);
        } else {
            dbConnectionDao.update(dbConnection);
        }
    }

    @Override
    public void addConnection(DbConnection connection) {
        // Validate unique alias within same environment
        if (connection.getAlias() != null) {
            DbConnection exist = dbConnectionDao.getByAliasAndEnv(connection.getAlias(), connection.getEnvironmentId());
            if (exist != null) {
                throw new RuntimeException("DbConnection with alias '" + connection.getAlias() + "' already exists in this environment.");
            }
        }
        dbConnectionDao.add(connection);
    }

    @Override
    public void updateConnection(DbConnection connection) {
        if (connection.getAlias() != null) {
            DbConnection exist = dbConnectionDao.getByAliasAndEnv(connection.getAlias(), connection.getEnvironmentId());
            if (exist != null && !exist.getId().equals(connection.getId())) {
                throw new RuntimeException("DbConnection with alias '" + connection.getAlias() + "' already exists in this environment.");
            }
        }
        dbConnectionDao.update(connection);
    }

    @Override
    public void deleteConnection(DbConnection connection) {
        dbConnectionDao.delete(connection);
    }
} 