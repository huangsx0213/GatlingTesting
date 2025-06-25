package com.qa.app.model.reports;

import java.util.List;

public class ModeGroup {
    private TestMode mode;
    private List<CaseReport> cases;

    // Getters and Setters
    public TestMode getMode() {
        return mode;
    }

    public void setMode(TestMode mode) {
        this.mode = mode;
    }

    public List<CaseReport> getCases() {
        return cases;
    }

    public void setCases(List<CaseReport> cases) {
        this.cases = cases;
    }
} 