package com.qa.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

import com.qa.app.model.Endpoint;
import com.qa.app.model.Environment;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IEndpointService;
import com.qa.app.service.api.IEnvironmentService;
import com.qa.app.service.impl.EndpointServiceImpl;
import com.qa.app.service.impl.EnvironmentServiceImpl;
import com.qa.app.service.ProjectContext;
import com.qa.app.util.AppConfig;
import com.qa.app.common.listeners.AppConfigChangeListener;

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
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;
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
        loadEndpoints();
        loadEnvironments();
        AppConfig.addChangeListener(this);
    }

    @Override
    public void onConfigChanged() {
        loadEndpoints();
        loadEnvironments();
    }

    public void refresh() {
        loadEndpoints();
        loadEnvironments();
    }

    private void loadEndpoints() {
        endpointList.clear();
        Integer projectId = ProjectContext.getCurrentProjectId();
        if (projectId != null) {
            try {
                for (Endpoint e : endpointService.getEndpointsByProjectId(projectId)) {
                    endpointList.add(new EndpointItem(e.getId(), e.getName(), e.getMethod(), e.getUrl(), e.getEnvironmentId()));
                }
            } catch (ServiceException e) {
                // 可加错误提示
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
        if (isNameEnvDuplicate(name, envId, selected.getId())) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Endpoint name + environment already exists!", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            Endpoint e = new Endpoint(selected.getId(), name, method, url, envId, ProjectContext.getCurrentProjectId());
            endpointService.updateEndpoint(e);
            loadEndpoints();
            for (EndpointItem item : endpointList) {
                if (item.getId() == e.getId()) {
                    endpointTable.getSelectionModel().select(item);
                    break;
                }
            }
            clearFields();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Endpoint updated successfully.", MainViewModel.StatusType.SUCCESS);
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

    // 内部类用于表格项
    public static class EndpointItem {
        private final int id;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty method;
        private final javafx.beans.property.SimpleStringProperty url;
        private final Integer environmentId;
        public EndpointItem(int id, String name, String method, String url, Integer environmentId) {
            this.id = id;
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.method = new javafx.beans.property.SimpleStringProperty(method);
            this.url = new javafx.beans.property.SimpleStringProperty(url);
            this.environmentId = environmentId;
        }
        public int getId() { return id; }
        public String getName() { return name.get(); }
        public void setName(String n) { name.set(n); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public String getMethod() { return method.get(); }
        public void setMethod(String m) { method.set(m); }
        public javafx.beans.property.StringProperty methodProperty() { return method; }
        public String getUrl() { return url.get(); }
        public void setUrl(String u) { url.set(u); }
        public javafx.beans.property.StringProperty urlProperty() { return url; }
        public Integer getEnvironmentId() { return environmentId; }
    }
} 