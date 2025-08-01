package com.qa.app.model;

public class GroovyVariable {
    private Integer id;
    private String name;
    private String value;
    private String description;
    private Integer environmentId;
    private Integer projectId;

    public GroovyVariable() {
    }

    public GroovyVariable(String name, String value, String description, Integer environmentId, Integer projectId) {
        this.name = name;
        this.value = value;
        this.description = description;
        this.environmentId = environmentId;
        this.projectId = projectId;
    }

    public GroovyVariable(Integer id, String name, String value, String description, Integer environmentId, Integer projectId) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.description = description;
        this.environmentId = environmentId;
        this.projectId = projectId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Integer environmentId) {
        this.environmentId = environmentId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }
} 