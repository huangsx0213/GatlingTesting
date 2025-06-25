package com.qa.app.model.reports;

import java.util.List;

public class CaseReport {
    private String tcid;
    private List<RequestReport> items;
    private boolean passed;

    // Getters and Setters
    public String getTcid() {
        return tcid;
    }

    public void setTcid(String tcid) {
        this.tcid = tcid;
    }

    public List<RequestReport> getItems() {
        return items;
    }

    public void setItems(List<RequestReport> items) {
        this.items = items;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }
} 