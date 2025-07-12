package com.qa.app.model;

import java.util.List;
import java.util.Map;

public record ScenarioRunItem(
    Scenario scenario,
    GatlingLoadParameters params,
    List<Map<String, Object>> items
) {
} 