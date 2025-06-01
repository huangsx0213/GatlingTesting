package com.qa.app.service.api;

import com.qa.app.model.Environment;
import com.qa.app.service.ServiceException;
import java.util.List;

public interface IEnvironmentService {
    void createEnvironment(Environment environment) throws ServiceException;
    Environment findEnvironmentById(int id) throws ServiceException;
    Environment findEnvironmentByName(String name) throws ServiceException;
    List<Environment> findAllEnvironments() throws ServiceException;
    void updateEnvironment(Environment environment) throws ServiceException;
    void deleteEnvironment(int id) throws ServiceException;
} 