package com.qa.app.ui.vm;

import com.qa.app.model.*;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.*;
import com.qa.app.service.impl.*;
import com.qa.app.service.util.VariableGenerator;
import com.qa.app.ui.util.HelpTooltipManager;
import com.qa.app.ui.vm.gatling.*;
import com.qa.app.common.listeners.AppConfigChangeListener;
import com.qa.app.service.ProjectContext;
import com.qa.app.service.EnvironmentContext;
import com.qa.app.util.AppConfig;
import javafx.scene.web.HTMLEditor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import org.controlsfx.control.CheckComboBox;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GatlingTestViewModel implements Initializable, AppConfigChangeListener {

    @FXML
    private TextField testIdField;
    @FXML
    private CheckBox isEnabledCheckBox;
    @FXML
    private ComboBox<String> suiteComboBox;
    @FXML
    private TextField tcidField;
    @FXML
    private HTMLEditor descriptionsArea;

    // Conditions Table
    @FXML
    private TableView<ConditionRow> conditionsTable;
    @FXML
    private TableColumn<ConditionRow, String> prefixColumn;
    @FXML
    private TableColumn<ConditionRow, String> conditionTcidColumn;
    @FXML
    private Button addConditionButton;
    @FXML
    private Button removeConditionButton;

    @FXML
    private TextArea expResultArea; // Used for raw JSON view if needed, but primarily using ResponseCheckHandler
    @FXML
    private TextArea saveFieldsArea; // Legacy/Unused? Keeping for FXML compatibility

    // Endpoint & URL Vars
    @FXML
    private ComboBox<String> endpointComboBox;
    @FXML
    private TableView<DynamicVariable> urlDynamicVarsTable;
    @FXML
    private TableColumn<DynamicVariable, String> urlDynamicKeyColumn;
    @FXML
    private TableColumn<DynamicVariable, String> urlDynamicValueColumn;
    @FXML
    private TableColumn<DynamicVariable, Void> urlDynamicActionColumn;
    @FXML
    private TextArea generatedUrlArea;

    // Body Template
    @FXML
    private ComboBox<String> bodyTemplateComboBox;
    @FXML
    private TableView<DynamicVariable> bodyDynamicVarsTable;
    @FXML
    private TableColumn<DynamicVariable, String> bodyDynamicKeyColumn;
    @FXML
    private TableColumn<DynamicVariable, String> bodyDynamicValueColumn;
    @FXML
    private TableColumn<DynamicVariable, Void> bodyDynamicActionColumn;
    @FXML
    private TextArea generatedBodyArea;
    @FXML
    private Label bodyHelpIcon;

    // Headers Template
    @FXML
    private ComboBox<String> headersTemplateComboBox;
    @FXML
    private TableView<DynamicVariable> headersTemplateVarsTable;
    @FXML
    private TableColumn<DynamicVariable, String> headersTemplateKeyColumn;
    @FXML
    private TableColumn<DynamicVariable, String> headersTemplateValueColumn;
    @FXML
    private TableColumn<DynamicVariable, Void> headersTemplateActionColumn;
    @FXML
    private TextArea generatedHeadersArea;
    @FXML
    private Label headersHelpIcon;

    @FXML
    private TextField tagsInputField;
    @FXML
    private FlowPane tagsFlowPane;
    @FXML
    private Spinner<Integer> waitTimeSpinner;

    // Buttons
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button runTestButton;
    @FXML
    private Button viewReportButton;
    @FXML
    private Button moveUpButton;
    @FXML
    private Button moveDownButton;

    // Test Table
    @FXML
    private TableView<GatlingTest> testTable;
    @FXML
    private TableColumn<GatlingTest, Boolean> isEnabledColumn;
    @FXML
    private TableColumn<GatlingTest, String> suiteColumn;
    @FXML
    private TableColumn<GatlingTest, String> testTcidColumn;
    @FXML
    private TableColumn<GatlingTest, String> descriptionsColumn;
    @FXML
    private TableColumn<GatlingTest, String> endpointColumn;
    @FXML
    private TableColumn<GatlingTest, String> tagsColumn;
    @FXML
    private TableColumn<GatlingTest, Integer> waitTimeColumn;
    @FXML
    private TableColumn<GatlingTest, String> headersTemplateNameColumn;
    @FXML
    private TableColumn<GatlingTest, String> bodyTemplateNameColumn;

    // Response Checks
    @FXML
    private TableView<ResponseCheck> responseChecksTable;
    @FXML
    private TableColumn<ResponseCheck, CheckType> checkTypeColumn;
    @FXML
    private TableColumn<ResponseCheck, String> checkExpressionColumn;
    @FXML
    private TableColumn<ResponseCheck, Operator> checkOperatorColumn;
    @FXML
    private TableColumn<ResponseCheck, String> checkExpectColumn;
    @FXML
    private TableColumn<ResponseCheck, String> checkSaveAsColumn;
    @FXML
    private Label responseHelpIcon;
    @FXML
    private Button responseHelpButton;

    // Filters
    @FXML
    private ComboBox<String> suiteFilterCombo;
    @FXML
    private TextField tagFilterField;
    @FXML
    private ComboBox<String> enabledFilterCombo;

    @FXML
    private Label runHelpIcon;

    // Services
    private final IGatlingTestService testService = new GatlingTestServiceImpl();
    private final IBodyTemplateService bodyTemplateService = new BodyTemplateServiceImpl();
    private final IHeadersTemplateService headersTemplateService = new HeadersTemplateServiceImpl();
    private final IEndpointService endpointService = new EndpointServiceImpl();

    // Helpers
    private GatlingTestTableHelper tableHelper;
    private GatlingTestEndpointHandler endpointHandler;
    private GatlingTestResponseCheckHandler responseCheckHandler;

    private TemplateHandler bodyTemplateHandler;
    private TemplateHandler headersTemplateHandler;
    private final TestCondictionHandler conditionHandler = new TestCondictionHandler();
    private TagHandler tagHandler;

    // Data
    private final ObservableList<DynamicVariable> bodyTemplateVariables = FXCollections.observableArrayList();
    private final Map<String, String> bodyTemplates = new HashMap<>();
    private final Map<Integer, String> bodyTemplateIdNameMap = new HashMap<>();

    private final ObservableList<DynamicVariable> headersTemplateVariables = FXCollections.observableArrayList();
    private final Map<String, String> headersTemplates = new HashMap<>();
    private final Map<Integer, String> headersTemplateIdNameMap = new HashMap<>();

    private final ObservableList<String> allTcids = FXCollections.observableArrayList();
    private final List<CheckComboBox<String>> conditionTcidComboBoxes = new ArrayList<>();
    private final ObservableList<String> prefixOptions = FXCollections.observableArrayList("Setup", "Teardown",
            "SuiteSetup", "SuiteTeardown");

    private MainViewModel mainViewModel;
    private Tooltip variableTooltip;
    private Tooltip responseCheckTooltip;
    private Tooltip executionFlowTooltip;

    private final CheckBox selectAllCheckBox = new CheckBox();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupWaitTimeSpinner();

        // Hidden ID field
        testIdField.setVisible(false);
        testIdField.setManaged(false);

        // Initialize Helpers
        endpointHandler = new GatlingTestEndpointHandler(
                endpointComboBox,
                urlDynamicVarsTable,
                urlDynamicKeyColumn,
                urlDynamicValueColumn,
                urlDynamicActionColumn,
                generatedUrlArea);
        endpointHandler.initialize();

        responseCheckHandler = new GatlingTestResponseCheckHandler(
                responseChecksTable,
                checkTypeColumn,
                checkExpressionColumn,
                checkOperatorColumn,
                checkExpectColumn,
                checkSaveAsColumn);
        responseCheckHandler.initialize();

        tableHelper = new GatlingTestTableHelper(
                testTable,
                isEnabledColumn,
                suiteColumn,
                testTcidColumn,
                descriptionsColumn,
                endpointColumn,
                tagsColumn,
                waitTimeColumn,
                headersTemplateNameColumn,
                bodyTemplateNameColumn,
                selectAllCheckBox);
        tableHelper.initialize();
        tableHelper.setDependencies(endpointHandler.getEndpointList(), bodyTemplateIdNameMap, headersTemplateIdNameMap);

        // Setup Template Handlers
        setupTemplateHandlers();

        // Setup Conditions
        setupConditionsTable();

        // Setup Tags
        setupTagHandler();

        // Initialize Suite Combo
        suiteComboBox.setEditable(true);
        loadAllSuites();

        // Initialize Filters
        if (enabledFilterCombo != null) {
            enabledFilterCombo.setItems(FXCollections.observableArrayList("All", "Enabled", "Disabled"));
            enabledFilterCombo.setValue("All");
        }

        // Load Data
        loadAllTcids();
        loadTemplates();
        loadHeadersTemplates();
        refreshTests();

        // Listeners
        tableHelper.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showTestDetails(newVal);
            updateGeneratedBody();
            updateGeneratedHeaders();
            if (newVal != null) {
                suiteComboBox.setValue(newVal.getSuite());
            }
        });

        // Bind View Report Button
        if (viewReportButton != null) {
            viewReportButton.disableProperty().bind(
                    tableHelper.getSelectionModel().selectedItemProperty().isNull());
        }

        AppConfig.addChangeListener(this);
        setupTooltips();
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        if (tableHelper != null)
            tableHelper.setMainViewModel(mainViewModel);
        if (responseCheckHandler != null)
            responseCheckHandler.setMainViewModel(mainViewModel);
    }

    @Override
    public void onConfigChanged() {
        Platform.runLater(() -> {
            refreshTests();
            endpointHandler.loadEndpoints();
        });
    }

    public void refresh() {
        refreshAll();
    }

    public void refreshDynamicVariables() {
        VariableGenerator.getInstance().reloadCustomVariables();
        loadTemplates();
        loadHeadersTemplates();
        // refresh dynamic variables table
        if (bodyDynamicVarsTable != null)
            bodyDynamicVarsTable.refresh();
        if (headersTemplateVarsTable != null)
            headersTemplateVarsTable.refresh();
        if (bodyTemplateComboBox != null)
            bodyTemplateComboBox.setItems(FXCollections.observableArrayList(bodyTemplateIdNameMap.values()));
        if (headersTemplateComboBox != null)
            headersTemplateComboBox.setItems(FXCollections.observableArrayList(headersTemplateIdNameMap.values()));
    }

    private void refreshAll() {
        endpointHandler.loadEndpoints();
        loadAllTcids();
        loadTemplates();
        loadHeadersTemplates();
        refreshTests();
        loadAllSuites();
        clearFields();
    }

    private void refreshTests() {
        try {
            Integer projectId = ProjectContext.getCurrentProjectId();
            if (projectId != null) {
                List<GatlingTest> tests = testService.findTestsByProjectId(projectId);
                tableHelper.setTestList(tests);
                updateSuiteFilterOptions();
            }
        } catch (ServiceException e) {
            showError("Failed to load tests: " + e.getMessage());
        }
    }

    private void setupWaitTimeSpinner() {
        waitTimeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3600, 0));
        waitTimeSpinner.setEditable(true);
        waitTimeSpinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                waitTimeSpinner.getEditor().setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void setupTemplateHandlers() {
        bodyTemplateHandler = new TemplateHandler(
                bodyTemplateComboBox, bodyTemplateVariables, bodyTemplates, bodyTemplateIdNameMap,
                bodyDynamicVarsTable, bodyDynamicKeyColumn, bodyDynamicValueColumn, bodyDynamicActionColumn,
                generatedBodyArea);
        headersTemplateHandler = new TemplateHandler(
                headersTemplateComboBox, headersTemplateVariables, headersTemplates, headersTemplateIdNameMap,
                headersTemplateVarsTable, headersTemplateKeyColumn, headersTemplateValueColumn,
                headersTemplateActionColumn, generatedHeadersArea);
        bodyTemplateHandler.setup();
        headersTemplateHandler.setup();
    }

    private void setupConditionsTable() {
        conditionsTable.setItems(conditionHandler.getConditionRows());
        prefixColumn.setCellValueFactory(cellData -> cellData.getValue().prefixProperty());
        conditionTcidColumn.setCellValueFactory(cellData -> {
            String joined = String.join(",", cellData.getValue().getTcids());
            return new javafx.beans.property.SimpleStringProperty(joined);
        });
        conditionTcidColumn.setCellFactory(col -> new TableCell<ConditionRow, String>() {
            private CheckComboBox<String> checkComboBox;

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    if (checkComboBox != null) {
                        conditionTcidComboBoxes.remove(checkComboBox);
                        checkComboBox = null;
                    }
                } else {
                    ConditionRow row = getTableView().getItems().get(getIndex());
                    if (checkComboBox == null) {
                        checkComboBox = new CheckComboBox<>();
                        conditionTcidComboBoxes.add(checkComboBox);
                        checkComboBox.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                            if (!isNowFocused) {
                                Platform.runLater(() -> getTableView().getSelectionModel().clearSelection());
                            }
                        });
                    }
                    checkComboBox.getItems().setAll(allTcids);
                    checkComboBox.setMaxWidth(Double.MAX_VALUE);
                    checkComboBox.setStyle("-fx-alignment: CENTER_LEFT;");
                    checkComboBox.getCheckModel().clearChecks();
                    for (String t : row.getTcids()) {
                        checkComboBox.getCheckModel().check(t);
                    }
                    checkComboBox.getCheckModel().getCheckedItems()
                            .addListener((javafx.collections.ListChangeListener<String>) c -> {
                                row.setTcids(FXCollections
                                        .observableArrayList(checkComboBox.getCheckModel().getCheckedItems()));
                            });
                    setGraphic(checkComboBox);
                    setText(null);
                }
            }
        });
        prefixColumn.setCellFactory(col -> new ComboBoxTableCell<>(prefixOptions));
        addConditionButton.setOnAction(e -> conditionHandler.addCondition("Setup"));
        removeConditionButton.setOnAction(e -> {
            int idx = conditionsTable.getSelectionModel().getSelectedIndex();
            conditionHandler.removeCondition(idx);
        });
    }

    private void setupTagHandler() {
        tagHandler = new TagHandler(tagsFlowPane, tagsInputField);
        tagsInputField.setOnAction(e -> tagHandler.addTag(tagsInputField.getText().trim()));
    }

    private void loadAllTcids() {
        try {
            allTcids.clear();
            for (GatlingTest t : testService.findAllTests()) {
                allTcids.add(t.getTcid());
            }
            for (CheckComboBox<String> cb : conditionTcidComboBoxes) {
                cb.getItems().setAll(allTcids);
            }
        } catch (ServiceException e) {
            showError("Failed to load TCIDs: " + e.getMessage());
        }
    }

    private void loadTemplates() {
        try {
            bodyTemplates.clear();
            bodyTemplateIdNameMap.clear();
            List<BodyTemplate> templates = bodyTemplateService.findAllBodyTemplates();
            for (BodyTemplate template : templates) {
                bodyTemplates.put(template.getName(), template.getContent());
                bodyTemplateIdNameMap.put(template.getId(), template.getName());
            }
            bodyTemplateComboBox.setItems(FXCollections.observableArrayList(bodyTemplateIdNameMap.values()));
        } catch (ServiceException e) {
            showError("Failed to load body templates: " + e.getMessage());
        }
    }

    private void loadHeadersTemplates() {
        try {
            headersTemplates.clear();
            headersTemplateIdNameMap.clear();
            List<HeadersTemplate> templates = headersTemplateService.getAllHeadersTemplates();
            for (HeadersTemplate template : templates) {
                headersTemplates.put(template.getName(), template.getContent());
                headersTemplateIdNameMap.put(template.getId(), template.getName());
            }
            headersTemplateComboBox.setItems(FXCollections.observableArrayList(headersTemplateIdNameMap.values()));
        } catch (ServiceException e) {
            showError("Failed to load headers templates: " + e.getMessage());
        }
    }

    private void loadAllSuites() {
        try {
            List<GatlingTest> allTests = testService.findAllTests();
            List<String> suites = allTests.stream()
                    .map(GatlingTest::getSuite)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
            suiteComboBox.getItems().setAll(suites);
        } catch (Exception ignore) {
        }
    }

    // =====================
    // UI Logic
    // =====================

    private void showTestDetails(GatlingTest test) {
        if (test != null) {
            testIdField.setText(String.valueOf(test.getId()));
            isEnabledCheckBox.setSelected(test.isEnabled());
            suiteComboBox.setValue(test.getSuite());
            tcidField.setText(test.getTcid());
            descriptionsArea.setHtmlText(test.getDescriptions());
            conditionHandler.deserializeConditions(test.getConditions());

            // Endpoint & URL Vars
            endpointHandler.selectEndpoint(test.getEndpointName());
            endpointHandler.setEndpointDynamicVariables(test.getEndpointDynamicVariables());

            // Templates
            if (bodyTemplateIdNameMap.containsKey(test.getBodyTemplateId())) {
                bodyTemplateComboBox.setValue(bodyTemplateIdNameMap.get(test.getBodyTemplateId()));
            } else {
                bodyTemplateComboBox.setValue(null);
            }
            bodyTemplateHandler.setAndFormatVariables(test.getBodyDynamicVariables());

            if (headersTemplateIdNameMap.containsKey(test.getHeadersTemplateId())) {
                headersTemplateComboBox.setValue(headersTemplateIdNameMap.get(test.getHeadersTemplateId()));
            } else {
                headersTemplateComboBox.setValue(null);
            }
            headersTemplateHandler.setAndFormatVariables(test.getHeadersDynamicVariables());

            tagHandler.setTagsFromString(test.getTags());
            waitTimeSpinner.getValueFactory().setValue(test.getWaitTime());

            // Response Checks
            responseCheckHandler.setResponseChecks(test.getResponseChecks());

            updateGeneratedBody();
            updateGeneratedHeaders();
        } else {
            clearFields();
        }
    }

    private void populateTestFromFields(GatlingTest test) {
        test.setEnabled(isEnabledCheckBox.isSelected());
        test.setSuite(suiteComboBox.getEditor().getText().trim());
        test.setTcid(tcidField.getText().trim());
        test.setDescriptions(descriptionsArea.getHtmlText().trim());
        test.setConditions(conditionHandler.serializeConditions());

        // Endpoint
        test.setEndpointName(endpointHandler.getSelectedEndpointName());
        test.setEndpointDynamicVariables(endpointHandler.getEndpointDynamicVariables());

        // Templates
        test.setBodyTemplateId(getBodyTemplateIdByName(bodyTemplateComboBox.getSelectionModel().getSelectedItem()));
        test.setHeadersTemplateId(
                getHeadersTemplateIdByName(headersTemplateComboBox.getSelectionModel().getSelectedItem()));
        test.setTags(tagHandler.getTagsString());
        test.setWaitTime(waitTimeSpinner.getValue());

        Map<String, String> bodyVars = new HashMap<>();
        bodyTemplateVariables.forEach(dv -> bodyVars.put(dv.getKey(), dv.getValue()));
        test.setDynamicVariables(bodyVars);

        Map<String, String> headersVars = new HashMap<>();
        headersTemplateVariables.forEach(dv -> headersVars.put(dv.getKey(), dv.getValue()));
        test.setHeadersDynamicVariables(headersVars);

        // Raw Content
        String selectedBodyTemplateName = bodyTemplateComboBox.getSelectionModel().getSelectedItem();
        if (selectedBodyTemplateName != null && bodyTemplates.containsKey(selectedBodyTemplateName)) {
            test.setBody(bodyTemplates.get(selectedBodyTemplateName));
        } else {
            test.setBody(null);
        }

        String selectedHeadersTemplateName = headersTemplateComboBox.getSelectionModel().getSelectedItem();
        if (selectedHeadersTemplateName != null && headersTemplates.containsKey(selectedHeadersTemplateName)) {
            test.setHeaders(headersTemplates.get(selectedHeadersTemplateName));
        } else {
            test.setHeaders(null);
        }

        test.setProjectId(ProjectContext.getCurrentProjectId());
        test.setResponseChecks(responseCheckHandler.getResponseChecksJson());

        // Add suite to combo if new
        if (test.getSuite() != null && !test.getSuite().isEmpty()
                && !suiteComboBox.getItems().contains(test.getSuite())) {
            suiteComboBox.getItems().add(test.getSuite());
        }
    }

    private int getBodyTemplateIdByName(String name) {
        return bodyTemplateIdNameMap.entrySet().stream()
                .filter(e -> e.getValue().equals(name))
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);
    }

    private int getHeadersTemplateIdByName(String name) {
        return headersTemplateIdNameMap.entrySet().stream()
                .filter(e -> e.getValue().equals(name))
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);
    }

    // =====================
    // Event Handlers
    // =====================

    @FXML
    private void handleAddTest() {
        String tcid = tcidField.getText().trim();
        if (tcid.isEmpty()) {
            showError("Input Error: TCID is required.");
            return;
        }
        GatlingTest newTest = new GatlingTest();
        populateTestFromFields(newTest);

        // Set displayOrder
        int maxOrder = tableHelper.getTestList().stream().mapToInt(GatlingTest::getDisplayOrder).max().orElse(0);
        newTest.setDisplayOrder(maxOrder + 1);

        try {
            testService.createTest(newTest);
            tableHelper.getTestList().add(newTest);
            clearFields();
            tableHelper.getSelectionModel().select(newTest);
            showSuccess("Test added successfully.");
            handleFilterTests(null);
        } catch (ServiceException e) {
            showError("Failed to add test: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateTest() {
        GatlingTest selectedTest = tableHelper.getSelectedTest();
        if (selectedTest == null) {
            showError("Please select a test to update.");
            return;
        }
        if (tcidField.getText().trim().isEmpty()) {
            showError("Input Error: TCID is required.");
            return;
        }
        populateTestFromFields(selectedTest);
        try {
            testService.updateTest(selectedTest);
            testTable.refresh();
            showSuccess("Test updated successfully.");
            handleFilterTests(null);
        } catch (ServiceException e) {
            showError("Failed to update test: " + e.getMessage());
        }
    }

    @FXML
    private void handleDuplicateTest() {
        GatlingTest selectedTest = tableHelper.getSelectedTest();
        if (selectedTest == null) {
            showError("Please select a test to duplicate.");
            return;
        }
        try {
            GatlingTest duplicate = new GatlingTest(selectedTest);
            int maxOrder = tableHelper.getTestList().stream().mapToInt(GatlingTest::getDisplayOrder).max().orElse(0);
            duplicate.setDisplayOrder(maxOrder + 1);
            testService.createTest(duplicate);
            showSuccess("Test duplicated successfully.");
            refreshTests(); // Reload to get ID and correct state
        } catch (ServiceException e) {
            showError("Error duplicating test: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteTest() {
        List<GatlingTest> selectedTests = tableHelper.getSelectedTests();
        if (selectedTests.isEmpty()) {
            showError("Please select at least one test to delete.");
            return;
        }
        try {
            for (GatlingTest test : selectedTests) {
                testService.removeTest(test.getId());
            }
            tableHelper.getTestList().removeAll(selectedTests);
            clearFields();
            showSuccess(selectedTests.size() + " test(s) deleted successfully.");
        } catch (ServiceException e) {
            showError("Failed to delete test(s): " + e.getMessage());
        }
    }

    @FXML
    private void handleClearFields() {
        clearFields();
        tableHelper.getSelectionModel().clearSelection();
    }

    private void clearFields() {
        testIdField.clear();
        isEnabledCheckBox.setSelected(false);
        suiteComboBox.setValue("");
        tcidField.clear();
        descriptionsArea.setHtmlText("");

        endpointHandler.clear();

        bodyTemplateComboBox.setValue(null);
        bodyTemplateVariables.clear();
        headersTemplateComboBox.setValue(null);
        headersTemplateVariables.clear();

        tagHandler.setTagsFromString("");
        waitTimeSpinner.getValueFactory().setValue(0);
        conditionHandler.getConditionRows().clear();

        responseCheckHandler.clear();

        updateGeneratedBody();
        updateGeneratedHeaders();
    }

    @FXML
    private void handleMoveUp() {
        GatlingTest selected = tableHelper.getSelectedTest();
        if (selected == null)
            return;
        int index = tableHelper.getTestList().indexOf(selected);
        if (index > 0) {
            java.util.Collections.swap(tableHelper.getTestList(), index, index - 1);
            updateDisplayOrderAndPersist();
            tableHelper.getSelectionModel().select(index - 1);
        }
    }

    @FXML
    private void handleMoveDown() {
        GatlingTest selected = tableHelper.getSelectedTest();
        if (selected == null)
            return;
        int index = tableHelper.getTestList().indexOf(selected);
        if (index < tableHelper.getTestList().size() - 1) {
            java.util.Collections.swap(tableHelper.getTestList(), index, index + 1);
            updateDisplayOrderAndPersist();
            tableHelper.getSelectionModel().select(index + 1);
        }
    }

    private void updateDisplayOrderAndPersist() {
        try {
            List<GatlingTest> list = tableHelper.getTestList();
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setDisplayOrder(i + 1);
            }
            testService.updateOrder(new ArrayList<>(list));
        } catch (ServiceException e) {
            showError("Failed to update test order: " + e.getMessage());
        }
    }

    @FXML
    private void handleRunTest() {
        List<GatlingTest> selectedTests = tableHelper.getSelectedTests();
        if (selectedTests.isEmpty()) {
            showError("Please select at least one test to run.");
            return;
        }

        // Validate Endpoints
        Integer envId = EnvironmentContext.getCurrentEnvironmentId();
        for (GatlingTest t : selectedTests) {
            try {
                Endpoint ep = endpointService.getEndpointByNameAndEnv(t.getEndpointName(), envId);
                if (ep == null) {
                    showError("Endpoint '" + t.getEndpointName() + "' not found for test " + t.getTcid());
                    return;
                }
            } catch (Exception e) {
                showError("Error finding endpoint: " + e.getMessage());
                return;
            }
        }

        GatlingLoadParameters loadParams = new GatlingLoadParameters();
        com.qa.app.model.threadgroups.StandardThreadGroup tg = new com.qa.app.model.threadgroups.StandardThreadGroup();
        tg.setNumThreads(1);
        tg.setRampUp(0);
        tg.setLoops(1);
        loadParams.setStandardThreadGroup(tg);
        loadParams.setType(com.qa.app.model.threadgroups.ThreadGroupType.STANDARD);

        if (runTestButton != null)
            runTestButton.setDisable(true);
        showInfo("Running " + selectedTests.size() + " Gatling test(s)...");

        Runnable onComplete = () -> Platform.runLater(() -> {
            handleFilterTests(null);
            showSuccess("Gatling test(s) completed.");
            if (runTestButton != null)
                runTestButton.setDisable(false);
        });

        try {
            testService.runTests(selectedTests, loadParams, onComplete);
        } catch (ServiceException e) {
            if (runTestButton != null)
                runTestButton.setDisable(false);
            showError("Failed to run tests: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilterTests(javafx.event.ActionEvent evt) {
        if (suiteFilterCombo == null || tagFilterField == null)
            return;
        String suite = suiteFilterCombo.getValue();
        String tagKw = tagFilterField.getText();
        String enabledOpt = enabledFilterCombo == null ? null : enabledFilterCombo.getValue();

        refreshTests(); // Reload all

        tableHelper.getTestList().removeIf(t -> {
            boolean ok = true;
            if (suite != null && !"All".equals(suite) && !suite.isBlank()) {
                ok = ok && suite.equals(t.getSuite());
            }
            if (ok && tagKw != null && !tagKw.isBlank()) {
                if (t.getTags() == null) {
                    ok = false;
                } else {
                    boolean anyMatch = false;
                    for (String token : tagKw.split("[,\n\r\t ]+")) {
                        if (!token.trim().isEmpty() && t.getTags().contains(token.trim())) {
                            anyMatch = true;
                            break;
                        }
                    }
                    ok = anyMatch;
                }
            }
            if (ok && enabledOpt != null && !"All".equals(enabledOpt)) {
                if ("Enabled".equals(enabledOpt))
                    ok = t.isEnabled();
                else if ("Disabled".equals(enabledOpt))
                    ok = !t.isEnabled();
            }
            return !ok;
        });
        updateSuiteFilterOptions();
        tableHelper.updateSelectAllCheckBoxState();
    }

    @FXML
    private void handleResetFilter(javafx.event.ActionEvent evt) {
        if (suiteFilterCombo != null)
            suiteFilterCombo.setValue("All");
        if (tagFilterField != null)
            tagFilterField.clear();
        if (enabledFilterCombo != null)
            enabledFilterCombo.setValue("All");
        refreshTests();
    }

    // Response Check Delegates
    @FXML
    private void handleAddResponseCheck() {
        responseCheckHandler.handleAddResponseCheck();
    }

    @FXML
    private void handleRemoveResponseCheck() {
        responseCheckHandler.handleRemoveResponseCheck();
    }

    @FXML
    private void handleCopyResponseCheck() {
        responseCheckHandler.handleCopyResponseCheck();
    }

    @FXML
    private void handlePasteResponseCheck() {
        responseCheckHandler.handlePasteResponseCheck();
    }

    // Tag Delegate
    @FXML
    private void handleTagInput() {
        tagHandler.addTag(tagsInputField.getText().trim());
    }

    // Condition Delegates
    @FXML
    private void handleAddCondition() {
        conditionHandler.addCondition("Setup");
    }

    @FXML
    private void handleRemoveCondition() {
        conditionHandler.removeCondition(conditionsTable.getSelectionModel().getSelectedIndex());
    }

    // Template Updates
    private void updateGeneratedBody() {
        String body = bodyTemplateHandler.buildContent();
        generatedBodyArea.setText(body == null ? "" : body);
    }

    private void updateGeneratedHeaders() {
        String headers = headersTemplateHandler.buildContent();
        generatedHeadersArea.setText(headers == null ? "" : headers);
    }

    private void updateSuiteFilterOptions() {
        if (suiteFilterCombo == null)
            return;
        List<String> suites = tableHelper.getTestList().stream()
                .map(GatlingTest::getSuite)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
        suites.add(0, "All");
        suiteFilterCombo.setItems(FXCollections.observableArrayList(suites));
    }

    // Status Helpers
    private void showError(String msg) {
        if (mainViewModel != null)
            mainViewModel.updateStatus(msg, MainViewModel.StatusType.ERROR);
    }

    private void showSuccess(String msg) {
        if (mainViewModel != null)
            mainViewModel.updateStatus(msg, MainViewModel.StatusType.SUCCESS);
    }

    private void showInfo(String msg) {
        if (mainViewModel != null)
            mainViewModel.updateStatus(msg, MainViewModel.StatusType.INFO);
    }

    private void setupTooltips() {
        variableTooltip = HelpTooltipManager.buildVariableTooltip();
        headersHelpIcon.setOnMouseClicked(event -> toggleTooltip(variableTooltip, headersHelpIcon));
        bodyHelpIcon.setOnMouseClicked(event -> toggleTooltip(variableTooltip, bodyHelpIcon));

        responseCheckTooltip = HelpTooltipManager.getResponseCheckTooltip();
        if (responseHelpButton != null)
            responseHelpButton.setOnAction(e -> handleResponseHelpClick());
        if (responseHelpIcon != null)
            responseHelpIcon.setOnMouseClicked(e -> handleResponseHelpClick());

        executionFlowTooltip = HelpTooltipManager.getExecutionFlowTooltip();
        if (runHelpIcon != null)
            runHelpIcon.setOnMouseClicked(e -> toggleTooltip(executionFlowTooltip, runHelpIcon));
    }

    private void toggleTooltip(Tooltip tooltip, Node ownerNode) {
        if (tooltip.isShowing()) {
            tooltip.hide();
        } else {
            Point2D p = ownerNode.localToScreen(ownerNode.getBoundsInLocal().getMaxX(),
                    ownerNode.getBoundsInLocal().getMaxY());
            tooltip.show(ownerNode, p.getX(), p.getY());
        }
    }

    @FXML
    private void handleResponseHelpClick() {
        if (responseCheckTooltip != null) {
            if (responseCheckTooltip.isShowing()) {
                responseCheckTooltip.hide();
            } else {
                Point2D p = responseHelpButton.localToScreen(
                        responseHelpButton.getBoundsInLocal().getMaxX(),
                        responseHelpButton.getBoundsInLocal().getMinY());
                responseCheckTooltip.show(responseHelpButton, p.getX(), p.getY());
            }
        }
    }
}
