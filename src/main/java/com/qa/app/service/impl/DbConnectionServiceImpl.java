package com.qa.app.service.impl;

import com.qa.app.dao.api.IDbConnectionDao;
import com.qa.app.dao.impl.DbConnectionDaoImpl;
import com.qa.app.model.DbConnection;
import com.qa.app.service.api.IDbConnectionService;

import java.util.List;

public class DbConnectionServiceImpl implements IDbConnectionService {

    private final IDbConnectionDao dao;

    public DbConnectionServiceImpl() {
        this.dao = new DbConnectionDaoImpl();
    }

    @Override
    public DbConnection get(Long id) {
        return dao.get(id);
    }

    @Override
    public List<DbConnection> getConnectionsByProject(Integer projectId) {
        return dao.getByProject(projectId);
    }

    @Override
    public DbConnection findByAlias(String alias) {
        return dao.getByAlias(alias);
    }

    @Override
    public void addConnection(DbConnection connection) {
        dao.add(connection);
    }

    @Override
    public void updateConnection(DbConnection connection) {
        dao.update(connection);
    }

    @Override
    public void deleteConnection(DbConnection connection) {
        dao.delete(connection);
    }
} 