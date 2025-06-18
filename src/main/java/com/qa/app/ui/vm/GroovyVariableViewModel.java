package com.qa.app.ui.vm;

import com.qa.app.service.VariableService;
import com.qa.app.util.GroovyVariable;
import com.qa.app.util.VariableGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GroovyVariableViewModel {

    // FXML fields for the new layout
    @FXML private ListView<GroovyVariable> variablesListView;
    @FXML private TextField nameField;
    @FXML private TextField formatField;
    @FXML private TextField descriptionField;
    @FXML private TextArea scriptArea;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    
    private final VariableService variableService = new VariableService();
    private ObservableList<GroovyVariable> variables;
    private GroovyVariable currentlyEditing = null;
    private boolean isNewVariable = false;

    @FXML
    public void initialize() {
        // --- Setup ListView ---
        variables = FXCollections.observableArrayList(variableService.loadVariables());
        variablesListView.setItems(variables);
        
        // Use a custom cell factory to show the variable name
        variablesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GroovyVariable item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
            }
        });
        
        // --- Setup Listeners ---
        variablesListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectVariable(newVal);
                }
            }
        );
        
        // --- Initial State ---
        if (!variables.isEmpty()) {
            variablesListView.getSelectionModel().selectFirst();
        } else {
            setDetailPaneDisabled(true);
        }
    }
    
    private void selectVariable(GroovyVariable variable) {
        isNewVariable = false;
        currentlyEditing = variable;
        
        nameField.setText(variable.getName());
        formatField.setText(variable.getFormat());
        descriptionField.setText(variable.getDescription());
        scriptArea.setText(variable.getGroovyScript());
        
        setDetailPaneDisabled(false);
    }
    
    @FXML
    private void handleAdd() {
        // Create a temporary new variable
        GroovyVariable newVar = new GroovyVariable("New Variable", "@{newVariable}", "Description", "return \"hello world\"");
        isNewVariable = true;
        currentlyEditing = newVar;

        // Add to list and select it
        variables.add(newVar);
        variablesListView.getSelectionModel().select(newVar);
        
        // Update detail pane
        nameField.setText(newVar.getName());
        formatField.setText(newVar.getFormat());
        descriptionField.setText(newVar.getDescription());
        scriptArea.setText(newVar.getGroovyScript());
        
        setDetailPaneDisabled(false);
        nameField.requestFocus(); // Focus on name for quick editing
    }

    @FXML
    private void handleDelete() {
        GroovyVariable selected = variablesListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete '" + selected.getName() + "'?", ButtonType.YES, ButtonType.NO);
            confirmDialog.setHeaderText("Delete Variable");
            Optional<ButtonType> result = confirmDialog.showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.YES) {
                variables.remove(selected);
                saveChangesToFile();
                if (variables.isEmpty()) {
                    setDetailPaneDisabled(true);
                }
            }
        }
    }

    @FXML
    private void handleSave() {
        if (currentlyEditing == null) {
            showAlert("Error", "No variable selected to save.");
            return;
        }

        // --- Create new variable from fields ---
        String name = nameField.getText();
        String format = formatField.getText();
        String description = descriptionField.getText();
        String script = scriptArea.getText();
        
        if (name.isBlank() || format.isBlank()) {
            showAlert("Validation Error", "Name and Format cannot be empty.");
            return;
        }
        
        GroovyVariable updatedVariable = new GroovyVariable(name, format, description, script);

        // --- Update the list ---
        int selectedIndex = variablesListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex != -1) {
             variables.set(selectedIndex, updatedVariable);
        }
       
        // --- Save and reload ---
        saveChangesToFile();
        
        // Reselect the updated item
        variablesListView.getSelectionModel().select(updatedVariable);
        
        showAlert("Success", "Variable '" + name + "' saved successfully.");
    }
    
    private void saveChangesToFile() {
        variableService.saveVariables(List.copyOf(variables)); // Use immutable list copy
        VariableGenerator.reloadCustomVariables(); // Notify core engine to reload
    }
    
    private void setDetailPaneDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        formatField.setDisable(disabled);
        descriptionField.setDisable(disabled);
        scriptArea.setDisable(disabled);
        saveButton.setDisable(disabled);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 