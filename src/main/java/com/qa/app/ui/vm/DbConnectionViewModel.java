package com.qa.app.ui.vm;

import com.qa.app.model.DbConnection;
import com.qa.app.service.ProjectContext;
import com.qa.app.service.api.IDbConnectionService;
import com.qa.app.service.impl.DbConnectionServiceImpl;
import com.qa.app.service.runner.DataSourceRegistry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import com.qa.app.model.DbType;
import javafx.scene.control.Label;

import javax.sql.DataSource;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class DbConnectionViewModel implements Initializable {

    @FXML private TableView<DbConnection> dbConnectionTable;
    @FXML private TableColumn<DbConnection, String> aliasColumn;
    @FXML private TableColumn<DbConnection, String> descriptionColumn;
    
    @FXML private TextField aliasField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<DbType> dbTypeCombo;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField dbNameField;
    @FXML private Label dbNameLabel;
    @FXML private TextField schemaField;
    @FXML private Label schemaLabel;
    @FXML private TextField serviceNameField;
    @FXML private Label serviceNameLabel;
    @FXML private TextField urlField; // now read-only auto-generated
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Spinner<Integer> poolSizeSpinner;
    @FXML private ComboBox<com.qa.app.model.Environment> environmentCombo;

    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button testConnectionButton;
    @FXML private Button duplicateButton;

    private final IDbConnectionService dbConnectionService = new DbConnectionServiceImpl();
    private final ObservableList<DbConnection> dbConnections = FXCollections.observableArrayList();
    private final ObservableList<com.qa.app.model.Environment> environments = FXCollections.observableArrayList();
    private final com.qa.app.service.api.IEnvironmentService environmentService = new com.qa.app.service.impl.EnvironmentServiceImpl();
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupEventListeners();
        configureSpinner();
        configureDbTypeCombo();
        addFormListeners();
        loadEnvironments();
        refresh(); // Initial load
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void refresh() {
        loadConnections();
        
        // 默认选择第一行数据
        Platform.runLater(() -> {
            if (!dbConnections.isEmpty()) {
                dbConnectionTable.getSelectionModel().select(0);
                populateForm(dbConnections.get(0));
            } else {
                toggleDbFields(null); // Ensure fields are hidden if no connections
            }
        });
    }

    private void setupTable() {
        aliasColumn.setCellValueFactory(new PropertyValueFactory<>("alias"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dbConnectionTable.setItems(dbConnections);
        dbConnectionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        
        // Add selection listener
        dbConnectionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
                duplicateButton.setDisable(false);
            } else {
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
                duplicateButton.setDisable(true);
            }
        });
    }

    private void setupEventListeners() {
        addButton.setOnAction(event -> handleAdd());
        updateButton.setOnAction(event -> handleUpdate());
        deleteButton.setOnAction(event -> handleDelete());
        clearButton.setOnAction(event -> clearForm());
        testConnectionButton.setOnAction(event -> handleTestConnection());
        duplicateButton.setOnAction(event -> handleDuplicate());
        
        // Initially disable update, delete and duplicate buttons
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        duplicateButton.setDisable(true);
    }
    
    private void addFormListeners() {
        dbTypeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            toggleDbFields(newV);
            updateUrlField();
        });
        hostField.textProperty().addListener(obs -> updateUrlField());
        portField.textProperty().addListener(obs -> updateUrlField());
        dbNameField.textProperty().addListener(obs -> updateUrlField());
        schemaField.textProperty().addListener(obs -> updateUrlField());
        serviceNameField.textProperty().addListener(obs -> updateUrlField());
    }
    
    private void configureSpinner() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 5);
        poolSizeSpinner.setValueFactory(valueFactory);
    }

    private void configureDbTypeCombo() {
        dbTypeCombo.getItems().setAll(DbType.values());
        dbTypeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(DbType type) { return type == null ? "" : type.name(); }
            @Override public DbType fromString(String s) { return DbType.valueOf(s); }
        });
    }

    private void loadConnections() {
        Integer projectId = ProjectContext.getCurrentProjectId();
        if (projectId != null) {
            dbConnections.setAll(dbConnectionService.getConnectionsByProject(projectId));
        } else {
            dbConnections.clear();
        }
    }

    private void loadEnvironments() {
        try {
            environments.setAll(environmentService.findAllEnvironments());
            environmentCombo.setItems(environments);
            environmentCombo.setConverter(new StringConverter<>() {
                @Override public String toString(com.qa.app.model.Environment env) { return env == null ? "" : env.getName(); }
                @Override public com.qa.app.model.Environment fromString(String s) { return environments.stream().filter(e -> e.getName().equals(s)).findFirst().orElse(null); }
            });
        } catch (Exception e) {
            showStatus("Failed to load environments: " + e.getMessage(), MainViewModel.StatusType.ERROR);
        }
    }

    private void populateForm(DbConnection connection) {
        aliasField.setText(connection.getAlias());
        descriptionArea.setText(connection.getDescription());
        dbTypeCombo.getSelectionModel().select(connection.getDbType());
        hostField.setText(connection.getHost());
        portField.setText(String.valueOf(connection.getPort()));
        dbNameField.setText(connection.getDatabase());
        schemaField.setText(connection.getSchema());
        serviceNameField.setText(connection.getServiceName());
        usernameField.setText(connection.getUsername());
        passwordField.setText(connection.getPassword());
        poolSizeSpinner.getValueFactory().setValue(connection.getPoolSize());
        // select environment
        environments.stream()
                .filter(env -> env.getId() == connection.getEnvironmentId())
                .findFirst()
                .ifPresent(env -> environmentCombo.getSelectionModel().select(env));
        
        toggleDbFields(connection.getDbType());
        updateUrlField();
    }

    private void clearForm() {
        aliasField.clear();
        descriptionArea.clear();
        dbTypeCombo.getSelectionModel().clearSelection();
        hostField.clear();
        portField.clear();
        dbNameField.clear();
        schemaField.clear();
        serviceNameField.clear();
        urlField.clear();
        usernameField.clear();
        passwordField.clear();
        poolSizeSpinner.getValueFactory().setValue(5);
        dbConnectionTable.getSelectionModel().clearSelection();
        toggleDbFields(null);
    }
    
    private DbConnection getConnectionFromForm() {
        DbConnection connection;
        if (dbConnectionTable.getSelectionModel().getSelectedItem() != null) {
            connection = dbConnectionTable.getSelectionModel().getSelectedItem();
        } else {
            connection = new DbConnection();
        }

        if (ProjectContext.getCurrentProjectId() == null) {
            showStatus("Error: No project selected. Please select a project first.", MainViewModel.StatusType.ERROR);
            return null;
        }

        connection.setAlias(aliasField.getText());
        connection.setDescription(descriptionArea.getText());
        connection.setDbType(dbTypeCombo.getValue());
        connection.setHost(hostField.getText());
        try { connection.setPort(Integer.parseInt(portField.getText())); } catch (NumberFormatException ignored) {
            connection.setPort(0);
        }
        connection.setDatabase(dbNameField.getText());
        connection.setSchema(schemaField.getText());
        connection.setServiceName(serviceNameField.getText());
        // Assemble URL is now done via listener, but we ensure it's set on the object
        String url = DataSourceRegistry.buildJdbcUrl(connection);
        connection.setJdbcUrl(url);
        
        connection.setUsername(usernameField.getText());
        connection.setPassword(passwordField.getText());
        connection.setPoolSize(poolSizeSpinner.getValue());
        connection.setProjectId(ProjectContext.getCurrentProjectId());
        if (environmentCombo.getSelectionModel().getSelectedItem() != null) {
            connection.setEnvironmentId(environmentCombo.getSelectionModel().getSelectedItem().getId());
        }
        return connection;
    }

    @FXML
    private void handleAdd() {
        if (!validateForm()) {
            return;
        }
        
        DbConnection connection = getConnectionFromForm();
        if (connection != null && connection.getId() == 0 && !connection.getAlias().isEmpty()) {
            dbConnectionService.addConnection(connection);
            loadConnections();
            clearForm();
            showStatus("Database connection added successfully.", MainViewModel.StatusType.SUCCESS);
        }
    }

    @FXML
    private void handleUpdate() {
        if (!validateForm()) {
            return;
        }
        
        DbConnection connection = getConnectionFromForm();
        if (connection != null && connection.getId() > 0) {
            dbConnectionService.updateConnection(connection);
            loadConnections();
            clearForm();
            showStatus("Database connection updated successfully.", MainViewModel.StatusType.SUCCESS);
        }
    }

    @FXML
    private void handleDelete() {
        DbConnection selected = dbConnectionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            dbConnectionService.deleteConnection(selected);
            loadConnections();
            clearForm();
            showStatus("Database connection deleted successfully.", MainViewModel.StatusType.SUCCESS);
        }
    }
    
    @FXML
    private void handleDuplicate() {
        DbConnection selected = dbConnectionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Create a new connection with the same properties but a different name
            DbConnection duplicate = new DbConnection();
            duplicate.setAlias(selected.getAlias() + " (Copy)");
            duplicate.setDescription(selected.getDescription());
            duplicate.setDbType(selected.getDbType());
            duplicate.setHost(selected.getHost());
            duplicate.setPort(selected.getPort());
            duplicate.setDatabase(selected.getDatabase());
            duplicate.setSchema(selected.getSchema());
            duplicate.setServiceName(selected.getServiceName());
            duplicate.setJdbcUrl(selected.getJdbcUrl());
            duplicate.setUsername(selected.getUsername());
            duplicate.setPassword(selected.getPassword());
            duplicate.setPoolSize(selected.getPoolSize());
            duplicate.setProjectId(selected.getProjectId());
            
            // Directly add the duplicate to the database
            dbConnectionService.addConnection(duplicate);
            loadConnections();
            showStatus("Database connection duplicated successfully.", MainViewModel.StatusType.SUCCESS);
        }
    }

    private void updateUrlField() {
        DbConnection tempConn = new DbConnection();
        tempConn.setDbType(dbTypeCombo.getValue());
        tempConn.setHost(hostField.getText());
        try {
            tempConn.setPort(Integer.parseInt(portField.getText()));
        } catch (NumberFormatException e) {
            urlField.setText("");
            return;
        }
        tempConn.setDatabase(dbNameField.getText());
        tempConn.setSchema(schemaField.getText());
        tempConn.setServiceName(serviceNameField.getText());

        if (tempConn.getDbType() != null && tempConn.getHost() != null && !tempConn.getHost().isBlank() && tempConn.getPort() > 0) {
            String generatedUrl = DataSourceRegistry.buildJdbcUrl(tempConn);
            urlField.setText(generatedUrl);
        } else {
            urlField.setText("");
        }
    }
    
    private void toggleDbFields(DbType type) {
        // Hide all optional fields and their labels by default
        dbNameLabel.setVisible(false); dbNameLabel.setManaged(false);
        dbNameField.setVisible(false); dbNameField.setManaged(false);
        schemaLabel.setVisible(false); schemaLabel.setManaged(false);
        schemaField.setVisible(false); schemaField.setManaged(false);
        serviceNameLabel.setVisible(false); serviceNameLabel.setManaged(false);
        serviceNameField.setVisible(false); serviceNameField.setManaged(false);

        if (type == null) return;

        // Show fields based on the selected database type
        switch (type) {
            case ORACLE:
                serviceNameLabel.setVisible(true); serviceNameLabel.setManaged(true);
                serviceNameField.setVisible(true); serviceNameField.setManaged(true);
                break;
            case POSTGRESQL:
                dbNameLabel.setVisible(true); dbNameLabel.setManaged(true);
                dbNameField.setVisible(true); dbNameField.setManaged(true);
                schemaLabel.setVisible(true); schemaLabel.setManaged(true);
                schemaField.setVisible(true); schemaField.setManaged(true);
                break;
            case MYSQL:
            case SQLSERVER:
                dbNameLabel.setVisible(true); dbNameLabel.setManaged(true);
                dbNameField.setVisible(true); dbNameField.setManaged(true);
                break;
        }
    }
    
    @FXML
    private void handleTestConnection() {
        if (!validateForm()) {
            return;
        }
        
        DbConnection testConn = getConnectionFromForm();
        if (testConn != null) {
            try {
                // Evict from cache to ensure we test with fresh settings from the form
                DataSourceRegistry.evict(testConn.getAlias());

                // Create a temporary DataSource for testing
                DataSource ds = DataSourceRegistry.get(testConn);
                try (Connection conn = ds.getConnection()) {
                    String dbInfo = "Database: " + (conn.getCatalog() != null ? conn.getCatalog() : "Unknown") + 
                                   ", Product: " + conn.getMetaData().getDatabaseProductName() + 
                                   " " + conn.getMetaData().getDatabaseProductVersion();
                    showStatus("Connection successful! " + dbInfo, MainViewModel.StatusType.SUCCESS);
                }
            } catch (Exception e) {
                showStatus("Connection test failed: " + e.getMessage(), MainViewModel.StatusType.ERROR);
                // Also evict on failure so a bad config isn't cached for next time
                DataSourceRegistry.evict(testConn.getAlias());
            }
        }
    }
    
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        
        if (aliasField.getText().trim().isEmpty()) {
            errors.append("Alias is required. ");
        }
        
        if (dbTypeCombo.getValue() == null) {
            errors.append("Database type is required. ");
        }
        
        if (hostField.getText().trim().isEmpty()) {
            errors.append("Host is required. ");
        }
        
        if (portField.getText().trim().isEmpty()) {
            errors.append("Port is required. ");
        }
        
        if (dbNameField.getText().trim().isEmpty()) {
            errors.append("Database name is required. ");
        }
        
        if (errors.length() > 0) {
            showStatus("Validation error: " + errors.toString(), MainViewModel.StatusType.ERROR);
            return false;
        }
        
        return true;
    }

    private void showStatus(String message, MainViewModel.StatusType type) {
        if (mainViewModel != null) {
            mainViewModel.updateStatus(message, type);
        }
    }
} 