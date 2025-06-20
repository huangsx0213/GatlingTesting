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

import com.qa.app.util.UserPreferences;

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

    // Dialog-level Save button
    @FXML
    private ButtonType saveButton;

    @FXML
    private DialogPane dialogPane;

    private final ObservableList<UltimateThreadGroupStep> ultimateSteps = FXCollections.observableArrayList();

    private UserPreferences prefs;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load user preferences
        prefs = UserPreferences.load();

        // Standard setup
        setupStandardTab();

        applyPreferencesToUI();

        // Ultimate Thread Group TableView setup
        setupUltimateTable();

        // Setup global Save button
        setupSaveButton();
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

    private void setupSaveButton() {
        if (dialogPane == null || saveButton == null) return;
        Button saveBtn = (Button) dialogPane.lookupButton(saveButton);
        if (saveBtn != null) {
            saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                // 阻止弹窗关闭
                e.consume();
                
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                if (selectedTab == standardLoadTab) {
                    StandardThreadGroup config = new StandardThreadGroup();
                    config.setNumThreads(standardNumThreadsSpinner.getValue());
                    config.setRampUp(standardRampUpSpinner.getValue());
                    config.setLoops(standardLoopsSpinner.getValue());
                    config.setScheduler(standardSchedulerCheckBox.isSelected());
                    config.setDuration(standardDurationSpinner.getValue());
                    config.setDelay(standardDelaySpinner.getValue());
                    prefs.standard = config;
                } else if (selectedTab == steppingLoadTab) {
                    SteppingThreadGroup config = new SteppingThreadGroup();
                    config.setNumThreads(steppingNumThreadsSpinner.getValue());
                    config.setInitialDelay(steppingInitialDelaySpinner.getValue());
                    config.setStartUsers(steppingStartUsersSpinner.getValue());
                    config.setIncrementUsers(steppingIncrementUsersSpinner.getValue());
                    config.setIncrementTime(steppingIncrementTimeSpinner.getValue());
                    config.setHoldLoad(steppingHoldLoadSpinner.getValue());
                    prefs.stepping = config;
                } else if (selectedTab == ultimateTab) {
                    UltimateThreadGroup config = new UltimateThreadGroup();
                    config.setSteps(new java.util.ArrayList<>(ultimateStepsTable.getItems()));
                    prefs.ultimate = config;
                }

                // Persist
                prefs.save();
            });
        }
    }

    private void applyPreferencesToUI() {
        // Standard
        StandardThreadGroup s = prefs.standard;
        standardNumThreadsSpinner.getValueFactory().setValue(s.getNumThreads());
        standardRampUpSpinner.getValueFactory().setValue(s.getRampUp());
        standardLoopsSpinner.getValueFactory().setValue(s.getLoops());
        standardSchedulerCheckBox.setSelected(s.isScheduler());
        standardDurationSpinner.getValueFactory().setValue(s.getDuration());
        standardDelaySpinner.getValueFactory().setValue(s.getDelay());

        // Stepping
        SteppingThreadGroup st = prefs.stepping;
        steppingNumThreadsSpinner.getValueFactory().setValue(st.getNumThreads());
        steppingInitialDelaySpinner.getValueFactory().setValue(st.getInitialDelay());
        steppingStartUsersSpinner.getValueFactory().setValue(st.getStartUsers());
        steppingIncrementUsersSpinner.getValueFactory().setValue(st.getIncrementUsers());
        steppingIncrementTimeSpinner.getValueFactory().setValue(st.getIncrementTime());
        steppingHoldLoadSpinner.getValueFactory().setValue(st.getHoldLoad());

        // Ultimate
        if (prefs.ultimate != null && prefs.ultimate.getSteps() != null) {
            ultimateSteps.setAll(prefs.ultimate.getSteps());
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

            // 保存到首选项
            prefs.standard = config;
        } else if (selectedTab == steppingLoadTab) {
            params.setType(ThreadGroupType.STEPPING);
            SteppingThreadGroup config = new SteppingThreadGroup();
            config.setNumThreads(steppingNumThreadsSpinner.getValue());
            config.setInitialDelay(steppingInitialDelaySpinner.getValue());
            config.setStartUsers(steppingStartUsersSpinner.getValue());
            config.setIncrementUsers(steppingIncrementUsersSpinner.getValue());
            config.setIncrementTime(steppingIncrementTimeSpinner.getValue());
            config.setHoldLoad(steppingHoldLoadSpinner.getValue());
            params.setSteppingThreadGroup(config);

            prefs.stepping = config;
        } else if (selectedTab == ultimateTab) {
            params.setType(ThreadGroupType.ULTIMATE);
            UltimateThreadGroup config = new UltimateThreadGroup();
            config.setSteps(new java.util.ArrayList<>(ultimateStepsTable.getItems()));
            params.setUltimateThreadGroup(config);
            // 保存到首选项
            prefs.ultimate = config;
        }

        // 持久化首选项
        prefs.save();

        return params;
    }
} 