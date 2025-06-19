package com.qa.app.service.api;

import com.qa.app.service.ServiceException;
import com.qa.app.util.GroovyVariable;

import java.util.List;

public interface IVariableService {
    List<GroovyVariable> loadVariables() throws ServiceException;
    void saveVariables(List<GroovyVariable> variables) throws ServiceException;
    void addVariable(GroovyVariable variable) throws ServiceException;
    void updateVariable(GroovyVariable variable) throws ServiceException;
    void deleteVariable(Integer id) throws ServiceException;
} 