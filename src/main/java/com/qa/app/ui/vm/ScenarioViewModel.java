package com.qa.app.ui.vm;

import com.qa.app.model.GatlingTest;
import com.qa.app.model.Scenario;
import com.qa.app.model.ScenarioStep;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.impl.GatlingTestServiceImpl;
import com.qa.app.ui.vm.MainViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * ViewModel for Scenario Management page.
 * This is the first skeleton implementation focusing on UI binding.
 */
public class ScenarioViewModel implements Initializable {

    // ----- Scenario list (left panel) -----
    @FXML private TableView<Scenario> scenarioTable;
    @FXML private TableColumn<Scenario, String> scNameCol;
    @FXML private TableColumn<Scenario, String> scDescCol;

    // ----- Scenario details (center) -----
    @FXML private TextField scenarioNameField;
    @FXML private TextArea scenarioDescArea;
    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> frequencyCombo;
    @FXML private ComboBox<String> threadGroupCombo;

    // ----- Available tests -----
    @FXML private TableView<GatlingTest> availableTestTable;
    @FXML private TableColumn<GatlingTest, String> tcidCol;
    @FXML private TableColumn<GatlingTest, String> suiteCol;
    @FXML private TableColumn<GatlingTest, String> availableDescCol;
    @FXML private TableColumn<GatlingTest, String> availableTagsCol;

    // ----- Scenario steps -----
    @FXML private TableView<ScenarioStep> scenarioStepTable;
    @FXML private TableColumn<ScenarioStep, Number> orderCol;
    @FXML private TableColumn<ScenarioStep, String> stepTcidCol;
    @FXML private TableColumn<ScenarioStep, Number> waitCol;
    @FXML private TableColumn<ScenarioStep, String> stepTagsCol;

    @FXML private ComboBox<String> suiteFilterCombo;
    @FXML private TextField tagFilterField;

    // Buttons
    @FXML private Button addButton;
    @FXML private Button removeButton;
    @FXML private Button moveUpButton;
    @FXML private Button moveDownButton;

    private final ObservableList<Scenario> scenarios = FXCollections.observableArrayList();
    private final ObservableList<GatlingTest> availableTests = FXCollections.observableArrayList();
    private final ObservableList<ScenarioStep> steps = FXCollections.observableArrayList();

    private final IGatlingTestService testService = new GatlingTestServiceImpl();

    private MainViewModel mainViewModel;

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initScenarioTable();
        initAvailableTestTable();
        initScenarioStepTable();
        initComboBoxes();
        initFilterControls();
        loadAvailableTests();
        startDatePicker.setValue(LocalDate.now());
    }

    private void initScenarioTable() {
        scNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        scDescCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        scenarioTable.setItems(scenarios);
    }

    private void initAvailableTestTable() {
        tcidCol.setCellValueFactory(new PropertyValueFactory<>("tcid"));
        suiteCol.setCellValueFactory(new PropertyValueFactory<>("suite"));
        availableTagsCol.setCellValueFactory(new PropertyValueFactory<>("tags"));
        availableDescCol.setCellValueFactory(new PropertyValueFactory<>("descriptions"));
        availableDescCol.setCellFactory(col -> new TableCell<GatlingTest, String>() {
            private Tooltip tooltip = new Tooltip();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setTooltip(null);} else {
                    setText(item);
                    if (item.length() > 20) { tooltip.setText(item); setTooltip(tooltip);} else { setTooltip(null);} }
            }
        });
        availableTestTable.setItems(availableTests);
        availableTestTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void initScenarioStepTable() {
        orderCol.setCellValueFactory(param -> javafx.beans.binding.Bindings.createIntegerBinding(param.getValue()::getOrder));
        stepTcidCol.setCellValueFactory(new PropertyValueFactory<>("testTcid"));
        waitCol.setCellValueFactory(param -> javafx.beans.binding.Bindings.createIntegerBinding(param.getValue()::getWaitTime));
        stepTagsCol.setCellValueFactory(new PropertyValueFactory<>("tags"));
        scenarioStepTable.setItems(steps);
    }

    private void initComboBoxes() {
        frequencyCombo.setItems(FXCollections.observableArrayList("Once", "Daily", "Weekly"));
        threadGroupCombo.setItems(FXCollections.observableArrayList("Standard", "Stepping", "Ultimate"));
    }

    private void initFilterControls() {
        // Load suites into filter combo (simple distinct list)
        try {
            java.util.List<GatlingTest> all = testService.findAllTests();
            java.util.Set<String> suites = new java.util.HashSet<>();
            for (GatlingTest t : all) { suites.add(t.getSuite()); }
            suiteFilterCombo.setItems(FXCollections.observableArrayList(suites));
            suiteFilterCombo.getItems().add(0, "All");
            suiteFilterCombo.getSelectionModel().selectFirst();
        } catch (ServiceException ignored) {}
    }

    private void loadAvailableTests() {
        try {
            availableTests.setAll(testService.findAllTests());
        } catch (ServiceException e) {
            showError("Failed to load tests: " + e.getMessage());
        }
    }

    // ======================= Button handlers =======================
    @FXML private void handleAddScenario() {
        Scenario sc = new Scenario();
        sc.setName("New Scenario");
        sc.setDescription("");
        scenarios.add(sc);
        scenarioTable.getSelectionModel().select(sc);
    }

    @FXML private void handleDuplicateScenario() {
        Scenario selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Scenario copy = new Scenario();
        copy.setName(selected.getName() + " Copy");
        copy.setDescription(selected.getDescription());
        scenarios.add(copy);
        scenarioTable.getSelectionModel().select(copy);
    }

    @FXML private void handleDeleteScenario() {
        Scenario selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            scenarios.remove(selected);
        }
    }

    @FXML private void handleClearSteps() {
        steps.clear();
    }

    @FXML private void handleSaveScenario() {
        // TODO: persist scenario & steps via service layer
        showInfo("Scenario saved (stub)");
    }

    @FXML private void handleRunScenario() {
        // TODO: construct batch execution and call service
        showInfo("Run Now clicked (stub)");
    }

    @FXML private void handleScheduleScenario() {
        // TODO: save schedule configuration
        showInfo("Schedule clicked (stub)");
    }

    @FXML private void openLoadDialog() {
        // TODO: open existing Gatling load dialog
        showInfo("Open Load Dialog (stub)");
    }

    @FXML private void handleFilterCases() {
        String suite = suiteFilterCombo.getValue();
        String tagKeyword = tagFilterField.getText();
        try {
            java.util.List<GatlingTest> all = testService.findAllTests();
            java.util.List<GatlingTest> filtered = new java.util.ArrayList<>();
            for (GatlingTest t : all) {
                boolean match = true;
                if (suite != null && !"All".equals(suite) && !suite.equalsIgnoreCase(t.getSuite())) {
                    match = false;
                }
                if (match && tagKeyword != null && !tagKeyword.isBlank()) {
                    String tags = t.getTags() == null ? "" : t.getTags();
                    if (!tags.toLowerCase().contains(tagKeyword.toLowerCase())) {
                        match = false;
                    }
                }
                if (match) filtered.add(t);
            }
            availableTests.setAll(filtered);
        } catch (ServiceException e) { showError("Filter failed: " + e.getMessage()); }
    }

    @FXML private void handleResetFilter() {
        tagFilterField.clear();
        suiteFilterCombo.getSelectionModel().selectFirst();
        loadAvailableTests();
    }

    @FXML private void handleAddToSteps() {
        ObservableList<GatlingTest> selectedItems = availableTestTable.getSelectionModel().getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()) { showError("Select case(s) to add."); return; }
        for (GatlingTest t : selectedItems) {
            ScenarioStep step = new ScenarioStep(steps.size() + 1, t.getTcid(), 0, t.getTags());
            steps.add(step);
        }
        refreshStepOrder();
    }

    @FXML private void handleRemoveFromSteps() {
        ScenarioStep selected = scenarioStepTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select a step to remove."); return; }
        steps.remove(selected);
        refreshStepOrder();
    }

    @FXML private void handleMoveStepUp() {
        int idx = scenarioStepTable.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            java.util.Collections.swap(steps, idx, idx - 1);
            refreshStepOrder();
            scenarioStepTable.getSelectionModel().select(idx - 1);
        }
    }

    @FXML private void handleMoveStepDown() {
        int idx = scenarioStepTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < steps.size() - 1) {
            java.util.Collections.swap(steps, idx, idx + 1);
            refreshStepOrder();
            scenarioStepTable.getSelectionModel().select(idx + 1);
        }
    }

    private void refreshStepOrder() {
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).setOrder(i + 1);
        }
        scenarioStepTable.refresh();
    }

    // ======================= Helpers =======================
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }
} 