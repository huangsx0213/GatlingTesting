package com.qa.app.service.impl;

import java.sql.SQLException;
import java.util.List;

import com.qa.app.dao.api.IEndpointDao;
import com.qa.app.dao.impl.EndpointDaoImpl;
import com.qa.app.model.Endpoint;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IEndpointService;

public class EndpointServiceImpl implements IEndpointService {
    private final IEndpointDao dao = new EndpointDaoImpl();

    @Override
    public void addEndpoint(Endpoint endpoint) throws ServiceException {
        try {
            if (endpoint == null || endpoint.getName() == null || endpoint.getName().trim().isEmpty()) {
                throw new ServiceException("Endpoint name is required.");
            }
            if (dao.getEndpointByName(endpoint.getName()) != null) {
                throw new ServiceException("Endpoint with this name already exists.");
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
} 