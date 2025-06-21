package com.qa.app.ui.vm;

import com.qa.app.model.GatlingTest;
import com.qa.app.model.Scenario;
import com.qa.app.model.ScenarioStep;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGatlingScenarioService;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.impl.GatlingScenarioServiceImpl;
import com.qa.app.service.impl.GatlingTestServiceImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ScenarioViewModel {

    // -------------- FXML components --------------
    @FXML private TextField scenarioNameField;
    @FXML private TextArea scenarioDescArea;
    @FXML private TableView<GatlingTest> availableTestTable;
    @FXML private TableView<ScenarioStep> scenarioStepTable;
    @FXML private TableView<Scenario> scenarioTable;

    @FXML private ComboBox<String> suiteFilterCombo;
    @FXML private TextField tagFilterField;

    @FXML private TableColumn<GatlingTest, String> tcidCol;
    @FXML private TableColumn<GatlingTest, String> suiteCol;
    @FXML private TableColumn<GatlingTest, String> availableTagsCol;
    @FXML private TableColumn<GatlingTest, String> availableDescCol;

    @FXML private TableColumn<ScenarioStep, Number> orderCol;
    @FXML private TableColumn<ScenarioStep, String> stepTcidCol;
    @FXML private TableColumn<ScenarioStep, Number> waitCol;
    @FXML private TableColumn<ScenarioStep, String> stepTagsCol;

    @FXML private TableColumn<Scenario, String> scNameCol;
    @FXML private TableColumn<Scenario, String> scDescCol;

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

    public void setMainViewModel(MainViewModel vm) {
        this.mainViewModel = vm;
    }

    @FXML
    public void initialize() {
        // bind table data
        availableTestTable.setItems(availableTests);
        scenarioStepTable.setItems(steps);
        scenarioTable.setItems(scenarios);

        // set column binding
        tcidCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTcid()));
        suiteCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getSuite()));
        availableTagsCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTags()));
        availableDescCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDescriptions()));

        orderCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getOrder()));
        stepTcidCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTestTcid()));
        waitCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getWaitTime()));
        stepTagsCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getTags()));

        scNameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
        scDescCol.setCellValueFactory(cell -> cell.getValue().descriptionProperty());

        // table selection listener same as endpoint
        scenarioTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> showScenarioDetails(newSel));

        // init combos
        frequencyCombo.setItems(FXCollections.observableArrayList("Once", "Daily", "Weekly"));
        threadGroupCombo.setItems(FXCollections.observableArrayList("Standard", "Stepping", "Ultimate"));

        // init time spinners
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        secondSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        reloadTests();
        reloadScenarios();

        updateSuiteFilterOptions();
    }

    private void updateSuiteFilterOptions() {
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
            availableTests.addAll(testService.findAllTests());
            updateSuiteFilterOptions();
        } catch (ServiceException e) {
            showError("load test cases failed: " + e.getMessage());
        }
    }

    private void reloadScenarios() {
        try {
            scenarios.clear();
            scenarios.addAll(scenarioService.findAllScenarios());
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
        ScenarioStep step = new ScenarioStep(steps.size() + 1, selected.getTcid(), 0, selected.getTags());
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
        startDatePicker.setValue(null);
        frequencyCombo.getSelectionModel().clearSelection();
        threadGroupCombo.getSelectionModel().clearSelection();
    }

    // TODO: schedule/thread group dialog
    @FXML private void openLoadDialog(ActionEvent evt) {
        showInfo("this feature is not implemented yet");
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
        sc.setThreadGroupJson("{}"); // default empty JSON
        sc.setScheduleJson("{}");
        try {
            scenarioService.createScenario(sc, new ArrayList<>(steps));
            scenarios.add(0, sc);
            showInfo("scenario created successfully");
            reloadScenarios();
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleSaveScenario(ActionEvent evt) {
        Scenario selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("please select a scenario to update"); return; }
        selected.setName(scenarioNameField.getText());
        selected.setDescription(scenarioDescArea.getText());
        try {
            scenarioService.updateScenario(selected, new ArrayList<>(steps));
            showInfo("save successfully");
            reloadScenarios();
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleDuplicateScenario(ActionEvent evt) {
        Scenario sel = scenarioTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            Scenario dup = scenarioService.duplicateScenario(sel.getId());
            scenarios.add(0, dup);
            showInfo("duplicate successfully");
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteScenario(ActionEvent evt) {
        Scenario sel = scenarioTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            scenarioService.deleteScenario(sel.getId());
            scenarios.remove(sel);
            showInfo("delete successfully");
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleRunScenario(ActionEvent evt) {
        Scenario sel = scenarioTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("please select a scenario"); return; }
        try {
            scenarioService.runScenario(sel.getId());
            showInfo("scenario started");
        } catch (ServiceException e) {
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
            showInfo("schedule saved");
        } catch (ServiceException e) {
            showError(e.getMessage());
        }
    }

    // ----------- utility methods ------------
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }

    // 新增显示详情
    private void showScenarioDetails(Scenario sc) {
        if (sc == null) {
            scenarioNameField.clear();
            scenarioDescArea.clear();
            steps.clear();
            return;
        }
        scenarioNameField.setText(sc.getName());
        scenarioDescArea.setText(sc.getDescription());
        try {
            steps.setAll(scenarioService.findStepsByScenarioId(sc.getId()));
        } catch (ServiceException e) {
            steps.clear();
        }
    }
} 