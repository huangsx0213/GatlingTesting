package com.qa.app.ui.vm.gatling;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.qa.app.model.ConditionRow;

public class TestCondictionHandler {
    private final ObservableList<ConditionRow> conditionRows = FXCollections.observableArrayList();

    public ObservableList<ConditionRow> getConditionRows() {
        return conditionRows;
    }

    public void addCondition(String prefix) {
        conditionRows.add(new ConditionRow(prefix, FXCollections.observableArrayList()));
    }

    public void removeCondition(int idx) {
        if (idx >= 0 && idx < conditionRows.size()) {
            conditionRows.remove(idx);
        }
    }

    public String serializeConditions() {
        StringBuilder sb = new StringBuilder();
        for (ConditionRow row : conditionRows) {
            if (row.getPrefix() != null && row.getTcids() != null && !row.getPrefix().isEmpty() && !row.getTcids().isEmpty()) {
                sb.append("[")
                  .append(row.getPrefix())
                  .append("]")
                  .append(String.join(",", row.getTcids()))
                  .append(";");
            }
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1); // remove last semicolon
        return sb.toString();
    }

    public void deserializeConditions(String conditions) {
        conditionRows.clear();
        if (conditions != null && !conditions.isEmpty()) {
            String[] items = conditions.split(";");
            for (String item : items) {
                int openIdx = item.indexOf("[");
                int closeIdx = item.indexOf("]");
                if (openIdx == 0 && closeIdx > openIdx) {
                    String prefix = item.substring(openIdx + 1, closeIdx);
                    String tcidStr = item.substring(closeIdx + 1);
                    ObservableList<String> tcidList = FXCollections.observableArrayList();
                    if (!tcidStr.isEmpty()) {
                        for (String t : tcidStr.split(",")) {
                            if (!t.isEmpty()) tcidList.add(t);
                        }
                    }
                    conditionRows.add(new ConditionRow(prefix, tcidList));
                }
            }
        }
    }
} 