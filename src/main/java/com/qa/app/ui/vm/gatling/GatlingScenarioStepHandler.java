package com.qa.app.ui.vm.gatling;

import com.qa.app.model.GatlingTest;
import com.qa.app.model.ScenarioStep;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.TableView;

/**
 * Handler for Scenario Step management (add/remove/reorder steps)
 */
public class GatlingScenarioStepHandler {

    private final ObservableList<ScenarioStep> steps = FXCollections.observableArrayList();
    private final TableView<ScenarioStep> scenarioStepTable;
    private final TableView<GatlingTest> availableTestTable;

    public GatlingScenarioStepHandler(
            TableView<ScenarioStep> scenarioStepTable,
            TableView<GatlingTest> availableTestTable) {
        this.scenarioStepTable = scenarioStepTable;
        this.availableTestTable = availableTestTable;
    }

    public ObservableList<ScenarioStep> getSteps() {
        return steps;
    }

    public void handleAddToSteps(ActionEvent evt) {
        GatlingTest selected = availableTestTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        ScenarioStep step = new ScenarioStep(steps.size() + 1, selected.getTcid(), selected.getWaitTime(),
                selected.getTags());
        steps.add(step);
    }

    public void handleRemoveFromSteps(ActionEvent evt) {
        ScenarioStep sel = scenarioStepTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            steps.remove(sel);
            reindexSteps();
        }
    }

    public void handleMoveStepUp(ActionEvent evt) {
        int idx = scenarioStepTable.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            ScenarioStep s = steps.remove(idx);
            steps.add(idx - 1, s);
            reindexSteps();
            scenarioStepTable.getSelectionModel().select(idx - 1);
        }
    }

    public void handleMoveStepDown(ActionEvent evt) {
        int idx = scenarioStepTable.getSelectionModel().getSelectedIndex();
        if (idx < steps.size() - 1 && idx >= 0) {
            ScenarioStep s = steps.remove(idx);
            steps.add(idx + 1, s);
            reindexSteps();
            scenarioStepTable.getSelectionModel().select(idx + 1);
        }
    }

    public void handleClearSteps(ActionEvent evt) {
        steps.clear();
    }

    private void reindexSteps() {
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).setOrder(i + 1);
        }
    }
}
