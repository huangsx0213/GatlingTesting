package com.qa.app.model;

public class ScenarioStep {
    private int order;
    private String testTcid;
    private int waitTime;
    private String tags;

    public ScenarioStep() {}

    public ScenarioStep(int order, String testTcid, int waitTime, String tags) {
        this.order = order;
        this.testTcid = testTcid;
        this.waitTime = waitTime;
        this.tags = tags;
    }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public String getTestTcid() { return testTcid; }
    public void setTestTcid(String testTcid) { this.testTcid = testTcid; }

    public int getWaitTime() { return waitTime; }
    public void setWaitTime(int waitTime) { this.waitTime = waitTime; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
} 