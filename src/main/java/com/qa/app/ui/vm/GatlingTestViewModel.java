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
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
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
import com.qa.app.service.impl.EndpointServiceImpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private ComboBox<Endpoint> endpointComboBox;
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

    private MainViewModel mainViewModel;
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final ObservableList<ConditionRow> conditionRows = FXCollections.observableArrayList();
    private final ObservableList<String> allTcids = FXCollections.observableArrayList();
    private final ObservableList<String> prefixOptions = FXCollections.observableArrayList("Setup", "Teardown", "SuiteSetup", "SuiteTeardown", "CheckWith");

    // 用于保存所有TCID下拉的引用
    private final List<CheckComboBox<String>> conditionTcidComboBoxes = new ArrayList<>();

    // 1. 新增 Map<Integer, String> bodyTemplateIdNameMap, headersTemplateIdNameMap
    private Map<Integer, String> bodyTemplateIdNameMap = new HashMap<>();
    private Map<Integer, String> headersTemplateIdNameMap = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize spinner
        waitTimeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3600, 0));
        waitTimeSpinner.setEditable(true);
        // 只允许输入数字
        waitTimeSpinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                waitTimeSpinner.getEditor().setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        // 失去焦点时同步 Spinner 值
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

        // Only dynamicValueColumn should be editable
        bodyDynamicValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        bodyDynamicValueColumn.setOnEditCommit(event -> {
            DynamicVariable editedVar = event.getTableView().getItems().get(event.getTablePosition().getRow());
            editedVar.setValue(event.getNewValue());
            GatlingTest selectedTest = testTable.getSelectionModel().getSelectedItem();
            if (selectedTest != null) {
                selectedTest.getBodyDynamicVariables().put(editedVar.getKey(), editedVar.getValue());
            }
        });

        // Add listener for template selection
        bodyTemplateComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        populateDynamicVariables(bodyTemplates.get(newValue));
                    } else {
                        bodyTemplateVariables.clear();
                    }
                    updateGeneratedBody();
                });
        bodyTemplateVariables.addListener((javafx.collections.ListChangeListener<DynamicVariable>) c -> updateGeneratedBody());

        // Initialize headers dynamic variables table
        headersTemplateKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        headersTemplateValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        headersTemplateVarsTable.setItems(headersTemplateVariables);
        headersTemplateVarsTable.setEditable(true);
        headersTemplateValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        headersTemplateValueColumn.setOnEditCommit(event -> {
            DynamicVariable editedVar = event.getTableView().getItems().get(event.getTablePosition().getRow());
            editedVar.setValue(event.getNewValue());
        });

        // Add listener for headersTemplateComboBox selection
        headersTemplateComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        populateHeadersTemplateVariables(headersTemplates.get(newValue));
                    } else {
                        headersTemplateVariables.clear();
                    }
                    updateGeneratedHeaders();
                });
        headersTemplateVariables.addListener((javafx.collections.ListChangeListener<DynamicVariable>) c -> updateGeneratedHeaders());
        // set prompt text for templateComboBox
        bodyTemplateComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(bodyTemplateComboBox.getPromptText());
                } else {
                    setText(item);
                }
            }
        });
        // set prompt text for headersTemplateComboBox
        headersTemplateComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(headersTemplateComboBox.getPromptText());
                } else {
                    setText(item);
                }
            }
        });

        expStatusComboBox.setItems(FXCollections.observableArrayList(
            "200", "400", "401", "403", "404", "408", "500","501", "503","504"
        ));
        expStatusComboBox.setValue("200");

        // --- Test Conditions Table ---
        conditionsTable.setItems(conditionRows);
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
                    // 先清空再加最新的allTcids
                    checkComboBox.getItems().setAll(allTcids);
                    checkComboBox.setPrefWidth(180);
                    checkComboBox.setMaxWidth(180);
                    checkComboBox.setMinWidth(120);
                    checkComboBox.setStyle("-fx-alignment: CENTER_LEFT;");
                    checkComboBox.getCheckModel().clearChecks();
                    for (String t : row.getTcids()) {
                        checkComboBox.getCheckModel().check(t);
                    }
                    checkComboBox.getCheckModel().getCheckedItems().addListener((javafx.collections.ListChangeListener<String>) c -> {
                        row.setTcids(FXCollections.observableArrayList(checkComboBox.getCheckModel().getCheckedItems()));
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

        endpointComboBox.setItems(endpointList);
        endpointComboBox.setConverter(new javafx.util.StringConverter<Endpoint>() {
            @Override
            public String toString(Endpoint endpoint) {
                return endpoint == null ? "" : endpoint.getName() + " [ " + endpoint.getMethod() + " " + endpoint.getUrl() + " ]";
            }
            @Override
            public Endpoint fromString(String s) {
                return endpointList.stream().filter(e -> (e.getName() + " [ " + e.getMethod() + " " + e.getUrl() + " ]").equals(s)).findFirst().orElse(null);
            }
        });
        endpointComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Endpoint item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(endpointComboBox.getPromptText());
                } else {
                    setText(item.getName() + " [ " + item.getMethod() + " " + item.getUrl() + " ]");
                }
            }
        });
        loadEndpoints();

        // 初始化suiteComboBox
        loadAllSuites();
        suiteComboBox.setEditable(true);
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
                    if (item.length() > 20) { // 超过20字符才显示tooltip，可根据需要调整
                        tooltip.setText(item);
                        setTooltip(tooltip);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });
        endpointColumn.setCellValueFactory(cellData -> {
            int endpointId = cellData.getValue().getEndpointId();
            Endpoint ep = endpointList.stream().filter(e -> e.getId() == endpointId).findFirst().orElse(null);
            return new javafx.beans.property.SimpleStringProperty(ep == null ? "" : ep.getName());
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

        // suiteComboBox自动补全和回填
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

    private void populateDynamicVariables(String template) {
        bodyTemplateVariables.clear();
        if (template != null) {
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(template);
            while (matcher.find()) {
                bodyTemplateVariables.add(new DynamicVariable(matcher.group(1), ""));
            }
        }
    }

    private void populateHeadersTemplateVariables(String template) {
        headersTemplateVariables.clear();
        if (template != null) {
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(template);
            while (matcher.find()) {
                headersTemplateVariables.add(new DynamicVariable(matcher.group(1), ""));
            }
        }
    }

    private void loadTests() {
        try {
            testList.setAll(testService.findAllTests());
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load tests: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void loadAllTcids() {
        try {
            allTcids.clear();
            for (GatlingTest t : testService.findAllTests()) {
                allTcids.add(t.getTcid());
            }
            // 通知所有CheckComboBox刷新items
            for (CheckComboBox<String> cb : conditionTcidComboBoxes) {
                cb.getItems().setAll(allTcids);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load TCIDs: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void showTestDetails(GatlingTest test) {
        if (test != null) {
            testIdField.setText(String.valueOf(test.getId()));
            isEnabledCheckBox.setSelected(test.isEnabled());
            suiteComboBox.setValue(test.getSuite());
            tcidField.setText(test.getTcid());
            descriptionsArea.setText(test.getDescriptions());
            deserializeConditions(test.getConditions());
            expResultArea.setText(test.getExpResult());
            saveFieldsArea.setText(test.getSaveFields());
            Endpoint selectedEp = endpointList.stream().filter(e -> e.getId() == test.getEndpointId()).findFirst().orElse(null);
            endpointComboBox.setValue(selectedEp);
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
            tags.clear();
            if (test.getTags() != null && !test.getTags().isEmpty()) {
                for (String tag : test.getTags().split(",")) {
                    if (!tag.trim().isEmpty()) tags.add(tag.trim());
                }
            }
            updateTagsFlowPane();
            waitTimeSpinner.getValueFactory().setValue(test.getWaitTime());
            expStatusComboBox.setValue(test.getExpStatus() == null || test.getExpStatus().isEmpty() ? "200" : test.getExpStatus());
            updateGeneratedBody();
            updateGeneratedHeaders();
        } else {
            clearFields();
        }
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
        tags.clear();
        updateTagsFlowPane();
        waitTimeSpinner.getValueFactory().setValue(0);
        expStatusComboBox.setValue("200");
        conditionRows.clear();
        updateGeneratedBody();
        updateGeneratedHeaders();
    }

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

    @FXML
    private void handleAddTest() {
        String suite = suiteComboBox.getEditor().getText().trim();
        String tcid = tcidField.getText().trim();
        String descriptions = descriptionsArea.getText().trim();
        Endpoint endpoint = endpointComboBox.getValue();
        if (suite.isEmpty() || tcid.isEmpty() || endpoint == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Suite, TCID, and Endpoint are required.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }
        GatlingTest newTest = new GatlingTest(suite, tcid, descriptions, endpoint.getId());
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
        newTest.setHeadersTemplateId(getHeadersTemplateIdByName(headersTemplateComboBox.getSelectionModel().getSelectedItem()));
        newTest.setTags(getTagsString());
        newTest.setWaitTime(waitTimeSpinner.getValue());
        newTest.setExpStatus(expStatusComboBox.getValue());
        newTest.setEndpointId(endpoint.getId());
        Map<String, String> headersVars = new HashMap<>();
        headersTemplateVariables.forEach(dv -> headersVars.put(dv.getKey(), dv.getValue()));
        newTest.setHeadersDynamicVariables(headersVars);

        // 新suite自动加入下拉
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
            loadAllTcids();
            conditionsTable.refresh();
            updateGeneratedBody();
            updateGeneratedHeaders();
            loadAllSuites();
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
        Endpoint endpoint = endpointComboBox.getValue();
        if (suite.isEmpty() || tcid.isEmpty() || endpoint == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Suite, TCID, and Endpoint are required.",
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
        selectedTest.setEndpointId(endpoint.getId());
        selectedTest.setHeaders(headersTemplateComboBox.getSelectionModel().getSelectedItem());
        Map<String, String> vars = new HashMap<>();
        bodyTemplateVariables.forEach(dv -> vars.put(dv.getKey(), dv.getValue()));
        selectedTest.setDynamicVariables(vars);
        selectedTest.setBody(buildRequestBody());
        selectedTest.setBodyTemplateId(getBodyTemplateIdByName(bodyTemplateComboBox.getSelectionModel().getSelectedItem()));
        selectedTest.setHeaders(buildHeaders());
        selectedTest.setHeadersTemplateId(getHeadersTemplateIdByName(headersTemplateComboBox.getSelectionModel().getSelectedItem()));
        selectedTest.setTags(getTagsString());
        selectedTest.setWaitTime(waitTimeSpinner.getValue());
        selectedTest.setExpStatus(expStatusComboBox.getValue());
        Map<String, String> headersVars = new HashMap<>();
        headersTemplateVariables.forEach(dv -> headersVars.put(dv.getKey(), dv.getValue()));
        selectedTest.setHeadersDynamicVariables(headersVars);

        // 新suite自动加入下拉
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
            loadAllTcids();
            conditionsTable.refresh();
            updateGeneratedBody();
            updateGeneratedHeaders();
            loadAllSuites();
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
            loadAllTcids();
            conditionsTable.refresh();
            updateGeneratedBody();
            updateGeneratedHeaders();
            loadAllSuites();
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
            loadTests();
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
        if (!tag.isEmpty() && !tags.contains(tag)) {
            tags.add(tag);
            updateTagsFlowPane();
            tagsInputField.clear();
        }
    }

    private void addTagChip(String tag) {
        HBox chip = new HBox();
        chip.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 4 8; -fx-border-radius: 4; -fx-background-radius: 4; -fx-alignment: center;");
        Label label = new Label(tag);
        label.setStyle("-fx-font-size: 12px;");
        Label close = new Label("  ×");
        close.setStyle("-fx-text-fill: #888; -fx-cursor: hand; -fx-font-size: 14px;");
        close.setOnMouseClicked(e -> {
            tags.remove(tag);
            updateTagsFlowPane();
        });
        chip.getChildren().addAll(label, close);
        tagsFlowPane.getChildren().add(chip);
    }

    private void updateTagsFlowPane() {
        tagsFlowPane.getChildren().clear();
        for (String tag : tags) {
            addTagChip(tag);
        }
        tagsFlowPane.getChildren().add(tagsInputField);
    }

    private String getTagsString() {
        return String.join(",", tags);
    }

    private String serializeConditions() {
        // [prefix]TC01,TC02;[prefix]TC01
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

    private void deserializeConditions(String conditions) {
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

    @FXML
    private void handleAddCondition() {
        conditionRows.add(new ConditionRow("Setup", FXCollections.observableArrayList()));
    }

    @FXML
    private void handleRemoveCondition() {
        int idx = conditionsTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            conditionRows.remove(idx);
        }
    }

    private void loadEndpoints() {
        endpointList.clear();
        try {
            endpointList.addAll(endpointService.getAllEndpoints());
        } catch (Exception e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load endpoints: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    public void refresh() {
        loadEndpoints();
        loadAllTcids();
        loadTests();
        loadTemplates();
        loadHeadersTemplates();
        clearFields();
        conditionsTable.refresh();
    }

    private int getBodyTemplateIdByName(String name) {
        for (Map.Entry<Integer, String> entry : bodyTemplateIdNameMap.entrySet()) {
            if (entry.getValue().equals(name)) return entry.getKey();
        }
        return 0;
    }

    private int getHeadersTemplateIdByName(String name) {
        for (Map.Entry<Integer, String> entry : headersTemplateIdNameMap.entrySet()) {
            if (entry.getValue().equals(name)) return entry.getKey();
        }
        return 0;
    }

    private void updateGeneratedBody() {
        String body = buildRequestBody();
        generatedBodyArea.setText(body == null ? "" : body);
    }

    private void updateGeneratedHeaders() {
        String headers = buildHeaders();
        generatedHeadersArea.setText(headers == null ? "" : headers);
    }

    private void loadAllSuites() {
        // 从testService获取所有suite去重后加入ComboBox
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
}