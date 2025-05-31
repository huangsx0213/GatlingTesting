package com.example.app.ui.vm;

import com.example.app.model.BodyTemplate;
import com.example.app.model.DynamicVariable;
import com.example.app.model.GatlingTest;
import com.example.app.service.impl.BodyTemplateServiceImpl;
import com.example.app.service.impl.GatlingTestServiceImpl;
import com.example.app.service.api.IBodyTemplateService;
import com.example.app.service.api.IGatlingTestService;
import com.example.app.service.ServiceException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GatlingTestViewModel implements Initializable {

    @FXML
    private TextField testIdField;
    @FXML
    private CheckBox isRunCheckBox;
    @FXML
    private TextField suiteField;
    @FXML
    private TextField tcidField;
    @FXML
    private TextArea descriptionsArea;
    @FXML
    private TextArea conditionsArea;
    @FXML
    private TextArea bodyOverrideArea;
    @FXML
    private TextField expStatusField;
    @FXML
    private TextArea expResultArea;
    @FXML
    private TextArea saveFieldsArea;
    @FXML
    private TextField endpointField;
    @FXML
    private TextArea headersArea;
    @FXML
    private ComboBox<String> templateComboBox;
    @FXML
    private TableView<DynamicVariable> dynamicVarsTable;
    @FXML
    private TableColumn<DynamicVariable, String> dynamicKeyColumn;
    @FXML
    private TableColumn<DynamicVariable, String> dynamicValueColumn;
    @FXML
    private TextField tagsField;
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
    private TableColumn<GatlingTest, Integer> idColumn;
    @FXML
    private TableColumn<GatlingTest, Boolean> isRunColumn;
    @FXML
    private TableColumn<GatlingTest, String> suiteColumn;
    @FXML
    private TableColumn<GatlingTest, String> tcidColumn;
    @FXML
    private TableColumn<GatlingTest, String> descriptionsColumn;
    @FXML
    private TableColumn<GatlingTest, String> endpointColumn;
    @FXML
    private TableColumn<GatlingTest, String> tagsColumn;
    @FXML
    private TableColumn<GatlingTest, Integer> waitTimeColumn;

    private final IGatlingTestService testService = new GatlingTestServiceImpl();
    private final IBodyTemplateService bodyTemplateService = new BodyTemplateServiceImpl();
    private final ObservableList<GatlingTest> testList = FXCollections.observableArrayList();
    private final ObservableList<DynamicVariable> dynamicVariables = FXCollections.observableArrayList();
    private Map<String, String> bodyTemplates = new HashMap<>();

    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize spinner
        waitTimeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3600, 0));
        
        // testIdField is hidden but used internally
        testIdField.setVisible(false);
        testIdField.setManaged(false);

        // Initialize dynamic variables table
            // Initialize dynamic variables table
            dynamicKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
            dynamicValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
            dynamicVarsTable.setItems(dynamicVariables);
            dynamicVarsTable.setEditable(true);

            // Only dynamicValueColumn should be editable
            dynamicValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            dynamicValueColumn.setOnEditCommit(event -> {
                DynamicVariable editedVar = event.getTableView().getItems().get(event.getTablePosition().getRow());
                editedVar.setValue(event.getNewValue());
                
                // Update the selected test's dynamic variables map immediately
                GatlingTest selectedTest = testTable.getSelectionModel().getSelectedItem();
                if (selectedTest != null) {
                    selectedTest.getDynamicVariables().put(editedVar.getKey(), editedVar.getValue());
                }
            });

            // Add listener for template selection
            templateComboBox.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            populateDynamicVariables(bodyTemplates.get(newValue));
                        } else {
                            dynamicVariables.clear();
                        }
                    }
            );
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        
        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        isRunColumn.setCellValueFactory(new PropertyValueFactory<>("isRun"));
        isRunColumn.setCellFactory(CheckBoxTableCell.forTableColumn(isRunColumn));
        suiteColumn.setCellValueFactory(new PropertyValueFactory<>("suite"));
        tcidColumn.setCellValueFactory(new PropertyValueFactory<>("tcid"));
        descriptionsColumn.setCellValueFactory(new PropertyValueFactory<>("descriptions"));
        endpointColumn.setCellValueFactory(new PropertyValueFactory<>("endpoint"));
        tagsColumn.setCellValueFactory(new PropertyValueFactory<>("tags"));
        waitTimeColumn.setCellValueFactory(new PropertyValueFactory<>("waitTime"));

        testTable.setItems(testList);
        testTable.setEditable(true);

        // Add listener for table selection
        testTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showTestDetails(newValue)
        );

        // Load initial data and templates
        loadTests();
        loadTemplates();
    }

    private void loadTemplates() {
        try {
            bodyTemplates.clear();
            for (BodyTemplate template : bodyTemplateService.findAllTemplates()) {
                bodyTemplates.put(template.getName(), template.getContent());
            }
            templateComboBox.setItems(FXCollections.observableArrayList(bodyTemplates.keySet()));
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load body templates: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void populateDynamicVariables(String template) {
        dynamicVariables.clear();
        if (template != null) {
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(template);
            while (matcher.find()) {
                dynamicVariables.add(new DynamicVariable(matcher.group(1), ""));
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

    private void showTestDetails(GatlingTest test) {
        if (test != null) {
            testIdField.setText(String.valueOf(test.getId()));
            isRunCheckBox.setSelected(test.isRun());
            suiteField.setText(test.getSuite());
            tcidField.setText(test.getTcid());
            descriptionsArea.setText(test.getDescriptions());
            conditionsArea.setText(test.getConditions());
            expStatusField.setText(test.getExpStatus());
            expResultArea.setText(test.getExpResult());
            saveFieldsArea.setText(test.getSaveFields());
            endpointField.setText(test.getEndpoint());
            headersArea.setText(test.getHeaders());
            templateComboBox.setValue(test.getBodyTemplateName());
            dynamicVariables.clear();
            if (test.getDynamicVariables() != null) {
                test.getDynamicVariables().forEach((key, value) -> dynamicVariables.add(new DynamicVariable(key, value)));
            }
            tagsField.setText(test.getTags());
            waitTimeSpinner.getValueFactory().setValue(test.getWaitTime());
        } else {
            clearFields();
        }
    }

    private void clearFields() {
        testIdField.clear();
        isRunCheckBox.setSelected(false);
        suiteField.clear();
        tcidField.clear();
        descriptionsArea.clear();
        conditionsArea.clear();
        expStatusField.clear();
        expResultArea.clear();
        saveFieldsArea.clear();
        endpointField.clear();
        headersArea.clear();
        templateComboBox.getSelectionModel().clearSelection();
        dynamicVariables.clear();
        tagsField.clear();
        waitTimeSpinner.getValueFactory().setValue(0);
    }

    private String buildRequestBody() {
        String selectedTemplateName = templateComboBox.getSelectionModel().getSelectedItem();
        if (selectedTemplateName == null || !bodyTemplates.containsKey(selectedTemplateName)) {
            return null;
        }
        String template = bodyTemplates.get(selectedTemplateName);
        String body = template;
        for (DynamicVariable var : dynamicVariables) {
            body = body.replace("${" + var.getKey() + "}", var.getValue());
        }
        return body;
    }

    @FXML
    private void handleAddTest() {
        String suite = suiteField.getText().trim();
        String tcid = tcidField.getText().trim();
        String descriptions = descriptionsArea.getText().trim();
        String endpoint = endpointField.getText().trim();

        if (suite.isEmpty() || tcid.isEmpty() || endpoint.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Suite, TCID, and Endpoint are required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }

        GatlingTest newTest = new GatlingTest(suite, tcid, descriptions, endpoint);
        newTest.setRun(isRunCheckBox.isSelected());
        newTest.setConditions(conditionsArea.getText());
        newTest.setExpStatus(expStatusField.getText());
        newTest.setExpResult(expResultArea.getText());
        newTest.setSaveFields(saveFieldsArea.getText());
        newTest.setHeaders(headersArea.getText());
        newTest.setBodyTemplateName(templateComboBox.getSelectionModel().getSelectedItem());
        Map<String, String> vars = new HashMap<>();
        dynamicVariables.forEach(dv -> vars.put(dv.getKey(), dv.getValue()));
        newTest.setDynamicVariables(vars);
        newTest.setBodyTemplate(buildRequestBody());
        newTest.setTags(tagsField.getText());
        newTest.setWaitTime(waitTimeSpinner.getValue());

        try {
            testService.createTest(newTest);
            testList.add(newTest);
            clearFields();
            testTable.getSelectionModel().select(newTest);
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Test added successfully.", MainViewModel.StatusType.SUCCESS);
            }
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
                mainViewModel.updateStatus("Selection Error: Please select a test from the table to update.", MainViewModel.StatusType.ERROR);
            }
            return;
        }

        String suite = suiteField.getText().trim();
        String tcid = tcidField.getText().trim();
        String endpoint = endpointField.getText().trim();

        if (suite.isEmpty() || tcid.isEmpty() || endpoint.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Suite, TCID, and Endpoint are required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }

        selectedTest.setRun(isRunCheckBox.isSelected());
        selectedTest.setSuite(suite);
        selectedTest.setTcid(tcid);
        selectedTest.setDescriptions(descriptionsArea.getText());
        selectedTest.setConditions(conditionsArea.getText());
        selectedTest.setExpStatus(expStatusField.getText());
        selectedTest.setExpResult(expResultArea.getText());
        selectedTest.setSaveFields(saveFieldsArea.getText());
        selectedTest.setEndpoint(endpoint);
        selectedTest.setHeaders(headersArea.getText());
        selectedTest.setBodyTemplateName(templateComboBox.getSelectionModel().getSelectedItem());
        Map<String, String> vars = new HashMap<>();
        dynamicVariables.forEach(dv -> vars.put(dv.getKey(), dv.getValue()));
        selectedTest.setDynamicVariables(vars);
        selectedTest.setBodyTemplate(buildRequestBody());
        selectedTest.setTags(tagsField.getText());
        selectedTest.setWaitTime(waitTimeSpinner.getValue());

        try {
            testService.updateTest(selectedTest);
            testTable.refresh();
            testTable.getSelectionModel().select(selectedTest);
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Test updated successfully.", MainViewModel.StatusType.SUCCESS);
            }
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
                mainViewModel.updateStatus("Selection Error: Please select a test from the table to delete.", MainViewModel.StatusType.ERROR);
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
                mainViewModel.updateStatus("Selection Error: Please select a test to run.", MainViewModel.StatusType.ERROR);
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
                mainViewModel.updateStatus("Test completed: " + selectedTest.getTcid(), MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to run test: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleRunSuite() {
        String suite = suiteField.getText().trim();
        if (suite.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Please enter a suite name to run.", MainViewModel.StatusType.ERROR);
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
                mainViewModel.updateStatus("Failed to run test suite: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }
}