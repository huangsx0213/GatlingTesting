package com.qa.app.model;

public class Endpoint {
    private int id;
    private String name;
    private String method;
    private String url;
    private Integer environmentId;
    private Integer projectId;

    // Order for UI display and persistence. Defaults to 0 meaning unspecified.
    private int displayOrder;

    public Endpoint() {}
    public Endpoint(int id, String name, String method, String url, Integer environmentId, Integer projectId) {
        this.id = id;
        this.name = name;
        this.method = method;
        this.url = url;
        this.environmentId = environmentId;
        this.projectId = projectId;
    }
    public Endpoint(String name, String method, String url, Integer environmentId, Integer projectId) {
        this.name = name;
        this.method = method;
        this.url = url;
        this.environmentId = environmentId;
        this.projectId = projectId;
        this.displayOrder = 0;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Integer getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(Integer environmentId) { this.environmentId = environmentId; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
} 