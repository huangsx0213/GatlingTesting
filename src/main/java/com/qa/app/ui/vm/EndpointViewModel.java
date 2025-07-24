package com.qa.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

import java.net.URL;
import java.util.ResourceBundle;

import com.qa.app.model.Endpoint;
import com.qa.app.model.Environment;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IEndpointService;
import com.qa.app.service.api.IEnvironmentService;
import com.qa.app.service.impl.EndpointServiceImpl;
import com.qa.app.service.impl.EnvironmentServiceImpl;
import com.qa.app.ui.util.ClickableTooltipTableCell;
import com.qa.app.util.AppConfig;
import com.qa.app.service.ProjectContext;
import com.qa.app.common.listeners.AppConfigChangeListener;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class EndpointViewModel implements Initializable, AppConfigChangeListener {
    @FXML
    private TextField endpointNameField;
    @FXML
    private ComboBox<String> methodComboBox;
    @FXML
    private TextField urlField;
    @FXML
    private ComboBox<Environment> environmentComboBox;
    @FXML
    private Button addButton;
    @FXML
    private Button duplicateButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;

    @FXML
    private Button moveUpButton;

    @FXML
    private Button moveDownButton;
    @FXML
    private TableView<EndpointItem> endpointTable;
    @FXML
    private TableColumn<EndpointItem, String> endpointNameColumn;
    @FXML
    private TableColumn<EndpointItem, String> methodColumn;
    @FXML
    private TableColumn<EndpointItem, String> urlColumn;
    @FXML
    private TableColumn<EndpointItem, String> environmentColumn;

    private final ObservableList<EndpointItem> endpointList = FXCollections.observableArrayList();
    private final IEndpointService endpointService = new EndpointServiceImpl();
    private final IEnvironmentService environmentService = new EnvironmentServiceImpl();
    private ObservableList<Environment> environmentList = FXCollections.observableArrayList();
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        endpointNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        methodColumn.setCellValueFactory(cellData -> cellData.getValue().methodProperty());
        urlColumn.setCellValueFactory(cellData -> cellData.getValue().urlProperty());
        urlColumn.setCellFactory(param -> new ClickableTooltipTableCell<>());
        environmentColumn.setCellValueFactory(cellData -> {
            Integer envId = cellData.getValue().getEnvironmentId();
            String envName = "";
            if (envId != null) {
                Environment env = environmentList.stream().filter(e -> e.getId() == envId).findFirst().orElse(null);
                if (env != null) envName = env.getName();
            }
            return new javafx.beans.property.SimpleStringProperty(envName);
        });
        endpointTable.setItems(endpointList);
        endpointTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        endpointTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateFields(newSelection);
            }
        });
        methodComboBox.setItems(FXCollections.observableArrayList("GET", "POST", "PUT", "DELETE", "PATCH"));
        methodComboBox.setPromptText("Select Method");
        methodComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(methodComboBox.getPromptText());
                } else {
                    setText(item);
                }
            }
        });
        loadEnvironments();
        loadEndpoints();

        AppConfig.addChangeListener(this);
    }

    @Override
    public void onConfigChanged() {
        loadEnvironments();
        loadEndpoints();
    }

    public void refresh() {
        loadEnvironments();
        loadEndpoints();
    }

    private void loadEndpoints() {
        endpointList.clear();
        Integer projectId = ProjectContext.getCurrentProjectId();
        if (projectId != null) {
            try {
                for (Endpoint e : endpointService.getEndpointsByProjectId(projectId)) {
                    endpointList.add(new EndpointItem(e.getId(), e.getName(), e.getMethod(), e.getUrl(), e.getEnvironmentId(), e.getProjectId()));
                }
            } catch (ServiceException e) {
                // 可加错误提示
            }
        }
        if (!endpointList.isEmpty()) {
            endpointTable.getSelectionModel().selectFirst();
        }

        // Ensure buttons are disabled if list empty
        boolean disabled = endpointList.isEmpty();
        if (moveUpButton != null) moveUpButton.setDisable(disabled);
        if (moveDownButton != null) moveDownButton.setDisable(disabled);
    }

    // ================== Move Up / Down ====================
    @FXML
    private void handleMoveUp() {
        EndpointItem selected = endpointTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int index = endpointList.indexOf(selected);
        if (index > 0) {
            EndpointItem above = endpointList.get(index - 1);
            endpointList.set(index - 1, selected);
            endpointList.set(index, above);

            updateDisplayOrderAndPersist();
            endpointTable.getSelectionModel().select(index - 1);
        }
    }

    @FXML
    private void handleMoveDown() {
        EndpointItem selected = endpointTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int index = endpointList.indexOf(selected);
        if (index < endpointList.size() - 1) {
            EndpointItem below = endpointList.get(index + 1);
            endpointList.set(index + 1, selected);
            endpointList.set(index, below);

            updateDisplayOrderAndPersist();
            endpointTable.getSelectionModel().select(index + 1);
        }
    }

    private void updateDisplayOrderAndPersist() {
        try {
            // Map EndpointItem to Endpoint and update displayOrder
            java.util.List<com.qa.app.model.Endpoint> toUpdate = new java.util.ArrayList<>();
            for (int i = 0; i < endpointList.size(); i++) {
                EndpointItem item = endpointList.get(i);
                com.qa.app.model.Endpoint e = new com.qa.app.model.Endpoint();
                e.setId(item.getId());
                e.setDisplayOrder(i + 1);
                toUpdate.add(e);
            }
            endpointService.updateOrder(toUpdate);
        } catch (ServiceException ex) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to update endpoint order: " + ex.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private void loadEnvironments() {
        try {
            environmentList.setAll(environmentService.findAllEnvironments());
        } catch (ServiceException e) {
            environmentList.clear();
        }
        environmentComboBox.setItems(environmentList);
    }

    private void populateFields(EndpointItem item) {
        if (item != null) {
            endpointNameField.setText(item.getName());
            methodComboBox.setValue(item.getMethod());
            urlField.setText(item.getUrl());
            Integer envId = item.getEnvironmentId();
            Environment env = null;
            if (envId != null) {
                env = environmentList.stream().filter(e -> e.getId() == envId).findFirst().orElse(null);
            }
            environmentComboBox.setValue(env);
        } else {
            clearFields();
        }
    }

    @FXML
    private void handleDuplicateEndpoint() {
        EndpointItem selected = endpointTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Please select an endpoint to duplicate.", MainViewModel.StatusType.ERROR);
            }
            return;
        }

        String newName = selected.getName() + " (copy)";
        while (isNameEnvDuplicate(newName, selected.getEnvironmentId(), null)) {
            newName += " (copy)";
        }

        try {
            Endpoint newEndpoint = new Endpoint(newName, selected.getMethod(), selected.getUrl(), selected.getEnvironmentId(), ProjectContext.getCurrentProjectId());
            endpointService.addEndpoint(newEndpoint);
            loadEndpoints();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Endpoint duplicated and added successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException ex) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to duplicate endpoint: " + ex.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    private boolean isNameEnvDuplicate(String name, Integer envId, Integer excludeId) {
        for (EndpointItem item : endpointList) {
            if (item.getName().equals(name)
                && ((item.getEnvironmentId() == null && envId == null) || (item.getEnvironmentId() != null && item.getEnvironmentId().equals(envId)))
                && (excludeId == null || item.getId() != excludeId)) {
                return true;
            }
        }
        return false;
    }

    @FXML
    private void handleAddEndpoint() {
        String name = endpointNameField.getText().trim();
        String method = methodComboBox.getValue();
        String url = urlField.getText().trim();
        Environment env = environmentComboBox.getValue();
        Integer envId = env != null ? env.getId() : null;
        if (name.isEmpty() || method == null || url.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Name, Method, and URL are required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        if (isNameEnvDuplicate(name, envId, null)) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Endpoint name + environment already exists!", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            Endpoint e = new Endpoint(name, method, url, envId, ProjectContext.getCurrentProjectId());
            endpointService.addEndpoint(e);
            loadEndpoints();
            clearFields();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Endpoint added successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException ex) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to add endpoint: " + ex.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleUpdateEndpoint() {
        EndpointItem selected = endpointTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select an endpoint to update.", MainViewModel.StatusType.ERROR);
            }
            return;
        }

        // 1. Capture the state of the selected endpoint BEFORE form data is applied
        Endpoint tempEndpoint = new Endpoint();
        // Manually copy properties needed for the check
        tempEndpoint.setId(selected.getId());
        tempEndpoint.setName(selected.getName());
        tempEndpoint.setMethod(selected.getMethod());
        tempEndpoint.setUrl(selected.getUrl());
        tempEndpoint.setEnvironmentId(selected.getEnvironmentId());
        tempEndpoint.setProjectId(selected.getProjectId());

        // 2. Populate the temporary object with form data
        if (!validateAndPopulateEndpoint(tempEndpoint)) return;
        
        try {
            // 3. Perform consistency check on the temporary object with new data
            String consistencyWarning = endpointService.checkVariableConsistency(tempEndpoint);
            boolean proceed = true; // Default to proceed if no warning
            if (consistencyWarning != null) {
                ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, consistencyWarning, okButton, cancelButton);
                alert.setTitle("Variable Consistency Warning");
                alert.setHeaderText("Potential variable mismatch across environments.");
                // Add application icon to alert window
                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon/favicon.png")));
                Optional<ButtonType> result = alert.showAndWait();
                proceed = result.isPresent() && result.get() == okButton;
            }

            if (proceed) {
                endpointService.updateEndpoint(tempEndpoint);
                loadEndpoints();
            clearFields();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Endpoint updated successfully.", MainViewModel.StatusType.SUCCESS);
                }
            }
        } catch (ServiceException ex) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to update endpoint: " + ex.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleDeleteEndpoint() {
        EndpointItem selected = endpointTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                endpointService.deleteEndpoint(selected.getId());
                loadEndpoints();
                clearFields();
                if (mainViewModel != null) {
                    mainViewModel.updateStatus("Endpoint deleted successfully.", MainViewModel.StatusType.SUCCESS);
                }
            } catch (ServiceException ex) {
                if (mainViewModel != null) {
                    mainViewModel.updateStatus("Failed to delete endpoint: " + ex.getMessage(), MainViewModel.StatusType.ERROR);
                }
            }
        } else {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select an endpoint to delete.", MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleClearEndpointForm() {
        clearFields();
    }

    private void clearFields() {
        endpointNameField.clear();
        methodComboBox.getSelectionModel().clearSelection();
        methodComboBox.setValue(null);
        urlField.clear();
        environmentComboBox.getSelectionModel().clearSelection();
        environmentComboBox.setValue(null);
        endpointTable.getSelectionModel().clearSelection();
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    private boolean validateAndPopulateEndpoint(Endpoint endpoint) {
        String name = endpointNameField.getText().trim();
        String method = methodComboBox.getValue();
        String url = urlField.getText().trim();
        Environment env = environmentComboBox.getValue();
        Integer envId = env != null ? env.getId() : null;

        if (name.isEmpty() || method == null || url.isEmpty()) {
            showStatus("Input Error: Name, Method, and URL are required.", MainViewModel.StatusType.ERROR);
            return false;
        }

        endpoint.setName(name);
        endpoint.setMethod(method);
        endpoint.setUrl(url);
        endpoint.setEnvironmentId(envId);
        endpoint.setProjectId(ProjectContext.getCurrentProjectId());
        return true;
    }

    private void showStatus(String message, MainViewModel.StatusType type) {
        if (mainViewModel != null) {
            mainViewModel.updateStatus(message, type);
        }
    }

    // 内部类用于表格项
    public static class EndpointItem {
        private final int id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty method;
        private final SimpleStringProperty url;
        private final Integer environmentId;
        private final Integer projectId;

        public EndpointItem(int id, String name, String method, String url, Integer environmentId, Integer projectId) {
            this.id = id;
            this.name = new SimpleStringProperty(name);
            this.method = new SimpleStringProperty(method);
            this.url = new SimpleStringProperty(url);
            this.environmentId = environmentId;
            this.projectId = projectId;
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public void setName(String n) { name.set(n); }
        public StringProperty nameProperty() { return name; }
        public String getMethod() { return method.get(); }
        public void setMethod(String m) { method.set(m); }
        public StringProperty methodProperty() { return method; }
        public String getUrl() { return url.get(); }
        public void setUrl(String u) { url.set(u); }
        public StringProperty urlProperty() { return url; }
        public Integer getEnvironmentId() { return environmentId; }
        public Integer getProjectId() { return projectId; }
    }
} 