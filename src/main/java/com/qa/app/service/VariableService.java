package com.qa.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qa.app.util.GroovyVariable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VariableService {

    private static final String VARIABLES_FILE_NAME = "custom-variables.groovy.json";

    private final ObjectMapper objectMapper;
    private final Path variablesFilePath;

    public VariableService() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        // This path is relative to the project root, suitable for development.
        this.variablesFilePath = Paths.get("src/main/resources", VARIABLES_FILE_NAME);
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            if (Files.notExists(variablesFilePath)) {
                Files.createDirectories(variablesFilePath.getParent());
                Files.createFile(variablesFilePath);
                // Write an empty JSON array to the new file
                objectMapper.writeValue(variablesFilePath.toFile(), new ArrayList<>());
            }
        } catch (IOException e) {
            System.err.println("FATAL: Could not create custom variables file at " + variablesFilePath);
            throw new RuntimeException("Could not initialize variables file", e);
        }
    }

    public List<GroovyVariable> loadVariables() {
        if (Files.notExists(variablesFilePath)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(variablesFilePath.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            System.err.println("Error loading variables from " + variablesFilePath);
            e.printStackTrace();
            // Return empty list to prevent application crash
            return new ArrayList<>();
        }
    }

    public void saveVariables(List<GroovyVariable> variables) {
        try {
            objectMapper.writeValue(variablesFilePath.toFile(), variables);
        } catch (IOException e) {
            System.err.println("Error saving variables to " + variablesFilePath);
            e.printStackTrace();
        }
    }

    public Path getVariablesFilePath() {
        return variablesFilePath;
    }
} 