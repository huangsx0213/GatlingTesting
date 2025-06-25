package com.qa.app.model.reports;

import java.util.List;

public class RequestReport {
    private String requestName;
    private RequestInfo request;
    private ResponseInfo response;
    private List<CheckReport> checks;
    private boolean passed;
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Getters and Setters
    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getRequestName() {
        return requestName;
    }

    public void setRequestName(String requestName) {
        this.requestName = requestName;
    }

    public RequestInfo getRequest() {
        return request;
    }

    public void setRequest(RequestInfo request) {
        this.request = request;
    }

    public ResponseInfo getResponse() {
        return response;
    }

    public void setResponse(ResponseInfo response) {
        this.response = response;
    }

    public List<CheckReport> getChecks() {
        return checks;
    }

    public void setChecks(List<CheckReport> checks) {
        this.checks = checks;
    }
} 