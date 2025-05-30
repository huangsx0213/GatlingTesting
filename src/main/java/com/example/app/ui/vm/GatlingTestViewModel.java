package com.example.app.ui.vm;

import com.example.app.model.GatlingTest;
import com.example.app.service.GatlingTestServiceImpl;
import com.example.app.service.IGatlingTestService;
import com.example.app.service.ServiceException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

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
    private TextField saveFieldsField;
    @FXML
    private TextField endpointField;
    @FXML
    private TextArea headersArea;
    @FXML
    private TextArea bodyTemplateArea;
    @FXML
    private TextArea bodyDefaultArea;
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
    private final ObservableList<GatlingTest> testList = FXCollections.observableArrayList();
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize spinner
        waitTimeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3600, 0));
        
        // testIdField is hidden but used internally
        testIdField.setVisible(false);
        testIdField.setManaged(false);
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

        // Load initial data
        loadTests();
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
            bodyOverrideArea.setText(test.getBodyOverride());
            expStatusField.setText(test.getExpStatus());
            expResultArea.setText(test.getExpResult());
            saveFieldsField.setText(test.getSaveFields());
            endpointField.setText(test.getEndpoint());
            headersArea.setText(test.getHeaders());
            bodyTemplateArea.setText(test.getBodyTemplate());
            bodyDefaultArea.setText(test.getBodyDefault());
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
        bodyOverrideArea.clear();
        expStatusField.clear();
        expResultArea.clear();
        saveFieldsField.clear();
        endpointField.clear();
        headersArea.clear();
        bodyTemplateArea.clear();
        bodyDefaultArea.clear();
        tagsField.clear();
        waitTimeSpinner.getValueFactory().setValue(0);
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
        newTest.setBodyOverride(bodyOverrideArea.getText());
        newTest.setExpStatus(expStatusField.getText());
        newTest.setExpResult(expResultArea.getText());
        newTest.setSaveFields(saveFieldsField.getText());
        newTest.setHeaders(headersArea.getText());
        newTest.setBodyTemplate(bodyTemplateArea.getText());
        newTest.setBodyDefault(bodyDefaultArea.getText());
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

        // Update the selected test with form data
        selectedTest.setRun(isRunCheckBox.isSelected());
        selectedTest.setSuite(suite);
        selectedTest.setTcid(tcid);
        selectedTest.setDescriptions(descriptionsArea.getText());
        selectedTest.setConditions(conditionsArea.getText());
        selectedTest.setBodyOverride(bodyOverrideArea.getText());
        selectedTest.setExpStatus(expStatusField.getText());
        selectedTest.setExpResult(expResultArea.getText());
        selectedTest.setSaveFields(saveFieldsField.getText());
        selectedTest.setEndpoint(endpoint);
        selectedTest.setHeaders(headersArea.getText());
        selectedTest.setBodyTemplate(bodyTemplateArea.getText());
        selectedTest.setBodyDefault(bodyDefaultArea.getText());
        selectedTest.setTags(tagsField.getText());
        selectedTest.setWaitTime(waitTimeSpinner.getValue());

        try {
            testService.updateTest(selectedTest);
            testTable.refresh();
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
            loadTests(); // Refresh the table
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