package com.qa.app.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BodyTemplate {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty content = new SimpleStringProperty();
    private final IntegerProperty environmentId = new SimpleIntegerProperty();

    public BodyTemplate() {
    }

    public BodyTemplate(String name, String content) {
        this.name.set(name);
        this.content.set(content);
    }

    public BodyTemplate(int id, String name, String content) {
        this.id.set(id);
        this.name.set(name);
        this.content.set(content);
    }

    public BodyTemplate(String name, String content, int environmentId) {
        this.name.set(name);
        this.content.set(content);
        this.environmentId.set(environmentId);
    }

    public BodyTemplate(int id, String name, String content, int environmentId) {
        this.id.set(id);
        this.name.set(name);
        this.content.set(content);
        this.environmentId.set(environmentId);
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

    public int getEnvironmentId() {
        return environmentId.get();
    }

    public void setEnvironmentId(int environmentId) {
        this.environmentId.set(environmentId);
    }

    public IntegerProperty environmentIdProperty() {
        return environmentId;
    }

    @Override
    public String toString() {
        return name.get(); // Display template name in ComboBox
    }
} 