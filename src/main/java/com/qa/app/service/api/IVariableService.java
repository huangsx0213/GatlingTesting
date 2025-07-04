package com.qa.app.service.api;

import com.qa.app.service.ServiceException;
import com.qa.app.service.script.GroovyScriptEngine;

import java.util.List;

public interface IVariableService {
    List<GroovyScriptEngine> loadVariables() throws ServiceException;
    void saveVariables(List<GroovyScriptEngine> variables) throws ServiceException;
    void addVariable(GroovyScriptEngine variable) throws ServiceException;
    void updateVariable(GroovyScriptEngine variable) throws ServiceException;
    void deleteVariable(Integer id) throws ServiceException;
} 