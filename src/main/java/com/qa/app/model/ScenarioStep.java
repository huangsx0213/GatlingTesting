package com.qa.app.model;

/**
 * 场景步骤：序号、测试用例 TCID、等待时间、Tags。
 */
public class ScenarioStep {
    public static final String REMOVE_TOKEN = "__REMOVE__";
    private int order; // 步骤序号（从 1 开始）
    private String testTcid; // 对应 GatlingTest.tcid
    private int waitTime; // 本步骤执行完后的等待时间（秒）
    private String tags; // 可选标签

    private java.util.Map<String, String> headersVariableOverrides;
    private java.util.Map<String, String> bodyVariableOverrides;
    private java.util.List<ResponseCheck> responseCheckOverrides;

    public ScenarioStep() {
    }

    public ScenarioStep(int order, String testTcid, int waitTime, String tags) {
        this.order = order;
        this.testTcid = testTcid;
        this.waitTime = waitTime;
        this.tags = tags;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getTestTcid() {
        return testTcid;
    }

    public void setTestTcid(String testTcid) {
        this.testTcid = testTcid;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public java.util.Map<String, String> getBodyVariableOverrides() {
        return bodyVariableOverrides;
    }

    public void setBodyVariableOverrides(java.util.Map<String, String> bodyVariableOverrides) {
        this.bodyVariableOverrides = bodyVariableOverrides;
    }

    public java.util.Map<String, String> getHeadersVariableOverrides() {
        return headersVariableOverrides;
    }

    public void setHeadersVariableOverrides(java.util.Map<String, String> headersVariableOverrides) {
        this.headersVariableOverrides = headersVariableOverrides;
    }

    public java.util.List<ResponseCheck> getResponseCheckOverrides() {
        return responseCheckOverrides;
    }

    public void setResponseCheckOverrides(java.util.List<ResponseCheck> responseCheckOverrides) {
        this.responseCheckOverrides = responseCheckOverrides;
    }

    /** 检查任一的覆盖 */
    public boolean hasAnyOverrides() {
        return (bodyVariableOverrides != null && !bodyVariableOverrides.isEmpty()) ||
                (headersVariableOverrides != null && !headersVariableOverrides.isEmpty()) ||
                (responseCheckOverrides != null && !responseCheckOverrides.isEmpty());
    }

    /** 合并并应用到 Body 变更 */
    public java.util.Map<String, String> mergeBody(java.util.Map<String, String> original) {
        java.util.Map<String, String> merged = new java.util.LinkedHashMap<>();
        if (original != null)
            merged.putAll(original);
        if (bodyVariableOverrides != null) {
            for (var e : bodyVariableOverrides.entrySet()) {
                if (REMOVE_TOKEN.equals(e.getValue())) {
                    merged.remove(e.getKey());
                } else {
                    merged.put(e.getKey(), e.getValue());
                }
            }
        }
        return merged;
    }

    /** 合并并应用到 Headers 变更 */
    public java.util.Map<String, String> mergeHeaders(java.util.Map<String, String> original) {
        java.util.Map<String, String> merged = new java.util.LinkedHashMap<>();
        if (original != null)
            merged.putAll(original);
        if (headersVariableOverrides != null) {
            for (var e : headersVariableOverrides.entrySet()) {
                if (REMOVE_TOKEN.equals(e.getValue())) {
                    merged.remove(e.getKey());
                } else {
                    merged.put(e.getKey(), e.getValue());
                }
            }
        }
        return merged;
    }

    /** 清除所有覆盖 */
    public void clearOverrides() {
        this.bodyVariableOverrides = null;
        this.headersVariableOverrides = null;
        this.responseCheckOverrides = null;
    }

}