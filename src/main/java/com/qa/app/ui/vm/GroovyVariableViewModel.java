package com.qa.app.ui.vm;

import com.qa.app.service.VariableService;
import com.qa.app.util.GroovyVariable;
import com.qa.app.util.VariableGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class GroovyVariableViewModel {

    @FXML private ListView<GroovyVariable> variablesListView;
    @FXML private TextField nameField;
    @FXML private TextField formatField;
    @FXML private TextField descriptionField;
    @FXML private TextArea scriptArea;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button addButton;

    private final VariableService variableService = new VariableService();
    private ObservableList<GroovyVariable> variables;
    private GroovyVariable currentlyEditing = null;
    private MainViewModel mainViewModel;

    @FXML
    public void initialize() {
        variables = FXCollections.observableArrayList(variableService.loadVariables());
        variablesListView.setItems(variables);

        variablesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GroovyVariable item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
            }
        });

        variablesListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectVariable(newVal);
                }
            }
        );

        if (!variables.isEmpty()) {
            variablesListView.getSelectionModel().selectFirst();
        } else {
            setDetailPaneDisabled(false);
        }
    }

    private void selectVariable(GroovyVariable variable) {
        currentlyEditing = variable;
        nameField.setText(variable.getName());
        formatField.setText(variable.getFormat());
        descriptionField.setText(variable.getDescription());
        scriptArea.setText(variable.getGroovyScript());
        setDetailPaneDisabled(false);
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
        GroovyVariable newVar = new GroovyVariable(name, format, description, script);
        variableService.addVariable(newVar);
        VariableGenerator.reloadCustomVariables();
        refresh();
        // Select the newly added variable
        variablesListView.getItems().stream()
            .filter(v -> v.getName().equals(name))
            .findFirst()
            .ifPresent(v -> variablesListView.getSelectionModel().select(v));
        if (mainViewModel != null) {
            mainViewModel.updateStatus("Variable '" + name + "' added successfully.", MainViewModel.StatusType.SUCCESS);
        }
    }

    @FXML
    private void handleSave() {
        GroovyVariable selected = variablesListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("No variable selected to save.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
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
        GroovyVariable updatedVariable = new GroovyVariable(selected.getId(), name, format, description, script);
        variableService.updateVariable(updatedVariable);
        VariableGenerator.reloadCustomVariables();
        refresh();
        // Select the updated variable
        variablesListView.getItems().stream()
            .filter(v -> v.getName().equals(name))
            .findFirst()
            .ifPresent(v -> variablesListView.getSelectionModel().select(v));
        if (mainViewModel != null) {
            mainViewModel.updateStatus("Variable '" + name + "' saved successfully.", MainViewModel.StatusType.SUCCESS);
        }
    }

    @FXML
    private void handleDelete() {
        GroovyVariable selected = variablesListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("No variable selected to delete.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        System.out.println("[DEBUG] Try to delete variable id: " + selected.getId()); // debug log
        if (selected.getId() != null) {
            variableService.deleteVariable(selected.getId());
        } else {
            System.out.println("[DEBUG] id is null");
        }
        VariableGenerator.reloadCustomVariables();
        refresh();
        if (mainViewModel != null) {
            mainViewModel.updateStatus("Variable deleted successfully.", MainViewModel.StatusType.SUCCESS);
        }
    }

    @FXML
    private void handleClear() {
        nameField.clear();
        formatField.clear();
        descriptionField.clear();
        scriptArea.clear();
        variablesListView.getSelectionModel().clearSelection();
        setDetailPaneDisabled(false);
        currentlyEditing = null;
        if (mainViewModel != null) {
            mainViewModel.updateStatus("Form cleared.", MainViewModel.StatusType.INFO);
        }
    }

    private void saveChangesToFile() {
        variableService.saveVariables(List.copyOf(variables));
        VariableGenerator.reloadCustomVariables();
    }
    
    private void clearAndEnableDetailPane() {
        nameField.clear();
        formatField.clear();
        descriptionField.clear();
        scriptArea.clear();
        setDetailPaneDisabled(false);
    }

    private void setDetailPaneDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        formatField.setDisable(disabled);
        descriptionField.setDisable(disabled);
        scriptArea.setDisable(disabled);
        saveButton.setDisable(disabled);
        deleteButton.setDisable(disabled);
    }

    private void showAlert(String title, String content) {
        // No longer used, replaced by status bar messages
    }
    
    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }
    
    public void refresh() {
        variables.setAll(variableService.loadVariables());
        if (!variables.isEmpty()) {
            variablesListView.getSelectionModel().selectFirst();
        } else {
            clearAndEnableDetailPane();
        }
    }
}
