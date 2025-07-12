package com.qa.app.model;

import javafx.beans.property.*;

/**
 * 场景实体，包含线程组与调度设置序列化后的 JSON 字符串。
 */
public class Scenario {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty threadGroupJson = new SimpleStringProperty();
    private final StringProperty scheduleJson = new SimpleStringProperty();
    private final IntegerProperty projectId = new SimpleIntegerProperty();

    public Scenario() {}

    public Scenario(String name, String description) {
        this.name.set(name);
        this.description.set(description);
    }

    // ------------- Getters / Setters -----------------
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }

    public String getThreadGroupJson() { return threadGroupJson.get(); }
    public void setThreadGroupJson(String threadGroupJson) { this.threadGroupJson.set(threadGroupJson); }
    public StringProperty threadGroupJsonProperty() { return threadGroupJson; }

    public String getScheduleJson() { return scheduleJson.get(); }
    public void setScheduleJson(String scheduleJson) { this.scheduleJson.set(scheduleJson); }
    public StringProperty scheduleJsonProperty() { return scheduleJson; }

    public int getProjectId() { return projectId.get(); }
    public void setProjectId(int projectId) { this.projectId.set(projectId); }
    public IntegerProperty projectIdProperty() { return projectId; }

    @Override
    public String toString() {
        return "Scenario{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                '}';
    }
} 