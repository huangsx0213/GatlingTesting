package com.qa.app.model;

public class HeadersTemplate {
    private int id;
    private String name;
    private String content;
    private Integer environmentId;

    public HeadersTemplate() {}
    public HeadersTemplate(int id, String name, String content, Integer environmentId) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.environmentId = environmentId;
    }
    public HeadersTemplate(String name, String content, Integer environmentId) {
        this.name = name;
        this.content = content;
        this.environmentId = environmentId;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(Integer environmentId) { this.environmentId = environmentId; }
} 