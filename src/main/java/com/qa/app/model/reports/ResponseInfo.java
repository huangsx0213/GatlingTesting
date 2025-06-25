package com.qa.app.model.reports;

import java.util.Map;

public class ResponseInfo {
    private int status;
    private Map<String, String> headers;
    private String bodySample;
    private long latencyMs;
    private long sizeBytes;

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBodySample() {
        return bodySample;
    }

    public void setBodySample(String bodySample) {
        this.bodySample = bodySample;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
} 