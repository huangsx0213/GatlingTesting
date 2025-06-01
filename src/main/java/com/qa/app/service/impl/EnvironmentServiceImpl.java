package com.qa.app.service.impl;

import com.qa.app.dao.api.IEnvironmentDao;
import com.qa.app.dao.impl.EnvironmentDaoImpl;
import com.qa.app.model.Environment;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IEnvironmentService;

import java.sql.SQLException;
import java.util.List;

public class EnvironmentServiceImpl implements IEnvironmentService {
    private final IEnvironmentDao environmentDao = new EnvironmentDaoImpl();

    @Override
    public void createEnvironment(Environment environment) throws ServiceException {
        try {
            if (environment == null || environment.getName() == null || environment.getName().trim().isEmpty()) {
                throw new ServiceException("Environment name is required.");
            }
            if (environmentDao.getEnvironmentByName(environment.getName()) != null) {
                throw new ServiceException("Environment with name '" + environment.getName() + "' already exists.");
            }
            environmentDao.addEnvironment(environment);
        } catch (SQLException e) {
            throw new ServiceException("Database error while creating environment: " + e.getMessage(), e);
        }
    }

    @Override
    public Environment findEnvironmentById(int id) throws ServiceException {
        try {
            return environmentDao.getEnvironmentById(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error while finding environment by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Environment findEnvironmentByName(String name) throws ServiceException {
        try {
            return environmentDao.getEnvironmentByName(name);
        } catch (SQLException e) {
            throw new ServiceException("Database error while finding environment by name: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Environment> findAllEnvironments() throws ServiceException {
        try {
            return environmentDao.getAllEnvironments();
        } catch (SQLException e) {
            throw new ServiceException("Database error while retrieving all environments: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateEnvironment(Environment environment) throws ServiceException {
        try {
            if (environment == null || environment.getId() <= 0 || environment.getName() == null || environment.getName().trim().isEmpty()) {
                throw new ServiceException("Environment ID and name are required for update.");
            }
            Environment existingEnvironment = environmentDao.getEnvironmentById(environment.getId());
            if (existingEnvironment == null) {
                throw new ServiceException("Environment with ID " + environment.getId() + " not found.");
            }
            if (!existingEnvironment.getName().equals(environment.getName())) {
                if (environmentDao.getEnvironmentByName(environment.getName()) != null) {
                    throw new ServiceException("Environment with name '" + environment.getName() + "' already exists.");
                }
            }
            environmentDao.updateEnvironment(environment);
        } catch (SQLException e) {
            throw new ServiceException("Database error while updating environment: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteEnvironment(int id) throws ServiceException {
        try {
            if (environmentDao.getEnvironmentById(id) == null) {
                throw new ServiceException("Environment with ID " + id + " not found.");
            }
            environmentDao.deleteEnvironment(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error while deleting environment: " + e.getMessage(), e);
        }
    }
} 