package com.qa.app.service.api;

import java.util.List;

import com.qa.app.model.Endpoint;
import com.qa.app.service.ServiceException;

public interface IEndpointService {
    void addEndpoint(Endpoint endpoint) throws ServiceException;
    void updateEndpoint(Endpoint endpoint) throws ServiceException;
    void deleteEndpoint(int id) throws ServiceException;
    Endpoint getEndpointById(int id) throws ServiceException;
    Endpoint getEndpointByName(String name) throws ServiceException;
    Endpoint getEndpointByNameAndEnv(String name, Integer environmentId) throws ServiceException;
    List<Endpoint> getAllEndpoints() throws ServiceException;
    List<Endpoint> getEndpointsByProjectId(Integer projectId) throws ServiceException;
    String checkVariableConsistency(Endpoint endpoint) throws ServiceException;

    void updateOrder(List<Endpoint> endpoints) throws ServiceException;
} 