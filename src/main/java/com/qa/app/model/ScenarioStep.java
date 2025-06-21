package com.qa.app.model;

/**
 * 场景步骤：序号、测试用例 TCID、等待时间、Tags。
 */
public class ScenarioStep {
    private int order;          // 步骤序号（从 1 开始）
    private String testTcid;    // 对应 GatlingTest.tcid
    private int waitTime;       // 本步骤执行完后的等待时间（秒）
    private String tags;        // 可选标签

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