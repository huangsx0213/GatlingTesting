package com.qa.app.model.reports;

import java.util.List;

public class FunctionalTestReport {
    private String originTcid;
    private String suite;
    private String executedAt;
    private List<ModeGroup> groups;
    private boolean passed;

    // Getters and Setters
    public String getOriginTcid() {
        return originTcid;
    }

    public void setOriginTcid(String originTcid) {
        this.originTcid = originTcid;
    }

    public String getSuite() {
        return suite;
    }

    public void setSuite(String suite) {
        this.suite = suite;
    }

    public String getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(String executedAt) {
        this.executedAt = executedAt;
    }

    public List<ModeGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<ModeGroup> groups) {
        this.groups = groups;
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
} 