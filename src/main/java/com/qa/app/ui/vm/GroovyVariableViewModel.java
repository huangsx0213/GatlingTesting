package com.qa.app.ui.vm;

import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IVariableService;
import com.qa.app.service.impl.VariableServiceImpl;
import com.qa.app.service.script.GroovyScriptEngine;
import com.qa.app.service.script.VariableGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class GroovyVariableViewModel implements Initializable {

    @FXML private TableView<GroovyScriptEngine> variablesTableView;
    @FXML private TableColumn<GroovyScriptEngine, String> nameColumn;
    @FXML private TableColumn<GroovyScriptEngine, String> descriptionColumn;
    @FXML private TextField nameField;
    @FXML private TextField formatField;
    @FXML private TextField descriptionField;
    @FXML private TextArea scriptArea;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button duplicateButton;
    @FXML private Button addButton;

    private final IVariableService variableService;
    private final ObservableList<GroovyScriptEngine> variables;
    private GroovyScriptEngine currentlyEditing = null;
    private MainViewModel mainViewModel;

    public GroovyVariableViewModel() {
        this.variableService = new VariableServiceImpl();
        this.variables = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        variablesTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
        descriptionColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        loadVariables();
    }

    public ObservableList<GroovyScriptEngine> getVariables() {
        return variables;
    }

    public void loadVariables() {
        try {
            variables.setAll(variableService.loadVariables());
            variablesTableView.setItems(variables);

            variablesTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        selectVariable(newVal);
                    }
                }
            );

            if (!variables.isEmpty()) {
                variablesTableView.getSelectionModel().selectFirst();
            } else {
                setDetailPaneDisabled(true); // No items, so disable details
            }
        } catch (ServiceException e) {
            variables.clear();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Error loading variables: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void selectVariable(GroovyScriptEngine variable) {
        currentlyEditing = variable;
        nameField.setText(variable.getName());
        formatField.setText(variable.getFormat());
        descriptionField.setText(variable.getDescription());
        scriptArea.setText(variable.getGroovyScript());
        setDetailPaneDisabled(false);
    }

    @FXML
    private void handleDuplicate() {
        if (currentlyEditing == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("No variable selected to duplicate.", MainViewModel.StatusType.ERROR);
            }
            return;
        }

        String newName = currentlyEditing.getName() + " (copy)";
        boolean nameExists = true;
        while (nameExists) {
            nameExists = false;
            for (GroovyScriptEngine v : variables) {
                if (v.getName().equals(newName)) {
                    nameExists = true;
                    break;
                }
            }
            if (nameExists) {
                newName += " (copy)";
            }
        }

        GroovyScriptEngine newVar = new GroovyScriptEngine(newName, currentlyEditing.getFormat(), currentlyEditing.getDescription(), currentlyEditing.getGroovyScript());
        try {
            variableService.addVariable(newVar);
            VariableGenerator.getInstance().reloadCustomVariables();
            loadVariables();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Variable '" + newName + "' duplicated and added successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Error duplicating variable: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleAdd() {
        String name = nameField.getText();
        String format = formatField.getText();
        String description = descriptionField.getText();
        String script = scriptArea.getText();

        if (name == null || name.isBlank() || format == null || format.isBlank()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Name and Format cannot be empty.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        // Check for duplicate name
        boolean nameExists = variables.stream().anyMatch(v -> v.getName().equals(name));
        if (nameExists) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Variable name already exists.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        GroovyScriptEngine newVar = new GroovyScriptEngine(name, format, description, script);
        try {
            variableService.addVariable(newVar);
            VariableGenerator.getInstance().reloadCustomVariables();
            loadVariables();
            // Select the newly added variable
            variablesTableView.getItems().stream()
                .filter(v -> v.getName().equals(name))
                .findFirst()
                .ifPresent(v -> variablesTableView.getSelectionModel().select(v));
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Variable '" + name + "' added successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Error adding variable: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleSave() {
        if (currentlyEditing == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("No variable selected to save.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        GroovyScriptEngine selected = currentlyEditing;
        String name = nameField.getText();
        String format = formatField.getText();
        String description = descriptionField.getText();
        String script = scriptArea.getText();
        if (name == null || name.isBlank() || format == null || format.isBlank()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Name and Format cannot be empty.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        // Check for duplicate name (ignore current editing variable)
        boolean nameExists = variables.stream().anyMatch(v -> v.getName().equals(name) && v != selected);
        if (nameExists) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Variable name already exists.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        // 保留原id
        GroovyScriptEngine updatedVariable = new GroovyScriptEngine(selected.getId(), name, format, description, script);
        try {
            variableService.updateVariable(updatedVariable);
            VariableGenerator.getInstance().reloadCustomVariables();
            loadVariables();
            // Select the updated variable
            variablesTableView.getItems().stream()
                .filter(v -> v.getName().equals(name))
                .findFirst()
                .ifPresent(v -> variablesTableView.getSelectionModel().select(v));
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Variable '" + name + "' saved successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Error saving variable: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (currentlyEditing == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("No variable selected to delete.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        GroovyScriptEngine selected = currentlyEditing;
        System.out.println("[DEBUG] Try to delete variable id: " + selected.getId()); // debug log
        try {
            if (selected.getId() != null) {
                variableService.deleteVariable(selected.getId());
            } else {
                System.out.println("[DEBUG] id is null");
            }
            VariableGenerator.getInstance().reloadCustomVariables();
            loadVariables();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Variable deleted successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Error deleting variable: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleClear() {
        nameField.clear();
        formatField.clear();
        descriptionField.clear();
        scriptArea.clear();
        variablesTableView.getSelectionModel().clearSelection();
        setDetailPaneDisabled(false);
        currentlyEditing = null;
        if (mainViewModel != null) {
            mainViewModel.updateStatus("Form cleared.", MainViewModel.StatusType.INFO);
        }
    }


    private void setDetailPaneDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        formatField.setDisable(disabled);
        descriptionField.setDisable(disabled);
        scriptArea.setDisable(disabled);
        saveButton.setDisable(disabled);
        deleteButton.setDisable(disabled);
    }


    
    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }
    
    public void refresh() {
        loadVariables();
    }
}
