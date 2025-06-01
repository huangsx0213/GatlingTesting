package com.qa.app.ui.vm;

import com.qa.app.model.Environment;
import com.qa.app.service.ServiceException;
import com.qa.app.service.impl.EnvironmentServiceImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.ResourceBundle;

public class EnvironmentViewModel implements Initializable {
    @FXML private TextField environmentNameField;
    @FXML private TextArea environmentDescriptionArea;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private TableView<Environment> environmentTable;
    @FXML private TableColumn<Environment, String> environmentNameColumn;
    @FXML private TableColumn<Environment, String> environmentDescriptionColumn;

    private final EnvironmentServiceImpl environmentService = new EnvironmentServiceImpl();
    private final ObservableList<Environment> environmentList = FXCollections.observableArrayList();
    private Environment selectedEnvironment = null;
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initialize();
    }

    public void initialize() {
        environmentNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        environmentDescriptionColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        environmentTable.setItems(environmentList);
        loadEnvironments();
        environmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> onTableSelectionChanged(newSel));
    }

    private void loadEnvironments() {
        environmentList.clear();
        try {
            environmentList.addAll(environmentService.findAllEnvironments());
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load environments: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void onTableSelectionChanged(Environment environment) {
        selectedEnvironment = environment;
        if (environment != null) {
            environmentNameField.setText(environment.getName());
            environmentDescriptionArea.setText(environment.getDescription());
        } else {
            clearForm();
        }
    }

    @FXML
    private void handleAddEnvironment(ActionEvent event) {
        String name = environmentNameField.getText().trim();
        String description = environmentDescriptionArea.getText().trim();
        if (name.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Name is required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        Environment environment = new Environment(name, description);
        try {
            environmentService.createEnvironment(environment);
            loadEnvironments();
            clearForm();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Environment added successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to add environment: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleUpdateEnvironment(ActionEvent event) {
        if (selectedEnvironment == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select an environment to update.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        selectedEnvironment.setName(environmentNameField.getText().trim());
        selectedEnvironment.setDescription(environmentDescriptionArea.getText().trim());
        try {
            environmentService.updateEnvironment(selectedEnvironment);
            loadEnvironments();
            clearForm();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Environment updated successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to update environment: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleDeleteEnvironment(ActionEvent event) {
        if (selectedEnvironment == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select an environment to delete.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            environmentService.deleteEnvironment(selectedEnvironment.getId());
            loadEnvironments();
            clearForm();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Environment deleted successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to delete environment: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleClearEnvironmentForm(ActionEvent event) {
        clearForm();
    }

    private void clearForm() {
        environmentNameField.clear();
        environmentDescriptionArea.clear();
        environmentTable.getSelectionModel().clearSelection();
        selectedEnvironment = null;
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void refresh() {
        loadEnvironments();
        clearForm();
    }
} 