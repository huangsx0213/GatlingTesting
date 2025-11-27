package com.qa.app.ui.vm.gatling;

import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.threadgroups.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

import java.util.ArrayList;

/**
 * Handler for Load Model configuration UI components.
 * Manages Standard, Stepping, and Ultimate thread group tabs.
 */
public class GatlingLoadModelHandler {

    private final TabPane loadModelTabPane;
    private final Tab standardLoadTab;
    private final Tab steppingLoadTab;
    private final Tab ultimateTab;

    // Standard Load controls
    private final Spinner<Integer> standardNumThreadsSpinner;
    private final Spinner<Integer> standardRampUpSpinner;
    private final Spinner<Integer> standardLoopsSpinner;
    private final CheckBox standardSchedulerCheckBox;
    private final Spinner<Integer> standardDurationSpinner;
    private final Spinner<Integer> standardDelaySpinner;

    // Stepping Load controls
    private final Spinner<Integer> steppingNumThreadsSpinner;
    private final Spinner<Integer> steppingInitialDelaySpinner;
    private final Spinner<Integer> steppingStartUsersSpinner;
    private final Spinner<Integer> steppingIncrementUsersSpinner;
    private final Spinner<Integer> steppingIncrementTimeSpinner;
    private final Spinner<Integer> steppingHoldLoadSpinner;

    // Ultimate Load controls
    private final TableView<UltimateThreadGroupStep> ultimateStepsTable;
    private final TableColumn<UltimateThreadGroupStep, Integer> ultimateStartTimeCol;
    private final TableColumn<UltimateThreadGroupStep, Integer> ultimateInitialLoadCol;
    private final TableColumn<UltimateThreadGroupStep, Integer> ultimateStartupTimeCol;
    private final TableColumn<UltimateThreadGroupStep, Integer> ultimateHoldTimeCol;
    private final TableColumn<UltimateThreadGroupStep, Integer> ultimateShutdownTimeCol;
    private final Button addUltimateStepButton;
    private final Button removeUltimateStepButton;

    private final ObservableList<UltimateThreadGroupStep> ultimateSteps = FXCollections.observableArrayList();

    public GatlingLoadModelHandler(
            TabPane loadModelTabPane,
            Tab standardLoadTab,
            Spinner<Integer> standardNumThreadsSpinner,
            Spinner<Integer> standardRampUpSpinner,
            Spinner<Integer> standardLoopsSpinner,
            CheckBox standardSchedulerCheckBox,
            Spinner<Integer> standardDurationSpinner,
            Spinner<Integer> standardDelaySpinner,
            Tab steppingLoadTab,
            Spinner<Integer> steppingNumThreadsSpinner,
            Spinner<Integer> steppingInitialDelaySpinner,
            Spinner<Integer> steppingStartUsersSpinner,
            Spinner<Integer> steppingIncrementUsersSpinner,
            Spinner<Integer> steppingIncrementTimeSpinner,
            Spinner<Integer> steppingHoldLoadSpinner,
            Tab ultimateTab,
            TableView<UltimateThreadGroupStep> ultimateStepsTable,
            TableColumn<UltimateThreadGroupStep, Integer> ultimateStartTimeCol,
            TableColumn<UltimateThreadGroupStep, Integer> ultimateInitialLoadCol,
            TableColumn<UltimateThreadGroupStep, Integer> ultimateStartupTimeCol,
            TableColumn<UltimateThreadGroupStep, Integer> ultimateHoldTimeCol,
            TableColumn<UltimateThreadGroupStep, Integer> ultimateShutdownTimeCol,
            Button addUltimateStepButton,
            Button removeUltimateStepButton) {
        this.loadModelTabPane = loadModelTabPane;
        this.standardLoadTab = standardLoadTab;
        this.standardNumThreadsSpinner = standardNumThreadsSpinner;
        this.standardRampUpSpinner = standardRampUpSpinner;
        this.standardLoopsSpinner = standardLoopsSpinner;
        this.standardSchedulerCheckBox = standardSchedulerCheckBox;
        this.standardDurationSpinner = standardDurationSpinner;
        this.standardDelaySpinner = standardDelaySpinner;
        this.steppingLoadTab = steppingLoadTab;
        this.steppingNumThreadsSpinner = steppingNumThreadsSpinner;
        this.steppingInitialDelaySpinner = steppingInitialDelaySpinner;
        this.steppingStartUsersSpinner = steppingStartUsersSpinner;
        this.steppingIncrementUsersSpinner = steppingIncrementUsersSpinner;
        this.steppingIncrementTimeSpinner = steppingIncrementTimeSpinner;
        this.steppingHoldLoadSpinner = steppingHoldLoadSpinner;
        this.ultimateTab = ultimateTab;
        this.ultimateStepsTable = ultimateStepsTable;
        this.ultimateStartTimeCol = ultimateStartTimeCol;
        this.ultimateInitialLoadCol = ultimateInitialLoadCol;
        this.ultimateStartupTimeCol = ultimateStartupTimeCol;
        this.ultimateHoldTimeCol = ultimateHoldTimeCol;
        this.ultimateShutdownTimeCol = ultimateShutdownTimeCol;
        this.addUltimateStepButton = addUltimateStepButton;
        this.removeUltimateStepButton = removeUltimateStepButton;
    }

    public void initialize() {
        initializeUltimateTable();
        initializeStandardBindings();
    }

    private void initializeUltimateTable() {
        if (ultimateStepsTable != null) {
            ultimateStepsTable.setItems(ultimateSteps);
            ultimateStartTimeCol.setCellValueFactory(cd -> cd.getValue().startTimeProperty().asObject());
            ultimateInitialLoadCol.setCellValueFactory(cd -> cd.getValue().initialLoadProperty().asObject());
            ultimateStartupTimeCol.setCellValueFactory(cd -> cd.getValue().startupTimeProperty().asObject());
            ultimateHoldTimeCol.setCellValueFactory(cd -> cd.getValue().holdTimeProperty().asObject());
            ultimateShutdownTimeCol.setCellValueFactory(cd -> cd.getValue().shutdownTimeProperty().asObject());

            ultimateStartTimeCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            ultimateInitialLoadCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            ultimateStartupTimeCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            ultimateHoldTimeCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            ultimateShutdownTimeCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

            if (addUltimateStepButton != null) {
                addUltimateStepButton.setOnAction(e -> ultimateSteps.add(new UltimateThreadGroupStep()));
            }
            if (removeUltimateStepButton != null) {
                removeUltimateStepButton.setOnAction(e -> {
                    UltimateThreadGroupStep sel = ultimateStepsTable.getSelectionModel().getSelectedItem();
                    if (sel != null)
                        ultimateSteps.remove(sel);
                });
            }
        }
    }

    private void initializeStandardBindings() {
        if (standardSchedulerCheckBox != null) {
            standardDurationSpinner.disableProperty().bind(standardSchedulerCheckBox.selectedProperty().not());
            standardDelaySpinner.disableProperty().bind(standardSchedulerCheckBox.selectedProperty().not());
            standardLoopsSpinner.disableProperty().bind(standardSchedulerCheckBox.selectedProperty());
        }
    }

    public GatlingLoadParameters buildLoadParameters() {
        GatlingLoadParameters params = new GatlingLoadParameters();

        Tab sel = loadModelTabPane != null ? loadModelTabPane.getSelectionModel().getSelectedItem() : null;
        int selIndex = loadModelTabPane != null ? loadModelTabPane.getSelectionModel().getSelectedIndex() : 0;

        if ((sel != null && sel == standardLoadTab) || selIndex == 0) {
            params.setType(ThreadGroupType.STANDARD);
            StandardThreadGroup stdCfg = new StandardThreadGroup();
            stdCfg.setNumThreads(standardNumThreadsSpinner.getValue());
            stdCfg.setRampUp(standardRampUpSpinner.getValue());
            stdCfg.setLoops(standardLoopsSpinner.getValue());
            stdCfg.setScheduler(standardSchedulerCheckBox.isSelected());
            stdCfg.setDuration(standardDurationSpinner.getValue());
            stdCfg.setDelay(standardDelaySpinner.getValue());
            params.setStandardThreadGroup(stdCfg);
        } else if ((sel != null && sel == steppingLoadTab) || selIndex == 1) {
            params.setType(ThreadGroupType.STEPPING);
            SteppingThreadGroup stepCfg = new SteppingThreadGroup();
            stepCfg.setNumThreads(steppingNumThreadsSpinner.getValue());
            stepCfg.setInitialDelay(steppingInitialDelaySpinner.getValue());
            stepCfg.setStartUsers(steppingStartUsersSpinner.getValue());
            stepCfg.setIncrementUsers(steppingIncrementUsersSpinner.getValue());
            stepCfg.setIncrementTime(steppingIncrementTimeSpinner.getValue());
            stepCfg.setHoldLoad(steppingHoldLoadSpinner.getValue());
            params.setSteppingThreadGroup(stepCfg);
        } else {
            params.setType(ThreadGroupType.ULTIMATE);
            UltimateThreadGroup ultCfg = new UltimateThreadGroup();
            ultCfg.setSteps(new ArrayList<>(ultimateSteps));
            params.setUltimateThreadGroup(ultCfg);
        }
        return params;
    }

    public void populateLoadModelFromParams(GatlingLoadParameters p) {
        if (p == null)
            return;
        switch (p.getType()) {
            case STANDARD -> {
                loadModelTabPane.getSelectionModel().select(standardLoadTab);
                StandardThreadGroup s = p.getStandardThreadGroup();
                if (s == null)
                    return;
                standardNumThreadsSpinner.getValueFactory().setValue(s.getNumThreads());
                standardRampUpSpinner.getValueFactory().setValue(s.getRampUp());
                standardLoopsSpinner.getValueFactory().setValue(s.getLoops());
                standardSchedulerCheckBox.setSelected(s.isScheduler());
                standardDurationSpinner.getValueFactory().setValue(s.getDuration());
                standardDelaySpinner.getValueFactory().setValue(s.getDelay());
            }
            case STEPPING -> {
                loadModelTabPane.getSelectionModel().select(steppingLoadTab);
                SteppingThreadGroup st = p.getSteppingThreadGroup();
                if (st == null)
                    return;
                steppingNumThreadsSpinner.getValueFactory().setValue(st.getNumThreads());
                steppingInitialDelaySpinner.getValueFactory().setValue(st.getInitialDelay());
                steppingStartUsersSpinner.getValueFactory().setValue(st.getStartUsers());
                steppingIncrementUsersSpinner.getValueFactory().setValue(st.getIncrementUsers());
                steppingIncrementTimeSpinner.getValueFactory().setValue(st.getIncrementTime());
                steppingHoldLoadSpinner.getValueFactory().setValue(st.getHoldLoad());
            }
            case ULTIMATE -> {
                loadModelTabPane.getSelectionModel().select(ultimateTab);
                UltimateThreadGroup ut = p.getUltimateThreadGroup();
                if (ut == null)
                    return;
                ultimateSteps.setAll(ut.getSteps());
            }
        }
    }

    public void resetToDefaults() {
        GatlingLoadParameters def = new GatlingLoadParameters();
        def.setType(ThreadGroupType.STANDARD);
        def.setStandardThreadGroup(new StandardThreadGroup());
        def.setSteppingThreadGroup(new SteppingThreadGroup());
        UltimateThreadGroup ut = new UltimateThreadGroup();
        ArrayList<UltimateThreadGroupStep> stepsDef = new ArrayList<>();
        stepsDef.add(new UltimateThreadGroupStep());
        ut.setSteps(stepsDef);
        def.setUltimateThreadGroup(ut);
        populateLoadModelFromParams(def);
    }
}
