package com.qa.app.service.impl;

import com.qa.app.dao.api.IGroovyVariableDao;
import com.qa.app.dao.impl.GroovyVariableDaoImpl;
import com.qa.app.model.GroovyVariable;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGroovyVariableService;

import java.sql.SQLException;
import java.util.List;

public class GroovyVariableServiceImpl implements IGroovyVariableService {

    private final IGroovyVariableDao groovyVariableDao = new GroovyVariableDaoImpl();

    @Override
    public void add(GroovyVariable variable) throws ServiceException {
        try {
            groovyVariableDao.add(variable);
        } catch (SQLException e) {
            throw new ServiceException("Error adding Groovy variable", e);
        }
    }

    @Override
    public void update(GroovyVariable variable) throws ServiceException {
        try {
            groovyVariableDao.update(variable);
        } catch (SQLException e) {
            throw new ServiceException("Error updating Groovy variable", e);
        }
    }

    @Override
    public void delete(int id) throws ServiceException {
        try {
            groovyVariableDao.delete(id);
        } catch (SQLException e) {
            throw new ServiceException("Error deleting Groovy variable", e);
        }
    }

    @Override
    public GroovyVariable getById(int id) throws ServiceException {
        try {
            return groovyVariableDao.getById(id);
        } catch (SQLException e) {
            throw new ServiceException("Error getting Groovy variable by id", e);
        }
    }

    @Override
    public List<GroovyVariable> getByProjectId(Integer projectId) throws ServiceException {
        try {
            return groovyVariableDao.getByProjectId(projectId);
        } catch (SQLException e) {
            throw new ServiceException("Error getting Groovy variables by project id", e);
        }
    }

    @Override
    public List<GroovyVariable> getAll() throws ServiceException {
        try {
            return groovyVariableDao.getAll();
        } catch (SQLException e) {
            throw new ServiceException("Error getting all Groovy variables", e);
        }
    }
} 