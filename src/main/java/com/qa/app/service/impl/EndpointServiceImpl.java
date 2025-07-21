package com.qa.app.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.qa.app.dao.api.IEndpointDao;
import com.qa.app.dao.api.IEnvironmentDao;
import com.qa.app.dao.impl.EndpointDaoImpl;
import com.qa.app.dao.impl.EnvironmentDaoImpl;
import com.qa.app.model.Endpoint;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IEndpointService;
import com.qa.app.service.util.VariableUtil;;

public class EndpointServiceImpl implements IEndpointService {
    private final IEndpointDao dao = new EndpointDaoImpl();
    private final IEnvironmentDao envDao = new EnvironmentDaoImpl();

    @Override
    public void addEndpoint(Endpoint endpoint) throws ServiceException {
        try {
            if (endpoint == null || endpoint.getName() == null || endpoint.getName().trim().isEmpty()) {
                throw new ServiceException("Endpoint name is required.");
            }
            // Strict validation for new endpoints
            validateEndpointVarsConsistency(endpoint);

            if (dao.getEndpointByNameAndEnv(endpoint.getName(), endpoint.getEnvironmentId()) != null) {
                throw new ServiceException("Endpoint with this name and environment already exists.");
            }
            dao.addEndpoint(endpoint);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateEndpoint(Endpoint endpoint) throws ServiceException {
        try {
            if (endpoint == null || endpoint.getId() <= 0) {
                throw new ServiceException("Endpoint ID is required.");
            }
            // Validation is now handled by the UI layer before calling update
            Endpoint exist = dao.getEndpointByNameAndEnv(endpoint.getName(), endpoint.getEnvironmentId());
            if (exist != null && exist.getId() != endpoint.getId()) {
                throw new ServiceException("Endpoint with this name and environment already exists.");
            }
            dao.updateEndpoint(endpoint);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteEndpoint(int id) throws ServiceException {
        try {
            dao.deleteEndpoint(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Endpoint getEndpointById(int id) throws ServiceException {
        try {
            return dao.getEndpointById(id);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Endpoint getEndpointByName(String name) throws ServiceException {
        try {
            return dao.getEndpointByName(name);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Endpoint> getAllEndpoints() throws ServiceException {
        try {
            return dao.getAllEndpoints();
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Endpoint getEndpointByNameAndEnv(String name, Integer environmentId) throws ServiceException {
        try {
            return dao.getEndpointByNameAndEnv(name, environmentId);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Endpoint> getEndpointsByProjectId(Integer projectId) throws ServiceException {
        try {
            return dao.getEndpointsByProjectId(projectId);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public String checkVariableConsistency(Endpoint endpoint) throws ServiceException {
        try {
            Set<String> newVars = VariableUtil.extractDynamicVars(endpoint.getUrl());
            List<Endpoint> sameNameEndpoints = dao.getEndpointsByName(endpoint.getName());

            for (Endpoint existing : sameNameEndpoints) {
                if (existing.getId() == endpoint.getId()) continue; // Skip self

                Set<String> existingVars = VariableUtil.extractDynamicVars(existing.getUrl());
                if (!newVars.equals(existingVars)) {
                    return "Dynamic variables do not match environment '" + envDao.getEnvironmentById(existing.getEnvironmentId()).getName() + "'.\n" +
                           "Existing: " + existingVars + "\n" +
                           "Proposed: " + newVars + "\n\nContinue with update?";
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Error during consistency check: " + e.getMessage(), e);
        }
        return null; // All consistent
    }

    private void validateEndpointVarsConsistency(Endpoint endpoint) throws ServiceException {
        String inconsistency = checkVariableConsistency(endpoint);
        if (inconsistency != null) {
            // Remove the "Continue with update?" part for the strict exception
            throw new ServiceException(inconsistency.split("\n\n")[0]);
        }
    }
} 