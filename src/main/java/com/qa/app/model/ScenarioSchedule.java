package com.qa.app.model;

/**
 * 场景调度配置，对应 scenario_schedule 表。
 */
public class ScenarioSchedule {
    private int scenarioId;
    private String cronExpr;    // Cron 表达式
    private String nextRunAt;   // 下次执行时间（ISO 字符串，便于展示）
    private boolean enabled;

    public ScenarioSchedule() {}

    public ScenarioSchedule(int scenarioId, String cronExpr, boolean enabled) {
        this.scenarioId = scenarioId;
        this.cronExpr = cronExpr;
        this.enabled = enabled;
    }

    public int getScenarioId() { return scenarioId; }
    public void setScenarioId(int scenarioId) { this.scenarioId = scenarioId; }

    public String getCronExpr() { return cronExpr; }
    public void setCronExpr(String cronExpr) { this.cronExpr = cronExpr; }

    public String getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(String nextRunAt) { this.nextRunAt = nextRunAt; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
} 