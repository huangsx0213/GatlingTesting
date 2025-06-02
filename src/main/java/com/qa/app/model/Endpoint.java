package com.qa.app.model;

public class Endpoint {
    private int id;
    private String name;
    private String method;
    private String url;
    private Integer environmentId;

    public Endpoint() {}
    public Endpoint(int id, String name, String method, String url, Integer environmentId) {
        this.id = id;
        this.name = name;
        this.method = method;
        this.url = url;
        this.environmentId = environmentId;
    }
    public Endpoint(String name, String method, String url, Integer environmentId) {
        this.name = name;
        this.method = method;
        this.url = url;
        this.environmentId = environmentId;
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
} 