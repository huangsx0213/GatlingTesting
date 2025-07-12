package com.qa.app.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BodyTemplate {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty content = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final IntegerProperty projectId = new SimpleIntegerProperty();

    public BodyTemplate() {
    }

    public BodyTemplate(String name, String content, String description, Integer projectId) {
        this.name.set(name);
        this.content.set(content);
        this.description.set(description);
        if (projectId != null) {
            this.projectId.set(projectId);
        }
    }

    public BodyTemplate(int id, String name, String content, String description, Integer projectId) {
        this.id.set(id);
        this.name.set(name);
        this.content.set(content);
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

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getContent() {
        return content.get();
    }

    public void setContent(String content) {
        this.content.set(content);
    }

    public StringProperty contentProperty() {
        return content;
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

    @Override
    public String toString() {
        return name.get(); // Display template name in ComboBox
    }
} 