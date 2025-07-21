package com.qa.app.ui.vm;

import com.qa.app.model.VariableTransformMethod;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IVariableTransformMethodService;
import com.qa.app.service.impl.VariableTransformMethodServiceImpl;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * JavaFX controller for managing variable transform methods.
 */
public class VariableTransformMethodViewModel implements Initializable {

    @FXML private TableView<VariableTransformMethod> methodsTableView;
    @FXML private TableColumn<VariableTransformMethod, String> nameColumn;
    @FXML private TableColumn<VariableTransformMethod, String> descColumn;
    @FXML private TableColumn<VariableTransformMethod, Boolean> enabledColumn;

    @FXML private TextField nameField;
    @FXML private TextArea scriptArea;
    @FXML private TextField descriptionField;
    @FXML private TextField paramSpecField;
    @FXML private TextField sampleUsageField;
    @FXML private CheckBox enabledCheckBox;

    // test conversion
    @FXML private TextField testInputField;
    @FXML private TextField testParamsField;
    @FXML private Button testRunButton;
    @FXML private Label testResultLabel;

    private final ObservableList<VariableTransformMethod> methods = FXCollections.observableArrayList();
    private final IVariableTransformMethodService service = new VariableTransformMethodServiceImpl();

    private VariableTransformMethod editing;
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        methodsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        descColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        enabledColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleBooleanProperty(c.getValue().isEnabled()));
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));
        loadMethods();

        methodsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                populateForm(newV);
            }
        });
    }

    public void refresh() {
        loadMethods();
    }

    private void loadMethods() {
        try {
            methods.setAll(service.findAll());
            methodsTableView.setItems(methods);
            if (!methods.isEmpty()) {
                methodsTableView.getSelectionModel().selectFirst();
            } else {
                clearForm();
            }
        } catch (ServiceException e) {
            updateStatus("Failed to load methods: " + e.getMessage(), MainViewModel.StatusType.ERROR);
        }
    }

    private void populateForm(VariableTransformMethod m) {
        editing = m;
        nameField.setText(m.getName());
        descriptionField.setText(m.getDescription());
        scriptArea.setText(m.getScript());
        paramSpecField.setText(m.getParamSpec());
        sampleUsageField.setText(m.getSampleUsage());
        enabledCheckBox.setSelected(m.isEnabled());
    }

    @FXML
    private void handleAdd() {
        VariableTransformMethod m = new VariableTransformMethod();
        if (!fillModelFromForm(m, true)) return;
        try {
            service.create(m);
            loadMethods();
            updateStatus("Added method '" + m.getName() + "'", MainViewModel.StatusType.SUCCESS);
        } catch (ServiceException e) {
            updateStatus("Add failed: " + e.getMessage(), MainViewModel.StatusType.ERROR);
        }
    }

    @FXML
    private void handleSave() {
        if (editing == null) {
            updateStatus("No selection to save", MainViewModel.StatusType.ERROR);
            return;
        }
        if (!fillModelFromForm(editing, false)) return;
        try {
            service.update(editing);
            loadMethods();
            updateStatus("Updated method", MainViewModel.StatusType.SUCCESS);
        } catch (ServiceException e) {
            updateStatus("Update failed: " + e.getMessage(), MainViewModel.StatusType.ERROR);
        }
    }

    @FXML
    private void handleDelete() {
        if (editing == null) {
            updateStatus("No selection to delete", MainViewModel.StatusType.ERROR);
            return;
        }
        try {
            service.delete(editing.getId());
            loadMethods();
            updateStatus("Deleted", MainViewModel.StatusType.SUCCESS);
        } catch (ServiceException e) {
            updateStatus("Delete failed: " + e.getMessage(), MainViewModel.StatusType.ERROR);
        }
    }

    @FXML
    private void handleClear() {
        editing = null;
        clearForm();
    }

    private void clearForm() {
        nameField.clear();
        descriptionField.clear();
        scriptArea.clear();
        paramSpecField.clear();
        sampleUsageField.clear();
        enabledCheckBox.setSelected(true);
    }

    private boolean fillModelFromForm(VariableTransformMethod m, boolean isNew) {
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            updateStatus("Name required", MainViewModel.StatusType.ERROR);
            return false;
        }
        m.setName(name.trim());
        m.setDescription(descriptionField.getText());
        m.setScript(scriptArea.getText());
        m.setParamSpec(paramSpecField.getText());
        m.setSampleUsage(sampleUsageField.getText());
        m.setEnabled(enabledCheckBox.isSelected());
        return true;
    }

    @FXML
    private void handleTestRun() {
        if (editing == null) {
            updateStatus("Select or save method first", MainViewModel.StatusType.ERROR);
            return;
        }
        String input = testInputField.getText();
        List<String> params = Arrays.stream(testParamsField.getText().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        try {
            Object result = service.apply(editing.getName(), input, params);
            testResultLabel.setText(String.valueOf(result));
        } catch (ServiceException e) {
            testResultLabel.setText("ERROR: " + e.getMessage());
        }
    }

    private void updateStatus(String msg, MainViewModel.StatusType type) {
        if (mainViewModel != null) {
            mainViewModel.updateStatus(msg, type);
        }
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }
} 