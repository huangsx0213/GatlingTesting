package com.qa.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.dao.api.IGroovyVariableDao;
import com.qa.app.dao.impl.GroovyVariableDaoImpl;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IVariableService;
import com.qa.app.service.script.GroovyScriptEngine;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VariableServiceImpl implements IVariableService {

    private final ObjectMapper objectMapper;
    private final IGroovyVariableDao groovyVariableDao;

    public VariableServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.groovyVariableDao = new GroovyVariableDaoImpl();
    }

    @Override
    public List<GroovyScriptEngine> loadVariables() throws ServiceException {
        try {
            List<com.qa.app.model.GroovyVariable> dbVariables = groovyVariableDao.getAll();
            return dbVariables.stream()
                    .map(this::convertModelToEngine)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new ServiceException("Error loading variables from database", e);
        }
    }

    @Override
    public void saveVariables(List<GroovyScriptEngine> variables) throws ServiceException {
        try {
            groovyVariableDao.deleteAll();
            for (GroovyScriptEngine engineVar : variables) {
                com.qa.app.model.GroovyVariable modelVar = convertEngineToModel(engineVar);
                groovyVariableDao.add(modelVar);
            }
        } catch (SQLException | JsonProcessingException e) {
            throw new ServiceException("Error saving variables to database", e);
        }
    }

    @Override
    public void addVariable(GroovyScriptEngine variable) throws ServiceException {
        try {
            com.qa.app.model.GroovyVariable modelVar = convertEngineToModel(variable);
            groovyVariableDao.add(modelVar);
        } catch (SQLException | JsonProcessingException e) {
            throw new ServiceException("Error adding variable", e);
        }
    }

    @Override
    public void updateVariable(GroovyScriptEngine variable) throws ServiceException {
        try {
            com.qa.app.model.GroovyVariable modelVar = convertEngineToModel(variable);
            groovyVariableDao.update(modelVar);
        } catch (SQLException | JsonProcessingException e) {
            throw new ServiceException("Error updating variable", e);
        }
    }

    @Override
    public void deleteVariable(Integer id) throws ServiceException {
        try {
            groovyVariableDao.delete(id);
        } catch (SQLException e) {
            throw new ServiceException("Error deleting variable", e);
        }
    }

    private GroovyScriptEngine convertModelToEngine(com.qa.app.model.GroovyVariable modelVar) {
        try {
            Map<String, String> valueMap = objectMapper.readValue(modelVar.getValue(), new TypeReference<>() {});
            return new GroovyScriptEngine(
                    modelVar.getId(),
                    modelVar.getName(),
                    valueMap.get("format"),
                    valueMap.get("description"),
                    valueMap.get("groovyScript")
            );
        } catch (IOException e) {
            return new GroovyScriptEngine(modelVar.getId(), modelVar.getName(), "ERROR", "Could not parse data", e.getMessage());
        }
    }

    private com.qa.app.model.GroovyVariable convertEngineToModel(GroovyScriptEngine engineVar) throws JsonProcessingException {
        Map<String, String> valueMap = Map.of(
                "format", engineVar.getFormat(),
                "description", engineVar.getDescription(),
                "groovyScript", engineVar.getGroovyScript()
        );
        String valueJson = objectMapper.writeValueAsString(valueMap);

        com.qa.app.model.GroovyVariable modelVar = new com.qa.app.model.GroovyVariable();
        modelVar.setName(engineVar.getName());
        modelVar.setValue(valueJson);
        modelVar.setId(engineVar.getId());
        return modelVar;
    }
}