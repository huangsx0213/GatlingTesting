package com.qa.app.ui.vm;

import com.qa.app.model.*;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.*;
import com.qa.app.service.impl.*;
import com.qa.app.ui.vm.gatling.TagHandler;
import com.qa.app.ui.vm.gatling.TemplateHandler;
import com.qa.app.ui.vm.gatling.TestCondictionHandler;
import com.qa.app.util.AppConfig;
import com.qa.app.util.HelpTooltipManager;
import com.qa.app.common.listeners.AppConfigChangeListener;
import com.qa.app.service.script.VariableGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import org.controlsfx.control.CheckComboBox;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.cell.TextFieldTableCell;
import com.qa.app.model.ResponseCheck;
import com.qa.app.model.CheckType;
import com.qa.app.model.Operator;
import javafx.util.converter.DefaultStringConverter;
import javafx.beans.property.SimpleStringProperty;
import com.qa.app.model.GatlingLoadParameters;
import com.qa.app.model.threadgroups.StandardThreadGroup;
import com.qa.app.model.threadgroups.ThreadGroupType;
import javafx.beans.binding.Bindings;
import com.qa.app.service.ProjectContext;
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
import javafx.scene.control.ComboBox;

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
    private TableColumn<DynamicVariable, Void> bodyDynamicActionColumn;
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
    private Button viewReportButton;
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
    private TableColumn<DynamicVariable, Void> headersTemplateActionColumn;
    @FXML
    private TableColumn<GatlingTest, String> headersTemplateNameColumn;
    @FXML
    private TableColumn<GatlingTest, String> bodyTemplateNameColumn;
    @FXML
    private FlowPane tagsFlowPane;
    @FXML
    private TextArea generatedHeadersArea;
    @FXML
    private TextArea generatedBodyArea;
    @FXML
    private Accordion testAccordion;
    @FXML
    private TitledPane apiConfigPane;

    @FXML
    private Label headersHelpIcon;

    @FXML
    private Label bodyHelpIcon;

    @FXML
    private Label responseHelpIcon;

    @FXML
    private Button responseHelpButton;

    // Response Checks UI Elements
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

    @FXML private ComboBox<String> suiteFilterCombo; // Filter dropdown
    @FXML private TextField tagFilterField;          // Tag keyword filter field
    @FXML private ComboBox<String> enabledFilterCombo; // Enabled status filter

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
    // Map for quick id-to-endpoint lookup
    private final Map<Integer, Endpoint> endpointIdMap = new HashMap<>();

    // New list for URL dynamic variables
    private final ObservableList<DynamicVariable> urlDynamicVariables = FXCollections.observableArrayList();

    // Response checks data list
    private final ObservableList<ResponseCheck> responseChecks = FXCollections.observableArrayList();

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
    private final Map<GatlingTest, BooleanProperty> selectionMap = new HashMap<>();
    private final CheckBox selectAllCheckBox = new CheckBox();

    private boolean isUpdatingSelection = false;
    
    private Tooltip variableTooltip;

    private Tooltip responseCheckTooltip;
    private Tooltip executionFlowTooltip;

    @FXML
    private Label runHelpIcon;

    private final ObjectMapper mapper = new ObjectMapper();

    // Clipboard for response checks
    private static final List<ResponseCheck> responseCheckClipboard = new ArrayList<>();

    // =====================
    // 1. Initialize related
    // =====================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupWaitTimeSpinner();

        // testIdField is hidden but used internally
        testIdField.setVisible(false);
        testIdField.setManaged(false);

        setupTemplateHandlers();

        setupConditionsTable();

        setupEndpointComboBox();

        // initialize suiteComboBox
        loadAllSuites();
        suiteComboBox.setEditable(true);

        // TagHandler initialization
        setupTagHandler();

        if(testAccordion!=null && apiConfigPane!=null){
            testAccordion.setExpandedPane(apiConfigPane);
        }

        setupResponseChecksTable();

        // 初始化启用状态过滤下拉
        if (enabledFilterCombo != null) {
            enabledFilterCombo.setItems(FXCollections.observableArrayList("All", "Enabled", "Disabled"));
            enabledFilterCombo.setValue("All");
        }
        
        loadEndpoints();
        loadAllTcids();
        loadTests();
        loadTemplates();
        loadHeadersTemplates();
        setupUrlDynamicVariablesHandler();

        if (viewReportButton != null) {
            viewReportButton.disableProperty().bind(
                testTable.getSelectionModel().selectedItemProperty().isNull()
                .or(Bindings.createBooleanBinding(() -> {
                    GatlingTest selected = testTable.getSelectionModel().getSelectedItem();
                    return selected == null || selected.getReportPath() == null || selected.getReportPath().isEmpty();
                }, testTable.getSelectionModel().selectedItemProperty()))
            );
        }

        AppConfig.addChangeListener(this);

        // Populate variable help
        List<Map<String, String>> varDefs = VariableGenerator.getInstance().getVariableDefinitions();
        StringBuilder helpText = new StringBuilder("可用变量:\n");
        for (Map<String, String> def : varDefs) {
            helpText.append(def.get("format")).append(" - ").append(def.get("description")).append("\n");
        }
        // This label seems to be missing from the FXML, but the code references it.
        // If variableHelpLabel is a valid FXML field, this will work. Otherwise, that's a separate bug.
        // variableHelpLabel.setText(helpText.toString());

        setupVariableHelpTooltips();
        setupResponseCheckHelpTooltip();
        setupRunHelpTooltip();
    }

    @Override
    public void onConfigChanged() {
        Platform.runLater(() -> {
            loadTests();
            loadEndpoints();
        });
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;

        setupTestTable();

        // Load initial data and templates
        loadTests();
        loadTemplates();
        loadHeadersTemplates();
        loadAllTcids();
    }

    private void setupWaitTimeSpinner() {
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
    }

    private void setupTemplateHandlers() {
        // Initialize TemplateHandler
        bodyTemplateHandler = new TemplateHandler(
                bodyTemplateComboBox, bodyTemplateVariables, bodyTemplates, bodyTemplateIdNameMap,
                bodyDynamicVarsTable, bodyDynamicKeyColumn, bodyDynamicValueColumn, bodyDynamicActionColumn, generatedBodyArea);
        headersTemplateHandler = new TemplateHandler(
                headersTemplateComboBox, headersTemplateVariables, headersTemplates, headersTemplateIdNameMap,
                headersTemplateVarsTable, headersTemplateKeyColumn, headersTemplateValueColumn, headersTemplateActionColumn, generatedHeadersArea);
        bodyTemplateHandler.setup();
        headersTemplateHandler.setup();
    }

    private void setupConditionsTable() {
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
    }

    private void setupEndpointComboBox() {
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

        // Listener to populate URL variables when endpoint changes
        endpointComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            populateUrlDynamicVariables(newVal);
            updateGeneratedUrl();
        });
    }

    private void setupTagHandler() {
        // TagHandler initialization
        tagHandler = new TagHandler(tagsFlowPane, tagsInputField);
        tagsInputField.setOnAction(e -> handleTagInput());
    }

    private void setupTestTable() {
        setupTestTableColumns();

        testTable.setItems(testList);
        testTable.setEditable(true);
        testTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setupTestTableListeners();
    }

    private void setupTestTableColumns() {
        // Selection column
        TableColumn<GatlingTest, Boolean> selectColumn = new TableColumn<>();
        selectColumn.setGraphic(selectAllCheckBox);
        selectColumn.setPrefWidth(40);
        selectColumn.setSortable(false);
        selectColumn.setCellValueFactory(cellData -> {
            GatlingTest test = cellData.getValue();
            return selectionMap.computeIfAbsent(test, k -> new SimpleBooleanProperty(false));
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        testTable.getColumns().add(0, selectColumn);

        // Initialize table columns
        isEnabledColumn.setCellValueFactory(new PropertyValueFactory<>("isEnabled"));
        isEnabledColumn.setCellFactory(column -> {
            TableCell<GatlingTest, Boolean> cell = new TableCell<>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item ? "Y" : "N");
                    }
                }
            };
            cell.setStyle("-fx-alignment: CENTER;");
            return cell;
        });
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
            int eid = cellData.getValue().getEndpointId();
            Endpoint ep = endpointIdMap.get(eid);
            String display = "";
            if (ep != null) {
                display = ep.getName() + " [ " + ep.getMethod() + " " + ep.getUrl() + " ]";
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
    }

    private void setupTestTableListeners() {
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

        // Listener for multi-selection changes to update checkboxes
        testTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<GatlingTest>) c -> {
            if (isUpdatingSelection) return;
            isUpdatingSelection = true;
            syncSelectionProperties();
            updateSelectAllCheckBoxState();
            isUpdatingSelection = false;
        });

        selectAllCheckBox.setOnAction(e -> {
            if (selectAllCheckBox.isSelected()) {
                testTable.getSelectionModel().selectAll();
            } else {
                testTable.getSelectionModel().clearSelection();
            }
            testTable.refresh();
        });
    }

    private void updateSelectAllCheckBoxState() {
        int selectedCount = testTable.getSelectionModel().getSelectedItems().size();
        if (selectedCount == 0) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
        } else if (selectedCount == testList.size() && !testList.isEmpty()) {
            selectAllCheckBox.setSelected(true);
            selectAllCheckBox.setIndeterminate(false);
        } else {
            selectAllCheckBox.setIndeterminate(true);
        }
    }

    /**
     * Ensure the BooleanProperty bound to each checkbox row reflects the actual selection model.
     */
    private void syncSelectionProperties() {
        isUpdatingSelection = true;
        for (GatlingTest t : testList) {
            BooleanProperty prop = selectionMap.get(t);
            if (prop != null) {
                boolean shouldBeSelected = testTable.getSelectionModel().getSelectedItems().contains(t);
                if (prop.get() != shouldBeSelected) {
                    prop.set(shouldBeSelected);
                }
            }
        }
        isUpdatingSelection = false;
    }

    // =====================
    // 2. Data loading related
    // =====================
    private void loadEndpoints() {
        endpointList.clear();
        endpointNameList.clear();
        endpointIdMap.clear();
        try {
            // get current environment
            String envName = AppConfig.getProperty("current.env", "dev");
            Integer envId = null;
            for (Environment env : environmentService.findAllEnvironments()) {
                if (env.getName().equals(envName)) {
                    envId = env.getId();
                    break;
                }
            }
            if (envId != null) {
                // only load endpoints in current environment
                for (Endpoint ep : endpointService.getAllEndpoints()) {
                    if (envId.equals(ep.getEnvironmentId())) {
                        endpointList.add(ep);
                        endpointIdMap.put(ep.getId(), ep);
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
            Integer projectId = ProjectContext.getCurrentProjectId();
            if (projectId != null) {
                List<GatlingTest> tests = testService.findTestsByProjectId(projectId);
                // Clear old selection states before loading new tests
                selectionMap.clear();
                testList.setAll(tests);
                // Initialize selection properties for the new tests
                for (GatlingTest test : tests) {
                    BooleanProperty selected = new SimpleBooleanProperty(false);
                    selected.addListener((obs, wasSelected, isSelected) -> {
                        if (isUpdatingSelection) return;
                        if (isSelected) {
                            testTable.getSelectionModel().select(test);
                        } else {
                            testTable.getSelectionModel().clearSelection(testList.indexOf(test));
                        }
                    });
                    selectionMap.put(test, selected);
                }
                updateSuiteFilterOptions();
                updateSelectAllCheckBoxState();
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
        conditionHandler.getConditionRows().clear();
        updateGeneratedBody();
        updateGeneratedHeaders();

        // Clear response checks
        responseChecks.clear();
        ensureDefaultStatusCheck();
        if (responseChecksTable != null) {
            responseChecksTable.refresh();
        }

        urlDynamicVariables.clear();
        generatedUrlArea.setText("");
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
            expResultArea.setText(test.getResponseChecks());
            String display = null;
            Endpoint epFound = endpointIdMap.get(test.getEndpointId());
            if (epFound != null) {
                display = epFound.getName() + " [ " + epFound.getMethod() + " " + epFound.getUrl() + " ]";
            }
            endpointComboBox.setValue(display);
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

            // Load response checks from test.responseChecks JSON list
            responseChecks.clear();
            if(test.getResponseChecks()!=null && !test.getResponseChecks().isEmpty()){
                try{
                    java.util.List<ResponseCheck> list = mapper.readValue(test.getResponseChecks(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ResponseCheck>>(){});
                    responseChecks.addAll(list);
                }catch(Exception ignored){}
            }
            // Ensure default status row
            ensureDefaultStatusCheck();

            // Populate URL dynamic variables based on endpoint URL and saved values
            populateUrlDynamicVariables(display);
            if(test!=null){
                Map<String,String> saved = test.getEndpointDynamicVariables();
                for (DynamicVariable dv : urlDynamicVariables) {
                    if (saved!=null && saved.containsKey(dv.getKey())) {
                        dv.setValue(saved.get(dv.getKey()));
                    }
                }
            }
            updateGeneratedUrl();

            updateGeneratedBody();
            updateGeneratedHeaders();
        } else {
            clearFields();
        }
    }

    private void populateTestFromFields(GatlingTest test) {
        String suite = suiteComboBox.getEditor().getText().trim();
        String tcid = tcidField.getText().trim();
        String descriptions = descriptionsArea.getText().trim();
        String endpointDisplay = endpointComboBox.getValue();
        String endpointName = endpointDisplay == null ? "" : endpointDisplay.split(" \\[")[0].trim();

        // Resolve endpoint ID
        int endpointId = 0;
        for (Endpoint ep : endpointList) {
            if (ep.getName().equals(endpointName)) { endpointId = ep.getId(); break; }
        }

        test.setEnabled(isEnabledCheckBox.isSelected());
        test.setSuite(suite);
        test.setTcid(tcid);
        test.setDescriptions(descriptions);
        test.setConditions(serializeConditions());
        test.setResponseChecks(expResultArea.getText());
        test.setEndpointId(endpointId);
        // keep name for UI convenience (not persisted)
        test.setEndpointName(endpointName);
        test.setBodyTemplateId(getBodyTemplateIdByName(bodyTemplateComboBox.getSelectionModel().getSelectedItem()));
        test.setHeadersTemplateId(
                getHeadersTemplateIdByName(headersTemplateComboBox.getSelectionModel().getSelectedItem()));
        test.setTags(getTagsString());
        test.setWaitTime(waitTimeSpinner.getValue());

        Map<String, String> bodyVars = new HashMap<>();
        bodyTemplateVariables.forEach(dv -> bodyVars.put(dv.getKey(), dv.getValue()));
        test.setDynamicVariables(bodyVars);

        Map<String, String> headersVars = new HashMap<>();
        headersTemplateVariables.forEach(dv -> headersVars.put(dv.getKey(), dv.getValue()));
        test.setHeadersDynamicVariables(headersVars);
        
        Map<String, String> urlVars = new HashMap<>();
        urlDynamicVariables.forEach(dv -> urlVars.put(dv.getKey(), dv.getValue()));
        test.setEndpointDynamicVariables(urlVars);

        // Store raw templates for runtime rendering
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

        // Serialize response checks to JSON
        try{
            String json = mapper.writeValueAsString(responseChecks);
            test.setResponseChecks(json);
        }catch(Exception e){
            test.setResponseChecks(null);
        }

        // new suite added to dropdown
        if (suite != null && !suite.isEmpty() && !suiteComboBox.getItems().contains(suite)) {
            suiteComboBox.getItems().add(suite);
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
        GatlingTest newTest = new GatlingTest(suite, tcid, descriptions, endpointName, ProjectContext.getCurrentProjectId());
        populateTestFromFields(newTest);

        try {
            testService.createTest(newTest);

            BooleanProperty selected = new SimpleBooleanProperty(false);
            selected.addListener((obs, wasSel, isSel) -> {
                if (isUpdatingSelection) return;
                if (isSel) {
                    testTable.getSelectionModel().select(newTest);
                } else {
                    testTable.getSelectionModel().clearSelection(testList.indexOf(newTest));
                }
            });
            selectionMap.put(newTest, selected);

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
        String tcid = tcidField.getText().trim();
        if (tcid.isEmpty()) {
             if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: TCID is required.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }
        populateTestFromFields(selectedTest);

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
    private void handleDuplicateTest() {
        final GatlingTest selectedTest = testTable.getSelectionModel().getSelectedItem();
        if (selectedTest == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Please select a test to duplicate.", MainViewModel.StatusType.ERROR);
            }
            return;
        }

        try {
            // Create a new test object as a copy
            GatlingTest duplicate = new GatlingTest(selectedTest);
            
            // Add it to the database
            testService.createTest(duplicate);
            
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Test " + selectedTest.getTcid() + " duplicated successfully as " + duplicate.getTcid(), MainViewModel.StatusType.SUCCESS);
            }

            // Refresh the view
            refreshAll();

        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Error duplicating test: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteTest() {
        ObservableList<GatlingTest> selectedTests = testTable.getSelectionModel().getSelectedItems();
        if (selectedTests.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select at least one test from the table to delete.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }

        List<GatlingTest> testsToDelete = new ArrayList<>(selectedTests);

        try {
            for (GatlingTest test : testsToDelete) {
                testService.removeTest(test.getId());
                selectionMap.remove(test);
            }
            testList.removeAll(testsToDelete);
            clearFields();
            if (mainViewModel != null) {
                mainViewModel.updateStatus(testsToDelete.size() + " test(s) deleted successfully.", MainViewModel.StatusType.SUCCESS);
            }
            refreshAll();
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to delete test(s): " + e.getMessage(), MainViewModel.StatusType.ERROR);
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
        ObservableList<GatlingTest> selectedTests = testTable.getSelectionModel().getSelectedItems();
        if (selectedTests.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select at least one test to run.",
                        MainViewModel.StatusType.ERROR);
            }
            return;
        }

        // Make a stable copy of the selection to avoid UI side-effects
        List<GatlingTest> testsToRun = new ArrayList<>(selectedTests);

        // Persist current form edits back to the focused test (if part of the run set)
        GatlingTest focused = testTable.getSelectionModel().getSelectedItem();
        if (focused != null && testsToRun.contains(focused)) {
            populateTestFromFields(focused);
        }

        // Prepare minimal load profile (single user by default)
        GatlingLoadParameters params = new GatlingLoadParameters();
        params.setType(ThreadGroupType.STANDARD);
        StandardThreadGroup tg = new StandardThreadGroup();
        tg.setNumThreads(1);
        tg.setRampUp(0);
        tg.setLoops(1);
        tg.setScheduler(false);
        tg.setDelay(0);
        params.setStandardThreadGroup(tg);

        // Notify UI + disable Run button to prevent duplicate clicks
        if (mainViewModel != null) {
            mainViewModel.updateStatus("Running " + testsToRun.size() + " Gatling test(s)...", MainViewModel.StatusType.INFO);
        }
        // Disable button
        runTestButton.setDisable(true);

        Runnable onComplete = () -> {
            // Re-enable Run button and refresh data on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                runTestButton.setDisable(false);
                loadTests();
                testTable.refresh();
                if (mainViewModel != null) {
                    mainViewModel.updateStatus("Gatling test(s) completed.", MainViewModel.StatusType.SUCCESS);
                }
            });
        };

        try {
            testService.runTests(testsToRun, params, onComplete);
        } catch (ServiceException ex) {
            runTestButton.setDisable(false);
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to run test(s): " + ex.getMessage(),
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
        return bodyTemplateHandler.buildContent();
    }

    private String buildHeaders() {
        return headersTemplateHandler.buildContent();
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

    public void refreshDynamicVariables() {
        VariableGenerator.getInstance().reloadCustomVariables();
        loadTemplates();
        loadHeadersTemplates();
        // refresh dynamic variables table
        if (bodyDynamicVarsTable != null) bodyDynamicVarsTable.refresh();
        if (headersTemplateVarsTable != null) headersTemplateVarsTable.refresh();
        if (bodyTemplateComboBox != null) bodyTemplateComboBox.setItems(FXCollections.observableArrayList(bodyTemplateIdNameMap.values()));
        if (headersTemplateComboBox != null) headersTemplateComboBox.setItems(FXCollections.observableArrayList(headersTemplateIdNameMap.values()));
    }

    // =====================
    // Response Check event handling
    // =====================
    @FXML
    private void handleAddResponseCheck() {
        ResponseCheck newCheck = new ResponseCheck();
        responseChecks.add(newCheck);
        responseChecksTable.getSelectionModel().clearSelection();
        responseChecksTable.getSelectionModel().select(newCheck);
        responseChecksTable.scrollTo(newCheck);
    }

    @FXML
    private void handleRemoveResponseCheck() {
        List<ResponseCheck> selected = new ArrayList<>(responseChecksTable.getSelectionModel().getSelectedItems());
        if (!selected.isEmpty()) {
            responseChecks.removeAll(selected);
        }
        ensureDefaultStatusCheck();
    }

    @FXML
    private void handleCopyResponseCheck() {
        List<ResponseCheck> selected = responseChecksTable.getSelectionModel().getSelectedItems();
        if (!selected.isEmpty()) {
            responseCheckClipboard.clear();
            for (ResponseCheck rc : selected) {
                responseCheckClipboard.add(new ResponseCheck(rc));
            }
            // Optional: provide feedback to the user
            if (mainViewModel != null) {
                mainViewModel.updateStatus(selected.size() + " check(s) copied to clipboard.", MainViewModel.StatusType.INFO);
            }
        }
    }

    @FXML
    private void handlePasteResponseCheck() {
        if (!responseCheckClipboard.isEmpty()) {
            for (ResponseCheck rc : responseCheckClipboard) {
                responseChecks.add(new ResponseCheck(rc)); // Add a new copy
            }
            // Optional: provide feedback to the user
            if (mainViewModel != null) {
                mainViewModel.updateStatus(responseCheckClipboard.size() + " check(s) pasted.", MainViewModel.StatusType.INFO);
            }
        }
    }

    private void setupResponseChecksTable(){
        // Allow multiple selections
        responseChecksTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if(responseChecksTable==null) return;
        responseChecksTable.setItems(responseChecks);

        // Type column
        checkTypeColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getType()));
        checkTypeColumn.setCellFactory(col -> new ComboBoxTableCell<ResponseCheck, CheckType>() {
            {
                getItems().addAll(CheckType.values());
            }
            @Override
            public void updateItem(CheckType item, boolean empty) {
                super.updateItem(item, empty);
                int row = getIndex();
                setEditable(!(row == 0));
            }
        });
        checkTypeColumn.setOnEditCommit(e -> e.getRowValue().setType(e.getNewValue()));

        // Expression column
        checkExpressionColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getExpression()));
        checkExpressionColumn.setCellFactory(column -> new TextFieldTableCell<ResponseCheck, String>(new DefaultStringConverter()) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                int row = getIndex();
                setEditable(!(row == 0));
            }
        });
        checkExpressionColumn.setOnEditCommit(e -> e.getRowValue().setExpression(e.getNewValue()));

        // Operator column
        checkOperatorColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getOperator()));
        checkOperatorColumn.setCellFactory(col -> new ComboBoxTableCell<ResponseCheck, Operator>() {
            {
                getItems().addAll(Operator.values());
            }
            @Override
            public void updateItem(Operator item, boolean empty) {
                super.updateItem(item, empty);
                int row = getIndex();
                setEditable(!(row == 0));
            }
        });
        checkOperatorColumn.setOnEditCommit(e -> e.getRowValue().setOperator(e.getNewValue()));

        // Expect column
        checkExpectColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getExpect()));
        checkExpectColumn.setCellFactory(column -> new TextFieldTableCell<>(new DefaultStringConverter()));
        checkExpectColumn.setOnEditCommit(event -> event.getRowValue().setExpect(event.getNewValue()));

        checkSaveAsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSaveAs()));
        checkSaveAsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        checkSaveAsColumn.setOnEditCommit(event -> event.getRowValue().setSaveAs(event.getNewValue()));

        responseChecksTable.setEditable(true);

        // ensure at least status row exists
        ensureDefaultStatusCheck();
    }

    private void ensureDefaultStatusCheck(){
        if(responseChecks.stream().noneMatch(rc -> rc.getType()==CheckType.STATUS)){
            String defaultStatus = "200";
            responseChecks.add(0,new ResponseCheck(CheckType.STATUS,"", Operator.IS, defaultStatus, null));
        }
    }

    private void updateSuiteFilterOptions() {
        if (suiteFilterCombo == null) return;
        List<String> suites = testList.stream()
                .map(GatlingTest::getSuite)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
        suites.add(0, "All");
        suiteFilterCombo.setItems(FXCollections.observableArrayList(suites));
    }

    @FXML
    private void handleFilterTests(javafx.event.ActionEvent evt) {
        if (suiteFilterCombo == null || tagFilterField == null) return;
        String suite = suiteFilterCombo.getValue();
        String tagKw = tagFilterField.getText();
        String enabledOpt = enabledFilterCombo == null ? null : enabledFilterCombo.getValue();
        loadTests(); // reload all tests first
        testList.removeIf(t -> {
            boolean ok = true;
            if (suite != null && !"All".equals(suite) && !suite.isBlank()) {
                ok = ok && suite.equals(t.getSuite());
            }
            if (ok && tagKw != null && !tagKw.isBlank()) {
                ok = t.getTags() != null && t.getTags().contains(tagKw);
            }
            if (ok && enabledOpt != null && !"All".equals(enabledOpt)) {
                if ("Enabled".equals(enabledOpt)) {
                    ok = t.isEnabled();
                } else if ("Disabled".equals(enabledOpt)) {
                    ok = !t.isEnabled();
                }
            }
            return !ok;
        });
        updateSuiteFilterOptions();
        updateSelectAllCheckBoxState();
    }

    @FXML
    private void handleResetFilter(javafx.event.ActionEvent evt) {
        if (suiteFilterCombo != null) suiteFilterCombo.setValue("All");
        if (tagFilterField != null) tagFilterField.clear();
        if (enabledFilterCombo != null) enabledFilterCombo.setValue("All");
        loadTests();
        updateSuiteFilterOptions();
        updateSelectAllCheckBoxState();
    }

    private void setupUrlDynamicVariablesHandler() {
        if (urlDynamicVarsTable == null) return; // FXML not loaded (unit tests)
        urlDynamicKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        urlDynamicValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        urlDynamicVarsTable.setItems(urlDynamicVariables);
        urlDynamicVarsTable.setEditable(true);

        // Value column with ComboBox suggestions (reuse VariableGenerator rules)
        urlDynamicValueColumn.setCellFactory(col -> {
            List<String> suggestions = VariableGenerator.getInstance().getVariableDefinitions().stream()
                .map(def -> def.get("format"))
                .collect(Collectors.toList());
            ComboBoxTableCell<DynamicVariable, String> cell = new ComboBoxTableCell<>(
                    new DefaultStringConverter(),
                    FXCollections.observableArrayList(suggestions));
            cell.setComboBoxEditable(true);
            return cell;
        });
        urlDynamicValueColumn.setOnEditCommit(e -> {
            e.getRowValue().setValue(e.getNewValue());
            updateGeneratedUrl();
        });

        // Optional action column (Edit dialog for long text)
        if (urlDynamicActionColumn != null) {
            urlDynamicActionColumn.setSortable(false);
            urlDynamicActionColumn.setCellFactory(col -> new TableCell<>() {
                private final Button editBtn = new Button("Edit");
                {
                    editBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
                    editBtn.setMaxWidth(Double.MAX_VALUE);
                    editBtn.setOnAction(evt -> {
                        DynamicVariable var = getTableView().getItems().get(getIndex());
                        String edited = showLargeTextEditDialog(var.getKey(), var.getValue());
                        if (edited != null) {
                            var.setValue(edited);
                            updateGeneratedUrl();
                        }
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(editBtn);
                    }
                }
            });
        }

        urlDynamicVariables.addListener((javafx.collections.ListChangeListener<DynamicVariable>) c -> updateGeneratedUrl());
    }

    private void populateUrlDynamicVariables(String endpointDisplay) {
        urlDynamicVariables.clear();
        if (endpointDisplay == null || endpointDisplay.isBlank()) return;
        String endpointName = endpointDisplay.split(" \\[")[0].trim();
        Endpoint ep = endpointList.stream().filter(e -> e.getName().equals(endpointName)).findFirst().orElse(null);
        if (ep == null || ep.getUrl() == null) return;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(ep.getUrl());
        java.util.Set<String> found = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            found.add(matcher.group(1).trim());
        }
        for (String varName : found) {
            urlDynamicVariables.add(new DynamicVariable(varName, ""));
        }
    }

    private String buildGeneratedUrl() {
        String endpointDisplay = endpointComboBox == null ? null : endpointComboBox.getValue();
        Endpoint ep = getEndpointByName(endpointDisplay);
        if (ep == null || ep.getUrl() == null) return "";

        String url = ep.getUrl();
        for (DynamicVariable dv : urlDynamicVariables) {
            String value = VariableGenerator.getInstance().resolveVariables(dv.getValue());
            if (value == null) value = "";
            url = url.replaceAll("@\\{" + java.util.regex.Pattern.quote(dv.getKey()) + "\\}", java.util.regex.Matcher.quoteReplacement(value));
        }
        return url;
    }

    private void updateGeneratedUrl() {
        if (generatedUrlArea != null) {
            String u = buildGeneratedUrl();
            generatedUrlArea.setText(u == null ? "" : u);
        }
    }

    // Helper method (copied from TemplateHandler)
    private String showLargeTextEditDialog(String key, String initialValue) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Value - " + key);
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        TextArea textArea = new TextArea(initialValue);
        textArea.setWrapText(true);
        textArea.setPrefSize(400, 300);
        dialog.getDialogPane().setContent(textArea);
        dialog.setResizable(true);
        dialog.setResultConverter(btn -> btn == okButtonType ? textArea.getText() : null);
        java.util.Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private Endpoint getEndpointByName(String displayString) {
        if (displayString == null || displayString.isBlank()) {
            return null;
        }
        String endpointName = displayString.split(" \\[")[0].trim();
        return endpointList.stream()
                .filter(e -> e.getName().equals(endpointName))
                .findFirst()
                .orElse(null);
    }

    private void setupVariableHelpTooltips() {
        variableTooltip = HelpTooltipManager.buildVariableTooltip();
        headersHelpIcon.setOnMouseClicked(event -> toggleTooltip(variableTooltip, headersHelpIcon));
        bodyHelpIcon.setOnMouseClicked(event -> toggleTooltip(variableTooltip, bodyHelpIcon));
    }

    private void setupResponseCheckHelpTooltip() {
        responseCheckTooltip = HelpTooltipManager.getResponseCheckTooltip();
        // responseHelpIcon has been removed; use button instead.
    }

    private void setupRunHelpTooltip() {
        executionFlowTooltip = HelpTooltipManager.getExecutionFlowTooltip();
        if (runHelpIcon != null) {
            runHelpIcon.setOnMouseClicked(e -> toggleTooltip(executionFlowTooltip, runHelpIcon));
        }
    }

    private void toggleTooltip(Tooltip tooltip, Node ownerNode) {
        if (tooltip.isShowing()) {
            tooltip.hide();
        } else {
            Point2D p = ownerNode.localToScreen(ownerNode.getBoundsInLocal().getMaxX(), ownerNode.getBoundsInLocal().getMaxY());
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
                    responseHelpButton.getBoundsInLocal().getMinY()
                );
                responseCheckTooltip.show(responseHelpButton, p.getX(), p.getY());
            }
        }
    }
}