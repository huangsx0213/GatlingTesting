package com.qa.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.dao.api.IGroovyVariableDao;
import com.qa.app.dao.impl.GroovyVariableDaoImpl;
import com.qa.app.util.GroovyVariable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VariableService {

    private final ObjectMapper objectMapper;
    private final IGroovyVariableDao groovyVariableDao;

    public VariableService() {
        this.objectMapper = new ObjectMapper();
        this.groovyVariableDao = new GroovyVariableDaoImpl();
    }

    public List<GroovyVariable> loadVariables() {
        try {
            List<com.qa.app.model.GroovyVariable> dbVariables = groovyVariableDao.getAll();
            return dbVariables.stream()
                    .map(this::convertModelToUtil)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            System.err.println("Error loading variables from database");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveVariables(List<GroovyVariable> variables) {
        try {
            // This is a simple but potentially destructive way to "sync"
            // It matches the old file-overwrite logic.
            groovyVariableDao.deleteAll();
            for (GroovyVariable utilVar : variables) {
                com.qa.app.model.GroovyVariable modelVar = convertUtilToModel(utilVar);
                groovyVariableDao.add(modelVar);
            }
        } catch (SQLException | JsonProcessingException e) {
            System.err.println("Error saving variables to database");
            e.printStackTrace();
        }
    }

    private GroovyVariable convertModelToUtil(com.qa.app.model.GroovyVariable modelVar) {
        try {
            Map<String, String> valueMap = objectMapper.readValue(modelVar.getValue(), new TypeReference<>() {});
            GroovyVariable utilVar = new GroovyVariable(
                    modelVar.getId(),
                    modelVar.getName(),
                    valueMap.get("format"),
                    valueMap.get("description"),
                    valueMap.get("groovyScript")
            );
            return utilVar;
        } catch (IOException e) {
            System.err.println("Error parsing variable value for " + modelVar.getName());
            // Return a "broken" representation
            return new GroovyVariable(modelVar.getId(), modelVar.getName(), "ERROR", "Could not parse data", e.getMessage());
        }
    }

    private com.qa.app.model.GroovyVariable convertUtilToModel(GroovyVariable utilVar) throws JsonProcessingException {
        Map<String, String> valueMap = Map.of(
                "format", utilVar.getFormat(),
                "description", utilVar.getDescription(),
                "groovyScript", utilVar.getGroovyScript()
        );
        String valueJson = objectMapper.writeValueAsString(valueMap);

        com.qa.app.model.GroovyVariable modelVar = new com.qa.app.model.GroovyVariable();
        modelVar.setName(utilVar.getName());
        modelVar.setValue(valueJson);
        // 直接使用id
        modelVar.setId(utilVar.getId());
        return modelVar;
    }

    public void addVariable(GroovyVariable variable) {
        try {
            com.qa.app.model.GroovyVariable modelVar = convertUtilToModel(variable);
            groovyVariableDao.add(modelVar);
        } catch (Exception e) {
            System.err.println("Error adding variable");
            e.printStackTrace();
        }
    }

    public void updateVariable(GroovyVariable variable) {
        try {
            com.qa.app.model.GroovyVariable modelVar = convertUtilToModel(variable);
            groovyVariableDao.update(modelVar);
        } catch (Exception e) {
            System.err.println("Error updating variable");
            e.printStackTrace();
        }
    }

    public void deleteVariable(Integer id) {
        try {
            groovyVariableDao.delete(id);
        } catch (Exception e) {
            System.err.println("Error deleting variable");
            e.printStackTrace();
        }
    }
} 