package com.qa.app.service.api;

import com.qa.app.model.GroovyVariable;
import com.qa.app.service.ServiceException;

import java.util.List;

public interface IGroovyVariableService {
    void add(GroovyVariable variable) throws ServiceException;
    void update(GroovyVariable variable) throws ServiceException;
    void delete(int id) throws ServiceException;
    GroovyVariable getById(int id) throws ServiceException;
    List<GroovyVariable> getByProjectId(Integer projectId) throws ServiceException;
    List<GroovyVariable> getAll() throws ServiceException;
} 