package com.qa.app.model;

public class HeadersTemplate {
    private int id;
    private String name;
    private String content;
    private Integer projectId;

    public HeadersTemplate() {}
    public HeadersTemplate(int id, String name, String content, Integer projectId) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.projectId = projectId;
    }
    public HeadersTemplate(String name, String content, Integer projectId) {
        this.name = name;
        this.content = content;
        this.projectId = projectId;
    }
    public HeadersTemplate(String name, String content) {
        this.name = name;
        this.content = content;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
} 