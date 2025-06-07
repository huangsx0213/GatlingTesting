package com.qa.app.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Environment {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty projectId = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();

    public Environment() {}

    public Environment(String name, String description, Integer projectId) {
        this.name.set(name);
        this.description.set(description);
        if (projectId != null) {
            this.projectId.set(projectId);
        }
    }

    public Environment(int id, String name, String description, Integer projectId) {
        this.id.set(id);
        this.name.set(name);
        this.description.set(description);
        if (projectId != null) {
            this.projectId.set(projectId);
        }
    }

    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public Integer getProjectId() {
        return projectId.get();
    }

    public void setProjectId(Integer projectId) {
        if (projectId != null) {
            this.projectId.set(projectId);
        }
    }

    public IntegerProperty projectIdProperty() {
        return projectId;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    @Override
    public String toString() {
        return name.get();
    }
} 