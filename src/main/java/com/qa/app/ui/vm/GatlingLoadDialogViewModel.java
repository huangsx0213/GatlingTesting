package com.qa.app.ui.vm;

import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.threadgroups.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL;
import java.util.ResourceBundle;

public class GatlingLoadDialogViewModel implements Initializable {

    // Common
    @FXML
    private TabPane tabPane;

    // Standard
    @FXML
    private Tab standardLoadTab;
    @FXML
    private Spinner<Integer> standardNumThreadsSpinner;
    @FXML
    private Spinner<Integer> standardRampUpSpinner;
    @FXML
    private Spinner<Integer> standardLoopsSpinner;
    @FXML
    private CheckBox standardSchedulerCheckBox;
    @FXML
    private Spinner<Integer> standardDurationSpinner;
    @FXML
    private Spinner<Integer> standardDelaySpinner;

    // Stepping
    @FXML
    private Tab steppingLoadTab;
    @FXML
    private Spinner<Integer> steppingNumThreadsSpinner;
    @FXML
    private Spinner<Integer> steppingInitialDelaySpinner;
    @FXML
    private Spinner<Integer> steppingStartUsersSpinner;
    @FXML
    private Spinner<Integer> steppingIncrementUsersSpinner;
    @FXML
    private Spinner<Integer> steppingIncrementTimeSpinner;
    @FXML
    private Spinner<Integer> steppingHoldLoadSpinner;
    @FXML
    private Spinner<Integer> steppingThreadLifetimeSpinner;

    // Ultimate
    @FXML
    private Tab ultimateTab;
    @FXML
    private TableView<UltimateThreadGroupStep> ultimateStepsTable;
    @FXML
    private TableColumn<UltimateThreadGroupStep, Integer> ultimateStartTimeCol;
    @FXML
    private TableColumn<UltimateThreadGroupStep, Integer> ultimateInitialLoadCol;
    @FXML
    private TableColumn<UltimateThreadGroupStep, Integer> ultimateStartupTimeCol;
    @FXML
    private TableColumn<UltimateThreadGroupStep, Integer> ultimateHoldTimeCol;
    @FXML
    private TableColumn<UltimateThreadGroupStep, Integer> ultimateShutdownTimeCol;
    @FXML
    private Button addUltimateStepButton;
    @FXML
    private Button removeUltimateStepButton;

    private final ObservableList<UltimateThreadGroupStep> ultimateSteps = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Standard setup
        setupStandardTab();

        // Ultimate Thread Group TableView setup
        setupUltimateTable();
    }

    private void setupStandardTab() {
        standardDurationSpinner.disableProperty().bind(standardSchedulerCheckBox.selectedProperty().not());
        standardDelaySpinner.disableProperty().bind(standardSchedulerCheckBox.selectedProperty().not());
        standardLoopsSpinner.disableProperty().bind(standardSchedulerCheckBox.selectedProperty());
    }

    private void setupUltimateTable() {
        ultimateStepsTable.setItems(ultimateSteps);

        ultimateStartTimeCol.setCellValueFactory(cellData -> cellData.getValue().startTimeProperty().asObject());
        ultimateInitialLoadCol.setCellValueFactory(cellData -> cellData.getValue().initialLoadProperty().asObject());
        ultimateStartupTimeCol.setCellValueFactory(cellData -> cellData.getValue().startupTimeProperty().asObject());
        ultimateHoldTimeCol.setCellValueFactory(cellData -> cellData.getValue().holdTimeProperty().asObject());
        ultimateShutdownTimeCol.setCellValueFactory(cellData -> cellData.getValue().shutdownTimeProperty().asObject());

        ultimateStartTimeCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        ultimateInitialLoadCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        ultimateStartupTimeCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        ultimateHoldTimeCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        ultimateShutdownTimeCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        addUltimateStepButton.setOnAction(e -> ultimateSteps.add(new UltimateThreadGroupStep()));
        removeUltimateStepButton.setOnAction(e -> {
            UltimateThreadGroupStep selected = ultimateStepsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ultimateSteps.remove(selected);
            }
        });

        // Add a default step
        if (ultimateSteps.isEmpty()) {
            ultimateSteps.add(new UltimateThreadGroupStep());
        }
    }

    public GatlingLoadParameters getParameters() {
        GatlingLoadParameters params = new GatlingLoadParameters();
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        if (selectedTab == standardLoadTab) {
            params.setType(ThreadGroupType.STANDARD);
            StandardThreadGroup config = new StandardThreadGroup();
            config.setNumThreads(standardNumThreadsSpinner.getValue());
            config.setRampUp(standardRampUpSpinner.getValue());
            config.setLoops(standardLoopsSpinner.getValue());
            config.setScheduler(standardSchedulerCheckBox.isSelected());
            config.setDuration(standardDurationSpinner.getValue());
            config.setDelay(standardDelaySpinner.getValue());
            params.setStandardThreadGroup(config);
        } else if (selectedTab == steppingLoadTab) {
            params.setType(ThreadGroupType.STEPPING);
            SteppingThreadGroup config = new SteppingThreadGroup();
            config.setNumThreads(steppingNumThreadsSpinner.getValue());
            config.setInitialDelay(steppingInitialDelaySpinner.getValue());
            config.setStartUsers(steppingStartUsersSpinner.getValue());
            config.setIncrementUsers(steppingIncrementUsersSpinner.getValue());
            config.setIncrementTime(steppingIncrementTimeSpinner.getValue());
            config.setHoldLoad(steppingHoldLoadSpinner.getValue());
            config.setThreadLifetime(steppingThreadLifetimeSpinner.getValue());
            params.setSteppingThreadGroup(config);
        } else if (selectedTab == ultimateTab) {
            params.setType(ThreadGroupType.ULTIMATE);
            UltimateThreadGroup config = new UltimateThreadGroup();
            config.setSteps(new java.util.ArrayList<>(ultimateStepsTable.getItems()));
            params.setUltimateThreadGroup(config);
        }

        return params;
    }
} 