package com.qa.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.cell.ComboBoxTableCell;
import org.controlsfx.control.CheckComboBox;

import com.qa.app.model.BodyTemplate;
import com.qa.app.model.ConditionRow;
import com.qa.app.model.DynamicVariable;
import com.qa.app.model.GatlingTest;
import com.qa.app.model.HeadersTemplate;
import com.qa.app.model.Endpoint;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IBodyTemplateService;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.api.IHeadersTemplateService;
import com.qa.app.service.api.IEndpointService;
import com.qa.app.service.impl.BodyTemplateServiceImpl;
import com.qa.app.service.impl.GatlingTestServiceImpl;
import com.qa.app.service.impl.HeadersTemplateServiceImpl;
import com.qa.app.ui.vm.gatling.TemplateHandler;
import com.qa.app.service.impl.EndpointServiceImpl;
import com.qa.app.ui.vm.gatling.TestCondictionHandler;
import com.qa.app.ui.vm.gatling.TagHandler;
import com.qa.app.util.AppConfig;
import com.qa.app.model.Environment;
import com.qa.app.service.impl.EnvironmentServiceImpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class GatlingTestViewModel implements Initializable {

    @FXML
    private TextField testIdField;
    @FXML
    private CheckBox isEnabledCheckBox;
    @FXML
    private ComboBox<String> suiteComboBox;
    @FXML
    private TextField tcidField;
    @FXML
    private TextArea descriptionsArea;
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
    private TextArea expResultArea;
    @FXML
    private TextArea saveFieldsArea;
    @FXML
    private ComboBox<String> endpointComboBox;
    @FXML
    private ComboBox<String> bodyTemplateComboBox;
    @FXML
    private TableView<DynamicVariable> bodyDynamicVarsTable;
    @FXML
    private TableColumn<DynamicVariable, String> bodyDynamicKeyColumn;
    @FXML
    private TableColumn<DynamicVariable, String> bodyDynamicValueColumn;
    @FXML
    private TextField tagsInputField;
    @FXML
    private Spinner<Integer> waitTimeSpinner;
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
    private Button runSuiteButton;
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
    private ComboBox<String> headersTemplateComboBox;
    @FXML
    private TableView<DynamicVariable> headersTemplateVarsTable;
    @FXML
    private TableColumn<DynamicVariable, String> headersTemplateKeyColumn;
    @FXML
    private TableColumn<DynamicVariable, String> headersTemplateValueColumn;
    @FXML
    private TableColumn<GatlingTest, String> headersTemplateNameColumn;
    @FXML
    private TableColumn<GatlingTest, String> bodyTemplateNameColumn;
    @FXML
    private FlowPane tagsFlowPane;
    @FXML
    private ComboBox<String> expStatusComboBox;
    @FXML
    private TextArea generatedHeadersArea;
    @FXML
    private TextArea generatedBodyArea;

    private final IGatlingTestService testService = new GatlingTestServiceImpl();
    private final IBodyTemplateService bodyTemplateService = new BodyTemplateServiceImpl();
    private final IHeadersTemplateService headersTemplateService = new HeadersTemplateServiceImpl();
    private final IEndpointService endpointService = new EndpointServiceImpl();
    private final ObservableList<GatlingTest> testList = FXCollections.observableArrayList();
    private final ObservableList<DynamicVariable> bodyTemplateVariables = FXCollections.observableArrayList();
    private Map<String, String> bodyTemplates = new HashMap<>();
    private final ObservableList<DynamicVariable> headersTemplateVariables = FXCollections.observableArrayList();
    private Map<String, String> headersTemplates = new HashMap<>();
    private final ObservableList<Endpoint> endpointList = FXCollections.observableArrayList();
    private final ObservableList<String> endpointNameList = FXCollections.observableArrayList();

    private MainViewModel mainViewModel;
    private final ObservableList<String> allTcids = FXCollections.observableArrayList();
    private final ObservableList<String> prefixOptions = FXCollections.observableArrayList("Setup", "Teardown",
            "SuiteSetup", "SuiteTeardown", "CheckWith");

    // Used to store all TCID dropdown references
    private final List<CheckComboBox<String>> conditionTcidComboBoxes = new ArrayList<>();
    // Used to store bodyTemplate and headersTemplate name by id
    private Map<Integer, String> bodyTemplateIdNameMap = new HashMap<>();
    private Map<Integer, String> headersTemplateIdNameMap = new HashMap<>();
    // Used to handle body and headers templates, and condition handler, tag handler
    private TemplateHandler bodyTemplateHandler;
    private TemplateHandler headersTemplateHandler;
    private final TestCondictionHandler conditionHandler = new TestCondictionHandler();
    private TagHandler tagHandler;

    private final EnvironmentServiceImpl environmentService = new EnvironmentServiceImpl();

    // =====================
    // 1. Initialize related
    // =====================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize spinner
        waitTimeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3600, 0));
        waitTimeSpinner.setEditable(true);
        // Only allow numeric input
        waitTimeSpinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                waitTimeSpinner.getEditor().setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        // Sync Spinner value when focus is lost
        waitTimeSpinner.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try {
                    int value = Integer.parseInt(waitTimeSpinner.getEditor().getText());
                    waitTimeSpinner.getValueFactory().setValue(value);
                } catch (NumberFormatException e) {
                    waitTimeSpinner.getValueFactory().setValue(0);
                }
            }
        });

        // testIdField is hidden but used internally
        testIdField.setVisible(false);
        testIdField.setManaged(false);

        // Initialize dynamic variables table
        bodyDynamicKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        bodyDynamicValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        bodyDynamicVarsTable.setItems(bodyTemplateVariables);
        bodyDynamicVarsTable.setEditable(true);

        // Set cellFactory to custom TextFieldTableCell with focus lost commit
        bodyDynamicValueColumn.setCellFactory(col -> new TextFieldTableCell<DynamicVariable, String>(
                new javafx.util.converter.DefaultStringConverter()) {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                if (textField != null) {
                    textField.focusedProperty().addListener((obs, was, isNow) -> {
                        if (!isNow && isEditing()) {
                            commitEdit(textField.getText());
                        }
                    });
                }
            }
        });
        bodyDynamicValueColumn.setOnEditCommit(event -> {
            DynamicVariable editedVar = event.getTableView().getItems().get(event.getTablePosition().getRow());
            editedVar.setValue(event.getNewValue());
            GatlingTest selectedTest = testTable.getSelectionModel().getSelectedItem();
            if (selectedTest != null) {
                selectedTest.getBodyDynamicVariables().put(editedVar.getKey(), editedVar.getValue());
            }
        });
        // Listen to TableView's editing cell when focus is lost and auto-commit
        bodyDynamicVarsTable.setRowFactory(tv -> {
            TableRow<DynamicVariable> row = new TableRow<>();
            row.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused && tv.getEditingCell() != null) {
                    tv.edit(-1, null); // End editing, auto-commit
                }
            });
            return row;
        });

        // Initialize TemplateHandler
        bodyTemplateHandler = new TemplateHandler(
                bodyTemplateComboBox, bodyTemplateVariables, bodyTemplates, bodyTemplateIdNameMap,
                bodyDynamicVarsTable, bodyDynamicKeyColumn, bodyDynamicValueColumn, generatedBodyArea);
        headersTemplateHandler = new TemplateHandler(
                headersTemplateComboBox, headersTemplateVariables, headersTemplates, headersTemplateIdNameMap,
                headersTemplateVarsTable, headersTemplateKeyColumn, headersTemplateValueColumn, generatedHeadersArea);
        bodyTemplateHandler.setup();
        headersTemplateHandler.setup();

        expStatusComboBox.setItems(FXCollections.observableArrayList(
                "200", "400", "401", "403", "404", "408", "500", "501", "503", "504"));
        expStatusComboBox.setValue("200");

        // --- Test Conditions Table ---
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
                    }
                    // clear all items and add latest allTcids
                    checkComboBox.getItems().setAll(allTcids);
                    checkComboBox.setPrefWidth(180);
                    checkComboBox.setMaxWidth(180);
                    checkComboBox.setMinWidth(120);
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
        conditionTcidColumn.setStyle("-fx-alignment: CENTER_LEFT;");
        // Prefix ComboBox
        prefixColumn.setCellFactory(col -> {
            TableCell<ConditionRow, String> cell = new ComboBoxTableCell<>(prefixOptions);
            return cell;
        });
        // Add/Remove button actions
        addConditionButton.setOnAction(e -> handleAddCondition());
        removeConditionButton.setOnAction(e -> handleRemoveCondition());

        endpointComboBox.setItems(endpointNameList);
        endpointComboBox.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String name) {
                return name == null ? "" : name;
            }
            @Override
            public String fromString(String s) {
                return s;
            }
        });
        endpointComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(endpointComboBox.getPromptText());
                } else {
                    setText(item);
                }
            }
        });
        loadEndpoints();

        // initialize suiteComboBox
        loadAllSuites();
        suiteComboBox.setEditable(true);

        // TagHandler initialization
        tagHandler = new TagHandler(tagsFlowPane, tagsInputField);
        tagsInputField.setOnAction(e -> handleTagInput());
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;

        // Initialize table columns

        isEnabledColumn.setCellValueFactory(new PropertyValueFactory<>("isEnabled"));
        isEnabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(isEnabledColumn));
        suiteColumn.setCellValueFactory(new PropertyValueFactory<>("suite"));
        testTcidColumn.setCellValueFactory(new PropertyValueFactory<>("tcid"));
        descriptionsColumn.setCellValueFactory(new PropertyValueFactory<>("descriptions"));
        descriptionsColumn.setCellFactory(col -> new TableCell<GatlingTest, String>() {
            private Tooltip tooltip = new Tooltip();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    if (item.length() > 20) { // Only show tooltip if text is longer than 20 characters
                        tooltip.setText(item);
                        setTooltip(tooltip);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });
        endpointColumn.setCellValueFactory(cellData -> {
            String endpointName = cellData.getValue().getEndpointName();
            // 查找当前环境下的endpoint
            String envName = AppConfig.getCurrentEnv();
            Integer envId = null;
            try {
                for (Environment env : environmentService.findAllEnvironments()) {
                    if (env.getName().equals(envName)) {
                        envId = env.getId();
                        break;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            String display = endpointName == null ? "" : endpointName;
            if (envId != null) {
                for (Endpoint ep : endpointList) {
                    if (ep.getName().equals(endpointName) && envId.equals(ep.getEnvironmentId())) {
                        display = ep.getName() + " [ " + ep.getMethod() + " " + ep.getUrl() + " ]";
                        break;
                    }
                }
            }
            return new javafx.beans.property.SimpleStringProperty(display);
        });
        endpointColumn.setCellFactory(col -> new TableCell<GatlingTest, String>() {
            private Tooltip tooltip = new Tooltip();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    if (item.length() > 20) { // 超过20字符显示tooltip
                        tooltip.setText(item);
                        setTooltip(tooltip);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });
        tagsColumn.setCellValueFactory(new PropertyValueFactory<>("tags"));
        waitTimeColumn.setCellValueFactory(new PropertyValueFactory<>("waitTime"));
        headersTemplateNameColumn.setCellValueFactory(cellData -> {
            int id = cellData.getValue().getHeadersTemplateId();
            String name = headersTemplateIdNameMap.getOrDefault(id, "");
            return new javafx.beans.property.SimpleStringProperty(name);
        });
        bodyTemplateNameColumn.setCellValueFactory(cellData -> {
            int id = cellData.getValue().getBodyTemplateId();
            String name = bodyTemplateIdNameMap.getOrDefault(id, "");
            return new javafx.beans.property.SimpleStringProperty(name);
        });

        testTable.setItems(testList);
        testTable.setEditable(true);

        // Add listener for table selection
        testTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    showTestDetails(newValue);
                    updateGeneratedBody();
                    updateGeneratedHeaders();
                });
        // Add listener for testList变动时刷新
        testList.addListener((javafx.collections.ListChangeListener<GatlingTest>) c -> {
            updateGeneratedBody();
            updateGeneratedHeaders();
        });

        // suiteComboBox auto-complete and fill
        testTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                suiteComboBox.setValue(newVal.getSuite());
            }
        });

        // Load initial data and templates
        loadTests();
        loadTemplates();
        loadHeadersTemplates();
        loadAllTcids();
    }

    // =====================
    // 2. Data loading related
    // =====================
    private void loadEndpoints() {
        endpointList.clear();
        endpointNameList.clear();
        try {
            // 获取当前环境
            String envName = AppConfig.getCurrentEnv();
            Integer envId = null;
            for (Environment env : environmentService.findAllEnvironments()) {
                if (env.getName().equals(envName)) {
                    envId = env.getId();
                    break;
                }
            }
            if (envId != null) {
                // 只加载当前环境下的endpoint
                for (Endpoint ep : endpointService.getAllEndpoints()) {
                    if (envId.equals(ep.getEnvironmentId())) {
                        endpointList.add(ep);
                    }
                }
            }
        } catch (Exception e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load endpoints: " + e.getMessage(),
                        MainViewModel.StatusType.ERROR);
            }
        }
        endpointComboBox.setItems(FXCollections.observableArrayList(endpointList.stream().map(ep -> ep.getName() + " [ " + ep.getMethod() + " " + ep.getUrl() + " ]").toList()));
        endpointComboBox.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String display) {
                return display == null ? "" : display;
            }
            @Override
            public String fromString(String s) {
                return s;
            }
        });
        endpointComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(endpointComboBox.getPromptText());
                } else {
                    setText(item);
                }
            }
        });
    }

    private void loadAllTcids() {
        try {
            allTcids.clear();
            for (GatlingTest t : testService.findAllTests()) {
                allTcids.add(t.getTcid());
            }
            // notify all CheckComboBox to refresh items
            for (CheckComboBox<String> cb : conditionTcidComboBoxes) {
                cb.getItems().setAll(allTcids);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load TCIDs: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void loadTests() {
        try {
            Integer projectId = AppConfig.getCurrentProjectId();
            if (projectId != null) {
                testList.setAll(testService.findTestsByProjectId(projectId));
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load tests: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
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
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load body templates: " + e.getMessage(),
                        MainViewModel.StatusType.ERROR);
            }
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
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load headers templates: " + e.getMessage(),
                        MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void loadAllSuites() {
        // get all suite from testService and add to ComboBox
        try {
            List<GatlingTest> allTests = testService.findAllTests();
            List<String> suites = new ArrayList<>();
            for (GatlingTest t : allTests) {
                if (t.getSuite() != null && !t.getSuite().isEmpty() && !suites.contains(t.getSuite())) {
                    suites.add(t.getSuite());
                }
            }
            suiteComboBox.getItems().setAll(suites);
        } catch (Exception e) {
            // ignore
        }
    }

    // =====================
    // 3. UI refresh/helper methods
    // =====================
    public void refresh() {
        refreshAll();
    }

    private void refreshAll() {
        loadEndpoints();
        loadAllTcids();
        loadTests();
        loadTemplates();
        loadHeadersTemplates();
        clearFields();
        conditionsTable.refresh();
        updateGeneratedBody();
        updateGeneratedHeaders();
        loadAllSuites();
    }

    private void clearFields() {
        testIdField.clear();
        isEnabledCheckBox.setSelected(false);
        suiteComboBox.setValue("");
        tcidField.clear();
        descriptionsArea.clear();
        expResultArea.clear();
        saveFieldsArea.clear();
        endpointComboBox.getSelectionModel().clearSelection();
        endpointComboBox.setValue(null);
        endpointComboBox.setPromptText("Select Endpoint");
        bodyTemplateComboBox.getSelectionModel().clearSelection();
        bodyTemplateComboBox.setValue(null);
        bodyTemplateVariables.clear();
        headersTemplateComboBox.getSelectionModel().clearSelection();
        headersTemplateComboBox.setValue(null);
        headersTemplateVariables.clear();
        tagHandler.setTagsFromString("");
        waitTimeSpinner.getValueFactory().setValue(0);
        expStatusComboBox.setValue("200");
        conditionHandler.getConditionRows().clear();
        updateGeneratedBody();
        updateGeneratedHeaders();
    }

    private void updateGeneratedBody() {
        String body = buildRequestBody();
        generatedBodyArea.setText(body == null ? "" : body);
    }

    private void updateGeneratedHeaders() {
        String headers = buildHeaders();
        generatedHeadersArea.setText(headers == null ? "" : headers);
    }

    private void showTestDetails(GatlingTest test) {
        if (test != null) {
            testIdField.setText(String.valueOf(test.getId()));
            isEnabledCheckBox.setSelected(test.isEnabled());
            suiteComboBox.setValue(test.getSuite());
            tcidField.setText(test.getTcid());
            descriptionsArea.setText(test.getDescriptions());
            conditionHandler.deserializeConditions(test.getConditions());
            expResultArea.setText(test.getExpResult());
            saveFieldsArea.setText(test.getSaveFields());
            String display = null;
            for (Endpoint ep : endpointList) {
                if (ep.getName().equals(test.getEndpointName())) {
                    display = ep.getName() + " [ " + ep.getMethod() + " " + ep.getUrl() + " ]";
                    break;
                }
            }
            endpointComboBox.setValue(display);
            if (bodyTemplateIdNameMap.containsKey(test.getBodyTemplateId())) {
                bodyTemplateComboBox.setValue(bodyTemplateIdNameMap.get(test.getBodyTemplateId()));
            } else {
                bodyTemplateComboBox.setValue(null);
            }
            bodyTemplateVariables.clear();
            if (test.getBodyDynamicVariables() != null) {
                test.getBodyDynamicVariables()
                        .forEach((key, value) -> bodyTemplateVariables.add(new DynamicVariable(key, value)));
            }
            if (headersTemplateIdNameMap.containsKey(test.getHeadersTemplateId())) {
                headersTemplateComboBox.setValue(headersTemplateIdNameMap.get(test.getHeadersTemplateId()));
            } else {
                headersTemplateComboBox.setValue(null);
            }
            headersTemplateVariables.clear();
            if (test.getHeadersDynamicVariables() != null) {
                test.getHeadersDynamicVariables()
                        .forEach((key, value) -> headersTemplateVariables.add(new DynamicVariable(key, value)));
            }
            tagHandler.setTagsFromString(test.getTags());
            waitTimeSpinner.getValueFactory().setValue(test.getWaitTime());
            expStatusComboBox.setValue(
                    test.getExpStatus() == null || test.getExpStatus().isEmpty() ? "200" : test.getExpStatus());
            updateGeneratedBody();
            updateGeneratedHeaders();
        } else {
            clearFields();
        }
    }

    // =====================
    // 4. Form/control event handling
    // =====================
    @FXML
    private void handleAddTest() {
        String suite = suiteComboBox.getEditor().getText().trim();
        String tcid = tcidField.getText().trim();
        String descriptions = descriptionsArea.getText().trim();
        String endpointDisplay = endpointComboBox.getValue();
        String endpointName = endpointDisplay == null ? "" : endpointDisplay.split(" \\[")[0].trim();
        if (tcid.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: TCID is required.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }
        GatlingTest newTest = new GatlingTest(suite, tcid, descriptions, endpointName, AppConfig.getCurrentProjectId());
        newTest.setEnabled(isEnabledCheckBox.isSelected());
        newTest.setConditions(serializeConditions());
        newTest.setExpResult(expResultArea.getText());
        newTest.setSaveFields(saveFieldsArea.getText());
        newTest.setHeaders(headersTemplateComboBox.getSelectionModel().getSelectedItem());
        Map<String, String> vars = new HashMap<>();
        bodyTemplateVariables.forEach(dv -> vars.put(dv.getKey(), dv.getValue()));
        newTest.setDynamicVariables(vars);
        newTest.setBody(buildRequestBody());
        newTest.setHeaders(buildHeaders());
        newTest.setBodyTemplateId(getBodyTemplateIdByName(bodyTemplateComboBox.getSelectionModel().getSelectedItem()));
        newTest.setHeadersTemplateId(
                getHeadersTemplateIdByName(headersTemplateComboBox.getSelectionModel().getSelectedItem()));
        newTest.setTags(getTagsString());
        newTest.setWaitTime(waitTimeSpinner.getValue());
        newTest.setExpStatus(expStatusComboBox.getValue());
        Map<String, String> headersVars = new HashMap<>();
        headersTemplateVariables.forEach(dv -> headersVars.put(dv.getKey(), dv.getValue()));
        newTest.setHeadersDynamicVariables(headersVars);
        newTest.setProjectId(AppConfig.getCurrentProjectId());
        // new suite auto-add to dropdown
        if (suite != null && !suite.isEmpty() && !suiteComboBox.getItems().contains(suite)) {
            suiteComboBox.getItems().add(suite);
        }
        try {
            testService.createTest(newTest);
            testList.add(newTest);
            clearFields();
            testTable.getSelectionModel().select(newTest);
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Test added successfully.", MainViewModel.StatusType.SUCCESS);
            }
            refreshAll();
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to add test: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleUpdateTest() {
        GatlingTest selectedTest = testTable.getSelectionModel().getSelectedItem();
        if (selectedTest == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select a test from the table to update.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }
        String suite = suiteComboBox.getEditor().getText().trim();
        String tcid = tcidField.getText().trim();
        String endpointDisplay = endpointComboBox.getValue();
        String endpointName = endpointDisplay == null ? "" : endpointDisplay.split(" \\[")[0].trim();
        if (tcid.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: TCID is required.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }
        selectedTest.setEnabled(isEnabledCheckBox.isSelected());
        selectedTest.setSuite(suite);
        selectedTest.setTcid(tcid);
        selectedTest.setDescriptions(descriptionsArea.getText());
        selectedTest.setConditions(serializeConditions());
        selectedTest.setExpResult(expResultArea.getText());
        selectedTest.setSaveFields(saveFieldsArea.getText());
        selectedTest.setEndpointName(endpointName);
        selectedTest.setHeaders(headersTemplateComboBox.getSelectionModel().getSelectedItem());
        Map<String, String> vars = new HashMap<>();
        bodyTemplateVariables.forEach(dv -> vars.put(dv.getKey(), dv.getValue()));
        selectedTest.setDynamicVariables(vars);
        selectedTest.setBody(buildRequestBody());
        selectedTest
                .setBodyTemplateId(getBodyTemplateIdByName(bodyTemplateComboBox.getSelectionModel().getSelectedItem()));
        selectedTest.setHeaders(buildHeaders());
        selectedTest.setHeadersTemplateId(
                getHeadersTemplateIdByName(headersTemplateComboBox.getSelectionModel().getSelectedItem()));
        selectedTest.setTags(getTagsString());
        selectedTest.setWaitTime(waitTimeSpinner.getValue());
        selectedTest.setExpStatus(expStatusComboBox.getValue());
        Map<String, String> headersVars = new HashMap<>();
        headersTemplateVariables.forEach(dv -> headersVars.put(dv.getKey(), dv.getValue()));
        selectedTest.setHeadersDynamicVariables(headersVars);
        selectedTest.setProjectId(AppConfig.getCurrentProjectId());
        // new suite auto-add to dropdown
        if (suite != null && !suite.isEmpty() && !suiteComboBox.getItems().contains(suite)) {
            suiteComboBox.getItems().add(suite);
        }
        try {
            testService.updateTest(selectedTest);
            testTable.refresh();
            testTable.getSelectionModel().select(selectedTest);
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Test updated successfully.", MainViewModel.StatusType.SUCCESS);
            }
            refreshAll();
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to update test: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleDeleteTest() {
        GatlingTest selectedTest = testTable.getSelectionModel().getSelectedItem();
        if (selectedTest == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select a test from the table to delete.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }

        try {
            testService.removeTest(selectedTest.getId());
            testList.remove(selectedTest);
            clearFields();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Test deleted successfully.", MainViewModel.StatusType.SUCCESS);
            }
            refreshAll();
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to delete test: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleClearFields() {
        clearFields();
        testTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRunTest() {
        GatlingTest selectedTest = testTable.getSelectionModel().getSelectedItem();
        if (selectedTest == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select a test to run.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }

        try {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Running test: " + selectedTest.getTcid(), MainViewModel.StatusType.INFO);
            }
            testService.runTest(selectedTest);
            testTable.refresh();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Test completed: " + selectedTest.getTcid(),
                        MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to run test: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleRunSuite() {
        String suite = suiteComboBox.getEditor().getText().trim();
        if (suite.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Please enter a suite name to run.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }

        try {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Running test suite: " + suite, MainViewModel.StatusType.INFO);
            }
            testService.runTestSuite(suite);
            refreshAll();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Test suite completed: " + suite, MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to run test suite: " + e.getMessage(),
                        MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleTagInput() {
        String tag = tagsInputField.getText().trim();
        tagHandler.addTag(tag);
    }

    @FXML
    private void handleAddCondition() {
        conditionHandler.addCondition("Setup");
    }

    @FXML
    private void handleRemoveCondition() {
        int idx = conditionsTable.getSelectionModel().getSelectedIndex();
        conditionHandler.removeCondition(idx);
    }

    // =====================
    // 5. Template/variable handling
    // =====================
    private String buildRequestBody() {
        String selectedTemplateName = bodyTemplateComboBox.getSelectionModel().getSelectedItem();
        if (selectedTemplateName == null || !bodyTemplates.containsKey(selectedTemplateName)) {
            return null;
        }
        String template = bodyTemplates.get(selectedTemplateName);
        String body = template;
        for (DynamicVariable var : bodyTemplateVariables) {
            body = body.replace("${" + var.getKey() + "}", var.getValue());
        }
        return body;
    }

    private String buildHeaders() {
        String selectedTemplateName = headersTemplateComboBox.getSelectionModel().getSelectedItem();
        if (selectedTemplateName == null || !headersTemplates.containsKey(selectedTemplateName)) {
            return null;
        }
        String template = headersTemplates.get(selectedTemplateName);
        String headers = template;
        for (DynamicVariable var : headersTemplateVariables) {
            headers = headers.replace("${" + var.getKey() + "}", var.getValue());
        }
        return headers;
    }

    private int getBodyTemplateIdByName(String name) {
        for (Map.Entry<Integer, String> entry : bodyTemplateIdNameMap.entrySet()) {
            if (entry.getValue().equals(name))
                return entry.getKey();
        }
        return 0;
    }

    private int getHeadersTemplateIdByName(String name) {
        for (Map.Entry<Integer, String> entry : headersTemplateIdNameMap.entrySet()) {
            if (entry.getValue().equals(name))
                return entry.getKey();
        }
        return 0;
    }

    private String getTagsString() {
        return tagHandler.getTagsString();
    }

    private String serializeConditions() {
        return conditionHandler.serializeConditions();
    }
}