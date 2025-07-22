package com.qa.app.ui.vm;

import com.qa.app.model.GatlingTest;
import com.qa.app.model.Scenario;
import com.qa.app.model.ScenarioStep;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGatlingScenarioService;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.impl.GatlingScenarioServiceImpl;
import com.qa.app.service.impl.GatlingTestServiceImpl;
import com.qa.app.ui.util.ClickableTooltipTableCell;
import com.qa.app.util.AppConfig;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import java.time.LocalTime;
import java.time.LocalDateTime;
import javafx.scene.control.Button;

import com.qa.app.common.listeners.AppConfigChangeListener;
import com.qa.app.service.ProjectContext;

public class GatlingScenarioViewModel implements AppConfigChangeListener {

    // -------------- FXML components --------------
    @FXML private TextField scenarioNameField;
    @FXML private TextArea scenarioDescArea;
    @FXML private TableView<GatlingTest> availableTestTable;
    @FXML private TableView<ScenarioStep> scenarioStepTable;
    @FXML private TableView<Scenario> scenarioTable;
    @FXML private CheckBox functionalTestCheckBox;
    @FXML private Label functionalHelpIcon;

    private javafx.scene.control.Tooltip functionalTooltip;

    @FXML private ComboBox<String> suiteFilterCombo;
    @FXML private TextField tagFilterField;

    @FXML private TableColumn<GatlingTest, String> tcidCol;
    @FXML private TableColumn<GatlingTest, String> suiteCol;
    @FXML private TableColumn<GatlingTest, String> availableTagsCol;
    @FXML private TableColumn<GatlingTest, String> availableDescCol;

    @FXML private TableColumn<ScenarioStep, Number> orderCol;
    @FXML private TableColumn<ScenarioStep, String> stepTcidCol;
    @FXML private TableColumn<ScenarioStep, Integer> waitCol;
    @FXML private TableColumn<ScenarioStep, String> stepTagsCol;

    @FXML private TableColumn<Scenario, String> scNameCol;
    @FXML private TableColumn<Scenario, String> scDescCol;
    @FXML private TableColumn<Scenario, String> scTypeCol;
    @FXML private TableColumn<Scenario, Number> scStepCountCol;
    @FXML private TableColumn<Scenario, String> functionalCol;  // New column for Functional Test flag

    @FXML private ComboBox<String> frequencyCombo;
    @FXML private ComboBox<String> threadGroupCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private Spinner<Integer> secondSpinner;

    // ----------------- data -------------------
    private final ObservableList<GatlingTest> availableTests = FXCollections.observableArrayList();
    private final ObservableList<ScenarioStep> steps = FXCollections.observableArrayList();
    private final ObservableList<Scenario> scenarios = FXCollections.observableArrayList();

    private final IGatlingTestService testService = new GatlingTestServiceImpl();
    private final IGatlingScenarioService scenarioService = new GatlingScenarioServiceImpl();

    // for MainViewModel to inject reference
    private MainViewModel mainViewModel;

    // ------------ Load Model Pane Controls -------------
    @FXML private TabPane loadModelTabPane;
    @FXML private Tab standardLoadTab;
    @FXML private Spinner<Integer> standardNumThreadsSpinner;
    @FXML private Spinner<Integer> standardRampUpSpinner;
    @FXML private Spinner<Integer> standardLoopsSpinner;
    @FXML private CheckBox standardSchedulerCheckBox;
    @FXML private Spinner<Integer> standardDurationSpinner;
    @FXML private Spinner<Integer> standardDelaySpinner;

    @FXML private Tab steppingLoadTab;
    @FXML private Spinner<Integer> steppingNumThreadsSpinner;
    @FXML private Spinner<Integer> steppingInitialDelaySpinner;
    @FXML private Spinner<Integer> steppingStartUsersSpinner;
    @FXML private Spinner<Integer> steppingIncrementUsersSpinner;
    @FXML private Spinner<Integer> steppingIncrementTimeSpinner;
    @FXML private Spinner<Integer> steppingHoldLoadSpinner;

    @FXML private Tab ultimateTab;
    @FXML private TableView<com.qa.app.model.threadgroups.UltimateThreadGroupStep> ultimateStepsTable;
    @FXML private TableColumn<com.qa.app.model.threadgroups.UltimateThreadGroupStep, Integer> ultimateStartTimeCol;
    @FXML private TableColumn<com.qa.app.model.threadgroups.UltimateThreadGroupStep, Integer> ultimateInitialLoadCol;
    @FXML private TableColumn<com.qa.app.model.threadgroups.UltimateThreadGroupStep, Integer> ultimateStartupTimeCol;
    @FXML private TableColumn<com.qa.app.model.threadgroups.UltimateThreadGroupStep, Integer> ultimateHoldTimeCol;
    @FXML private TableColumn<com.qa.app.model.threadgroups.UltimateThreadGroupStep, Integer> ultimateShutdownTimeCol;
    @FXML private Button addUltimateStepButton;
    @FXML private Button removeUltimateStepButton;

    private final ObservableList<com.qa.app.model.threadgroups.UltimateThreadGroupStep> ultimateSteps = FXCollections.observableArrayList();

    private final java.util.Map<Scenario, javafx.beans.property.BooleanProperty> selectionMap = new java.util.HashMap<>();
    private final javafx.scene.control.CheckBox selectAllCheckBoxSc = new javafx.scene.control.CheckBox();

    private boolean isUpdatingSelection = false;

    @FXML private Button runScenarioButton;
    @FXML private Button moveScenarioUpButton;
    @FXML private Button moveScenarioDownButton;

    public void setMainViewModel(MainViewModel vm) {
        this.mainViewModel = vm;
    }

    public void refresh() {
        reloadTests();
        reloadScenarios();
        updateSuiteFilterOptions();
    }

    @FXML
    public void initialize() {
        AppConfig.addChangeListener(this);
        // bind table data
        availableTestTable.setItems(availableTests);
        scenarioStepTable.setItems(steps);
        scenarioTable.setItems(scenarios);

        // set column binding
        tcidCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTcid()));
        suiteCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getSuite()));
        availableTagsCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTags()));
        availableDescCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDescriptions()));
        availableDescCol.setCellFactory(param -> new ClickableTooltipTableCell<>());

        orderCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getOrder()));
        stepTcidCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTestTcid()));
        waitCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getWaitTime()).asObject());
        waitCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        waitCol.setOnEditCommit(evt -> {
            ScenarioStep step = evt.getRowValue();
            Integer newVal = evt.getNewValue();
            if (newVal != null) {
                step.setWaitTime(newVal);
            }
        });
        stepTagsCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTags()));

        scNameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
        functionalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().isFunctionalTest() ? "Y" : "N"));
        scDescCol.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        scDescCol.setCellFactory(param -> new ClickableTooltipTableCell<>());

        // Type column shows load model type
        scTypeCol.setCellValueFactory(cell -> {
            String type="";
            try {
                String json = cell.getValue().getThreadGroupJson();
                if(json!=null && !json.isBlank()){
                    com.fasterxml.jackson.databind.ObjectMapper om=new com.fasterxml.jackson.databind.ObjectMapper();
                    com.qa.app.model.GatlingLoadParameters p = om.readValue(json, com.qa.app.model.GatlingLoadParameters.class);
                    if(p.getType()!=null) type = p.getType().name();
                }
            } catch(Exception ignore){}
            return new javafx.beans.property.SimpleStringProperty(type);
        });

        // Steps count column
        scStepCountCol.setCellValueFactory(cell -> {
            int count=0;
            try {
                count = scenarioService.findStepsByScenarioId(cell.getValue().getId()).size();
            } catch(Exception ignore){}
            return new javafx.beans.property.SimpleIntegerProperty(count);
        });

        // table selection listener same as endpoint
        scenarioTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> showScenarioDetails(newSel));

        // init combos
        frequencyCombo.setItems(FXCollections.observableArrayList("Once", "Daily", "Weekly"));
        if (threadGroupCombo != null) {
            threadGroupCombo.setItems(FXCollections.observableArrayList("Standard", "Stepping", "Ultimate"));
        }

        // init time spinners
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        secondSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        reloadTests();
        reloadScenarios();

        updateSuiteFilterOptions();

        // ----- Load Model Pane initialization -----
        initLoadModelPane();

        // 设置多选表格及复选框列
        scenarioTable.setEditable(true);
        scenarioTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        // ---- 添加选择列 ----
        javafx.scene.control.TableColumn<Scenario, Boolean> selectColumn = new javafx.scene.control.TableColumn<>();
        selectColumn.setGraphic(selectAllCheckBoxSc);
        selectColumn.setPrefWidth(40);
        selectColumn.setMinWidth(40);
        selectColumn.setMaxWidth(40);
        selectColumn.setSortable(false);
        selectColumn.setCellValueFactory(cd -> {
            Scenario sc = cd.getValue();
            return selectionMap.computeIfAbsent(sc, k -> new javafx.beans.property.SimpleBooleanProperty(false));
        });
        selectColumn.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(selectColumn));
        scenarioTable.getColumns().add(0, selectColumn);

        // Ensure functionalCol is added after scNameCol
        if (functionalCol != null) {
            int nameIndex = scenarioTable.getColumns().indexOf(scNameCol);
            if (nameIndex != -1) {
                scenarioTable.getColumns().add(nameIndex + 1, functionalCol);
            }
            functionalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().isFunctionalTest() ? "Y" : "N"));
        }

        // 监听表格多选变化以同步 selectionMap
        scenarioTable.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<Scenario>) c -> {
            if (isUpdatingSelection) return;
            isUpdatingSelection = true;
            syncSelectionProperties();
            updateSelectAllCheckBoxState();
            isUpdatingSelection = false;
        });

        // selectAll 逻辑
        selectAllCheckBoxSc.setOnAction(e -> {
            if (selectAllCheckBoxSc.isSelected()) {
                scenarioTable.getSelectionModel().selectAll();
            } else {
                scenarioTable.getSelectionModel().clearSelection();
            }
            scenarioTable.refresh();
        });

        // 使步骤表格可编辑，以便用户可双击修改等待时间
        if (scenarioStepTable != null) {
            scenarioStepTable.setEditable(true);
        }

        // --- Tooltip for Functional Test ---
        functionalTooltip = com.qa.app.ui.util.HelpTooltipManager.getFunctionalTestTooltip();
        if (functionalHelpIcon != null) {
            functionalHelpIcon.setOnMouseClicked(e -> toggleTooltip(functionalTooltip, functionalHelpIcon));
        }
    }

    @Override
    public void onConfigChanged() {
        reloadTests();
        reloadScenarios();
        updateSuiteFilterOptions();
    }

    private void updateSuiteFilterOptions() {
        if (availableTests.isEmpty()) {
            suiteFilterCombo.setItems(FXCollections.observableArrayList("All"));
            return;
        }
        List<String> suites = availableTests.stream()
                .map(GatlingTest::getSuite)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
        suites.add(0, "All");
        suiteFilterCombo.setItems(FXCollections.observableArrayList(suites));
    }

    private void reloadTests() {
        try {
            availableTests.clear();
            Integer projectId = ProjectContext.getCurrentProjectId();
            if (projectId != null) {
                availableTests.addAll(testService.findAllTestsByProjectId(projectId));
            }
            updateSuiteFilterOptions();
        } catch (ServiceException e) {
            showError("load test cases failed: " + e.getMessage());
        }
    }

    private void reloadScenarios() {
        try {
            selectionMap.clear();
            scenarios.clear();
            Integer projectId = ProjectContext.getCurrentProjectId();
            if (projectId != null) {
                scenarios.setAll(scenarioService.findAllScenarios(projectId));
            }
            for (Scenario sc : scenarios) {
                javafx.beans.property.BooleanProperty selected = new javafx.beans.property.SimpleBooleanProperty(false);
                selected.addListener((obs, wasSel, isSel) -> {
                    if (isUpdatingSelection) return;
                    if (isSel) {
                        scenarioTable.getSelectionModel().select(sc);
                    } else {
                        scenarioTable.getSelectionModel().clearSelection(scenarios.indexOf(sc));
                    }
                });
                selectionMap.put(sc, selected);
            }
            updateSelectAllCheckBoxState();
        } catch (ServiceException e) {
            showError("load scenarios failed: " + e.getMessage());
        }
    }

    // ------------------- event handling ----------------------
    @FXML
    private void handleFilterCases(ActionEvent evt) {
        String suite = suiteFilterCombo.getValue();
        String tagKw = tagFilterField.getText();
        reloadTests();
        availableTests.removeIf(t -> {
            boolean ok = true;
            if (suite != null && !suite.equals("All") && !suite.isBlank()) {
                ok = ok && suite.equals(t.getSuite());
            }
            if (ok && tagKw != null && !tagKw.isBlank()) {
                ok = t.getTags() != null && t.getTags().contains(tagKw);
            }
            return !ok;
        });
    }

    @FXML
    private void handleResetFilter(ActionEvent evt) {
        suiteFilterCombo.setValue("All");
        tagFilterField.clear();
        reloadTests();
    }

    @FXML
    private void handleAddToSteps(ActionEvent evt) {
        GatlingTest selected = availableTestTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ScenarioStep step = new ScenarioStep(steps.size() + 1, selected.getTcid(), selected.getWaitTime(), selected.getTags());
        steps.add(step);
    }

    @FXML
    private void handleRemoveFromSteps(ActionEvent evt) {
        ScenarioStep sel = scenarioStepTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            steps.remove(sel);
            reindexSteps();
        }
    }

    @FXML
    private void handleMoveStepUp(ActionEvent evt) {
        int idx = scenarioStepTable.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            ScenarioStep s = steps.remove(idx);
            steps.add(idx - 1, s);
            reindexSteps();
            scenarioStepTable.getSelectionModel().select(idx - 1);
        }
    }

    @FXML
    private void handleMoveStepDown(ActionEvent evt) {
        int idx = scenarioStepTable.getSelectionModel().getSelectedIndex();
        if (idx < steps.size() - 1 && idx >= 0) {
            ScenarioStep s = steps.remove(idx);
            steps.add(idx + 1, s);
            reindexSteps();
            scenarioStepTable.getSelectionModel().select(idx + 1);
        }
    }

    @FXML
    private void handleClearSteps(ActionEvent evt) {
        steps.clear();
    }

    @FXML
    private void handleMoveScenarioUp() {
        Scenario selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        int index = scenarios.indexOf(selected);
        if (index > 0) {
            java.util.Collections.swap(scenarios, index, index - 1);
            updateScenarioOrderAndPersist();
            scenarioTable.getSelectionModel().select(index - 1);
        }
    }

    @FXML
    private void handleMoveScenarioDown() {
        Scenario selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        int index = scenarios.indexOf(selected);
        if (index > -1 && index < scenarios.size() - 1) {
            java.util.Collections.swap(scenarios, index, index + 1);
            updateScenarioOrderAndPersist();
            scenarioTable.getSelectionModel().select(index + 1);
        }
    }

    private void updateScenarioOrderAndPersist() {
        try {
            for (int i = 0; i < scenarios.size(); i++) {
                scenarios.get(i).setDisplayOrder(i + 1);
            }
            scenarioService.updateOrder(new ArrayList<>(scenarios));
        } catch (ServiceException e) {
            showError("Failed to update scenario order: " + e.getMessage());
        }
    }

    private void reindexSteps() {
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).setOrder(i + 1);
        }
    }

    // clear form fields
    @FXML
    private void handleClearScenarioForm(ActionEvent evt) {
        clearScenarioForm();
    }

    private void clearScenarioForm() {
        scenarioNameField.clear();
        scenarioDescArea.clear();
        steps.clear();
        scenarioTable.getSelectionModel().clearSelection();

        // reset functional flag
        if (functionalTestCheckBox != null) functionalTestCheckBox.setSelected(false);

        startDatePicker.setValue(null);
        frequencyCombo.getSelectionModel().clearSelection();
        if (threadGroupCombo != null) threadGroupCombo.getSelectionModel().clearSelection();

        resetLoadModelDefaults();

        setCurrentTimeDefaults();
    }

    private void resetLoadModelDefaults() {
        // Create default parameter object based on model defaults
        com.qa.app.model.GatlingLoadParameters def = new com.qa.app.model.GatlingLoadParameters();
        def.setType(com.qa.app.model.threadgroups.ThreadGroupType.STANDARD);
        def.setStandardThreadGroup(new com.qa.app.model.threadgroups.StandardThreadGroup());
        def.setSteppingThreadGroup(new com.qa.app.model.threadgroups.SteppingThreadGroup());
        com.qa.app.model.threadgroups.UltimateThreadGroup ut = new com.qa.app.model.threadgroups.UltimateThreadGroup();
        java.util.List<com.qa.app.model.threadgroups.UltimateThreadGroupStep> stepsDef = new java.util.ArrayList<>();
        stepsDef.add(new com.qa.app.model.threadgroups.UltimateThreadGroupStep());
        ut.setSteps(stepsDef);
        def.setUltimateThreadGroup(ut);

        // apply to UI
        populateLoadModelFromParams(def);
    }

    @FXML private void openLoadDialog(ActionEvent evt) {
        showInfo("Load dialog feature is not implemented yet.");
    }

    @FXML
    private void handleAddScenario(ActionEvent evt) {
        String name = scenarioNameField.getText();
        if (name == null || name.isBlank()) {
            showError("scenario name cannot be empty");
            return;
        }
        Scenario sc = new Scenario();
        sc.setName(name);
        sc.setDescription(scenarioDescArea.getText());
        if (functionalTestCheckBox != null) {
            sc.setFunctionalTest(functionalTestCheckBox.isSelected());
        }
        Integer projectId = ProjectContext.getCurrentProjectId();
        if (projectId == null) {
            showError("No project selected!");
            return;
        }
        sc.setProjectId(projectId);
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            sc.setThreadGroupJson(om.writeValueAsString(buildLoadParameters()));
            sc.setScheduleJson(buildScheduleJson());
        } catch(Exception ex){ sc.setThreadGroupJson("{}"); }
        try {
            scenarioService.createScenario(sc, new ArrayList<>(steps));
            // save scheduler cron after id generated
            saveSchedulerToDb(sc.getId());
            scenarios.add(0, sc);
            javafx.beans.property.BooleanProperty selProp = new javafx.beans.property.SimpleBooleanProperty(false);
            selProp.addListener((obs, wasSel, isSel) -> {
                if (isUpdatingSelection) return;
                if (isSel) {
                    scenarioTable.getSelectionModel().select(sc);
                } else {
                    scenarioTable.getSelectionModel().clearSelection(scenarios.indexOf(sc));
                }
            });
            selectionMap.put(sc, selProp);
            showInfo("Scenario '" + sc.getName() + "' created successfully.");
            reloadScenarios();
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleSaveScenario(ActionEvent evt) {
        Scenario selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("please select a scenario to update"); return; }
        String name = scenarioNameField.getText();
        if (name == null || name.isBlank()) {
            showError("scenario name cannot be empty");
            return;
        }
        selected.setName(name);
        selected.setDescription(scenarioDescArea.getText());
        if (functionalTestCheckBox != null) {
            selected.setFunctionalTest(functionalTestCheckBox.isSelected());
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            selected.setThreadGroupJson(om.writeValueAsString(buildLoadParameters()));
            selected.setScheduleJson(buildScheduleJson());
            scenarioService.updateScenario(selected, new ArrayList<>(steps));
            // save scheduler cron after id generated
            saveSchedulerToDb(selected.getId());
            showInfo("Scenario '" + selected.getName() + "' saved successfully.");
            reloadScenarios();
        } catch (ServiceException se) {
            showError(se.getMessage());
        } catch (Exception ex){
            showError("save failed: "+ex.getMessage());
        }
    }

    @FXML
    private void handleDuplicateScenario(ActionEvent evt) {
        Scenario sel = scenarioTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            Scenario dup = scenarioService.duplicateScenario(sel.getId());
            scenarios.add(0, dup);
            javafx.beans.property.BooleanProperty dupSel = new javafx.beans.property.SimpleBooleanProperty(false);
            dupSel.addListener((obs, wasSel, isSel) -> {
                if (isUpdatingSelection) return;
                if (isSel) {
                    scenarioTable.getSelectionModel().select(dup);
                } else {
                    scenarioTable.getSelectionModel().clearSelection(scenarios.indexOf(dup));
                }
            });
            selectionMap.put(dup, dupSel);
            showInfo("Scenario '" + sel.getName() + "' duplicated successfully as '" + dup.getName() + "'.");
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteScenario(ActionEvent evt) {
        javafx.collections.ObservableList<Scenario> selectedScenarios = scenarioTable.getSelectionModel().getSelectedItems();
        if (selectedScenarios.isEmpty()) return;
        try {
            for (Scenario sc : new java.util.ArrayList<>(selectedScenarios)) {
                scenarioService.deleteScenario(sc.getId());
                selectionMap.remove(sc);
            }
            scenarios.removeAll(selectedScenarios);
            showInfo(selectedScenarios.size() + " scenario(s) deleted successfully.");
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleRunScenario(ActionEvent evt) {
        javafx.collections.ObservableList<Scenario> selected = scenarioTable.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showError("please select at least one scenario");
            return;
        }

        // Check for mixed functional test states
        if (selected.size() > 1) {
            boolean firstFunctional = selected.get(0).isFunctionalTest();
            boolean hasMixed = selected.stream().anyMatch(sc -> sc.isFunctionalTest() != firstFunctional);
            if (hasMixed) {
                showError("Cannot run mixed functional and non-functional scenarios together.");
                return;  // Cancel run
            }
        }

        // Disable the Run button to prevent duplicate clicks
        Button srcBtn;
        if (runScenarioButton != null) {
            srcBtn = runScenarioButton;
        } else if (evt.getSource() instanceof Button) {
            srcBtn = (Button) evt.getSource();
        } else {
            srcBtn = null;
        }
        if (srcBtn != null) srcBtn.setDisable(true);

        String runningMsg = "Running " + selected.size() + " scenario(s)...";
        if (mainViewModel != null) {
            mainViewModel.updateStatus(runningMsg, MainViewModel.StatusType.INFO);
        } else {
            com.qa.app.ui.vm.MainViewModel.showGlobalStatus(runningMsg, com.qa.app.ui.vm.MainViewModel.StatusType.INFO);
        }

        Runnable onComplete = () -> javafx.application.Platform.runLater(() -> {
            if (srcBtn != null) srcBtn.setDisable(false);
            reloadScenarios();
        });

        try {
            // Always call the multi-scenario API for simplicity
            scenarioService.runScenarios(new java.util.ArrayList<>(selected), onComplete);
        } catch (ServiceException e) {
            if (srcBtn != null) srcBtn.setDisable(false);
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleScheduleScenario(ActionEvent evt) {
        Scenario sel = scenarioTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("please select a scenario"); return; }

        LocalDate date = startDatePicker.getValue();
        String freq = frequencyCombo.getValue();
        if (date == null || freq == null) {
            showError("please select start time and frequency");
            return;
        }

        // simple cron mapping
        String cron = "";
        switch (freq) {
            case "Daily": cron = "0 0 0 * * ?"; break;
            case "Weekly": cron = "0 0 0 ? * MON"; break;
            case "Once": default: cron = ""; break;
        }

        int h = hourSpinner.getValue();
        int m = minuteSpinner.getValue();
        int s = secondSpinner.getValue();
        java.time.LocalDateTime startDateTime = date.atTime(h, m, s);

        try {
            scenarioService.upsertSchedule(sel.getId(), cron, true);
            // 更新 scenario.scheduleJson
            sel.setScheduleJson("{\"startDateTime\":\""+startDateTime.toString()+"\",\"frequency\":\""+freq+"\"}");
            scenarioService.updateScenario(sel, scenarioService.findStepsByScenarioId(sel.getId()));
            showInfo("Schedule saved for scenario '" + sel.getName() + "'.");
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    // ----------- utility methods ------------
    private void showError(String msg) {
        if (mainViewModel != null) {
            mainViewModel.updateStatus(msg, MainViewModel.StatusType.ERROR);
        } else {
            com.qa.app.ui.vm.MainViewModel.showGlobalStatus(msg, com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
        }
    }

    private void showInfo(String msg) {
        if (mainViewModel != null) {
            mainViewModel.updateStatus(msg, MainViewModel.StatusType.SUCCESS);
        } else {
            com.qa.app.ui.vm.MainViewModel.showGlobalStatus(msg, com.qa.app.ui.vm.MainViewModel.StatusType.SUCCESS);
        }
    }

    // 新增显示详情
    private void showScenarioDetails(Scenario sc) {
        if (sc == null) {
            scenarioNameField.clear();
            scenarioDescArea.clear();
            steps.clear();
            if (functionalTestCheckBox != null) functionalTestCheckBox.setSelected(false);
            return;
        }
        scenarioNameField.setText(sc.getName());
        scenarioDescArea.setText(sc.getDescription());
        try {
            steps.setAll(scenarioService.findStepsByScenarioId(sc.getId()));
        } catch (ServiceException e) {
            steps.clear();
        }
        // set functional checkbox state
        if (functionalTestCheckBox != null) {
            functionalTestCheckBox.setSelected(sc.isFunctionalTest());
        }
        try {
            if(sc.getThreadGroupJson()!=null && !sc.getThreadGroupJson().isBlank()){
               com.fasterxml.jackson.databind.ObjectMapper om=new com.fasterxml.jackson.databind.ObjectMapper();
               var p=om.readValue(sc.getThreadGroupJson(), com.qa.app.model.GatlingLoadParameters.class);
               populateLoadModelFromParams(p);
            }
        }catch(Exception ignore){}

        // populate schedule UI
        try{
            String schedJson=sc.getScheduleJson();
            if(schedJson!=null && !schedJson.isBlank()){
                com.fasterxml.jackson.databind.ObjectMapper om=new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String,Object> map = om.readValue(schedJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){});
                Object dtObj=map.get("startDateTime");
                if(dtObj!=null){
                    LocalDateTime ldt=LocalDateTime.parse(dtObj.toString());
                    startDatePicker.setValue(ldt.toLocalDate());
                    hourSpinner.getValueFactory().setValue(ldt.getHour());
                    minuteSpinner.getValueFactory().setValue(ldt.getMinute());
                    secondSpinner.getValueFactory().setValue(ldt.getSecond());
                }
                Object freqObj=map.get("frequency");
                if(freqObj!=null){
                    frequencyCombo.setValue(freqObj.toString());
                }
            }
        }catch(Exception ignore){}
    }

    private void initLoadModelPane() {
        // Ultimate table setup
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

            if (addUltimateStepButton != null)
                addUltimateStepButton.setOnAction(e -> ultimateSteps.add(new com.qa.app.model.threadgroups.UltimateThreadGroupStep()));
            if (removeUltimateStepButton != null)
                removeUltimateStepButton.setOnAction(e -> {
                    var sel = ultimateStepsTable.getSelectionModel().getSelectedItem();
                    if (sel != null) ultimateSteps.remove(sel);
                });
        }

        // Standard field bindings
        if (standardSchedulerCheckBox != null) {
            standardDurationSpinner.disableProperty().bind(standardSchedulerCheckBox.selectedProperty().not());
            standardDelaySpinner.disableProperty().bind(standardSchedulerCheckBox.selectedProperty().not());
            standardLoopsSpinner.disableProperty().bind(standardSchedulerCheckBox.selectedProperty());
        }
    }


    private com.qa.app.model.GatlingLoadParameters buildLoadParameters() {
        com.qa.app.model.GatlingLoadParameters params = new com.qa.app.model.GatlingLoadParameters();

        javafx.scene.control.Tab sel = loadModelTabPane != null ? loadModelTabPane.getSelectionModel().getSelectedItem() : null;
        int selIndex = loadModelTabPane != null ? loadModelTabPane.getSelectionModel().getSelectedIndex() : 0;

        if ((sel != null && sel == standardLoadTab) || selIndex==0) {
            params.setType(com.qa.app.model.threadgroups.ThreadGroupType.STANDARD);
            com.qa.app.model.threadgroups.StandardThreadGroup stdCfg = new com.qa.app.model.threadgroups.StandardThreadGroup();
            stdCfg.setNumThreads(standardNumThreadsSpinner.getValue());
            stdCfg.setRampUp(standardRampUpSpinner.getValue());
            stdCfg.setLoops(standardLoopsSpinner.getValue());
            stdCfg.setScheduler(standardSchedulerCheckBox.isSelected());
            stdCfg.setDuration(standardDurationSpinner.getValue());
            stdCfg.setDelay(standardDelaySpinner.getValue());
            params.setStandardThreadGroup(stdCfg);
        } else if ((sel != null && sel == steppingLoadTab) || selIndex==1) {
            params.setType(com.qa.app.model.threadgroups.ThreadGroupType.STEPPING);
            com.qa.app.model.threadgroups.SteppingThreadGroup stepCfg = new com.qa.app.model.threadgroups.SteppingThreadGroup();
            stepCfg.setNumThreads(steppingNumThreadsSpinner.getValue());
            stepCfg.setInitialDelay(steppingInitialDelaySpinner.getValue());
            stepCfg.setStartUsers(steppingStartUsersSpinner.getValue());
            stepCfg.setIncrementUsers(steppingIncrementUsersSpinner.getValue());
            stepCfg.setIncrementTime(steppingIncrementTimeSpinner.getValue());
            stepCfg.setHoldLoad(steppingHoldLoadSpinner.getValue());
            params.setSteppingThreadGroup(stepCfg);
        } else {
            params.setType(com.qa.app.model.threadgroups.ThreadGroupType.ULTIMATE);
            com.qa.app.model.threadgroups.UltimateThreadGroup ultCfg = new com.qa.app.model.threadgroups.UltimateThreadGroup();
            ultCfg.setSteps(new java.util.ArrayList<>(ultimateSteps));
            params.setUltimateThreadGroup(ultCfg);
        }
        return params;
    }

    private void populateLoadModelFromParams(com.qa.app.model.GatlingLoadParameters p){
        if (p == null) return;
        switch(p.getType()){
            case STANDARD -> {
                loadModelTabPane.getSelectionModel().select(standardLoadTab);
                var s=p.getStandardThreadGroup(); if(s==null) return;
                standardNumThreadsSpinner.getValueFactory().setValue(s.getNumThreads());
                standardRampUpSpinner.getValueFactory().setValue(s.getRampUp());
                standardLoopsSpinner.getValueFactory().setValue(s.getLoops());
                standardSchedulerCheckBox.setSelected(s.isScheduler());
                standardDurationSpinner.getValueFactory().setValue(s.getDuration());
                standardDelaySpinner.getValueFactory().setValue(s.getDelay());
            }
            case STEPPING -> {
                loadModelTabPane.getSelectionModel().select(steppingLoadTab);
                var st=p.getSteppingThreadGroup(); if(st==null) return;
                steppingNumThreadsSpinner.getValueFactory().setValue(st.getNumThreads());
                steppingInitialDelaySpinner.getValueFactory().setValue(st.getInitialDelay());
                steppingStartUsersSpinner.getValueFactory().setValue(st.getStartUsers());
                steppingIncrementUsersSpinner.getValueFactory().setValue(st.getIncrementUsers());
                steppingIncrementTimeSpinner.getValueFactory().setValue(st.getIncrementTime());
                steppingHoldLoadSpinner.getValueFactory().setValue(st.getHoldLoad());
            }
            case ULTIMATE -> {
                loadModelTabPane.getSelectionModel().select(ultimateTab);
                var ut=p.getUltimateThreadGroup(); if(ut==null) return;
                ultimateSteps.setAll(ut.getSteps());
            }
        }
    }

    private void setCurrentTimeDefaults() {
        java.time.LocalDate today = java.time.LocalDate.now();
        if(startDatePicker!=null) startDatePicker.setValue(today);
        LocalTime now = LocalTime.now();
        hourSpinner.getValueFactory().setValue(now.getHour());
        minuteSpinner.getValueFactory().setValue(now.getMinute());
        secondSpinner.getValueFactory().setValue(now.getSecond());
    }

    private void saveSchedulerToDb(int scenarioId){
        try{
            LocalDate date=startDatePicker.getValue();
            if(date==null) return;
            LocalTime t=LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue(), secondSpinner.getValue());
            String freq=frequencyCombo.getValue();
            String cron="";
            if("Daily".equals(freq)) cron="0 "+t.getMinute()+" "+t.getHour()+" * * ?";
            else if("Weekly".equals(freq)) cron="0 "+t.getMinute()+" "+t.getHour()+" ? * MON";
            // Once 无 cron
            scenarioService.upsertSchedule(scenarioId, cron, true);
            // update scheduleJson
        }catch(Exception ignore){}
    }

    private String buildScheduleJson(){
        java.util.Map<String,Object> map=new java.util.HashMap<>();
        if(startDatePicker.getValue()!=null){
           LocalTime lt=LocalTime.of(hourSpinner.getValue(),minuteSpinner.getValue(),secondSpinner.getValue());
           map.put("startDateTime",java.time.LocalDateTime.of(startDatePicker.getValue(),lt).toString());
        }
        map.put("frequency",frequencyCombo.getValue());
        try{
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        }catch(Exception e){return "{}";}
    }

    private void updateSelectAllCheckBoxState() {
        int selectedCount = scenarioTable.getSelectionModel().getSelectedItems().size();
        if (selectedCount == 0) {
            selectAllCheckBoxSc.setSelected(false);
            selectAllCheckBoxSc.setIndeterminate(false);
        } else if (selectedCount == scenarios.size() && !scenarios.isEmpty()) {
            selectAllCheckBoxSc.setSelected(true);
            selectAllCheckBoxSc.setIndeterminate(false);
        } else {
            selectAllCheckBoxSc.setIndeterminate(true);
        }
    }

    private void syncSelectionProperties() {
        isUpdatingSelection = true;
        for (Scenario sc : scenarios) {
            javafx.beans.property.BooleanProperty prop = selectionMap.get(sc);
            if (prop != null) {
                boolean shouldSelect = scenarioTable.getSelectionModel().getSelectedItems().contains(sc);
                if (prop.get() != shouldSelect) {
                    prop.set(shouldSelect);
                }
            }
        }
        isUpdatingSelection = false;
    }

    // Reuse toggle logic similar to GatlingTestViewModel
    private void toggleTooltip(javafx.scene.control.Tooltip tooltip, javafx.scene.Node owner) {
        if (tooltip == null || owner == null) return;
        if (tooltip.isShowing()) {
            tooltip.hide();
        } else {
            javafx.geometry.Point2D p = owner.localToScreen(owner.getBoundsInLocal().getMaxX(), owner.getBoundsInLocal().getMaxY());
            tooltip.show(owner, p.getX(), p.getY());
        }
    }
} 