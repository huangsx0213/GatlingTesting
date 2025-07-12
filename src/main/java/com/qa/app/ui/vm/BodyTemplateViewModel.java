package com.qa.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

import com.qa.app.model.BodyTemplate;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IBodyTemplateService;
import com.qa.app.service.impl.BodyTemplateServiceImpl;
import com.qa.app.util.AppConfig;
import com.qa.app.common.listeners.AppConfigChangeListener;
import com.qa.app.ui.vm.gatling.TemplateValidator;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import com.qa.app.service.ProjectContext;
import javafx.scene.control.TableView;

public class BodyTemplateViewModel implements Initializable, AppConfigChangeListener {
    @FXML
    private TextField bodyTemplateNameField;
    @FXML
    private TextArea bodyTemplateContentArea;
    @FXML
    private TextArea bodyTemplateDescriptionArea;
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
    private TableView<BodyTemplateItem> bodyTemplateTable;
    @FXML
    private TableColumn<BodyTemplateItem, String> bodyTemplateNameColumn;
    @FXML
    private TableColumn<BodyTemplateItem, String> bodyTemplateDescriptionColumn;
    @FXML
    private TableColumn<BodyTemplateItem, String> bodyTemplateContentColumn;
    @FXML
    private ComboBox<String> bodyFormatComboBox;
    @FXML
    private Button formatButton;
    @FXML
    private Button validateButton;

    private final ObservableList<BodyTemplateItem> bodyTemplateList = FXCollections.observableArrayList();
    private final IBodyTemplateService bodyTemplateService = new BodyTemplateServiceImpl();
    private MainViewModel mainViewModel;
    private Integer projectId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bodyTemplateNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        bodyTemplateDescriptionColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        bodyTemplateContentColumn.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        bodyTemplateTable.setItems(bodyTemplateList);
        bodyTemplateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bodyTemplateTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                bodyTemplateContentArea.setText(newSelection.getContent());
                bodyTemplateDescriptionArea.setText(newSelection.getDescription());
                bodyTemplateNameField.setText(newSelection.getName());
            }
        });
        loadBodyTemplates();
        AppConfig.addChangeListener(this);
        
        // Initialize format dropdown, now including FTL
        bodyFormatComboBox.getItems().addAll("FTL", "JSON", "XML", "TEXT");
        bodyFormatComboBox.getSelectionModel().selectFirst();

        // Double-click to show full content
        bodyTemplateTable.setRowFactory(tv -> {
            TableRow<BodyTemplateItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showContentInPopup(row.getItem());
                }
            });
            return row;
        });
        
        // Truncate long content in the table and add a tooltip
        setupContentColumnCellFactory();
    }
    
    @Override
    public void onConfigChanged() {
        loadBodyTemplates();
    }

    private void loadBodyTemplates() {
        bodyTemplateList.clear();
        Integer projectId = ProjectContext.getCurrentProjectId();
        if (projectId != null) {
            try {
                for (BodyTemplate t : bodyTemplateService.findBodyTemplatesByProjectId(projectId)) {
                    bodyTemplateList.add(new BodyTemplateItem(t.getId(), t.getName(), t.getContent(), t.getDescription(), t.getProjectId()));
                }
            } catch (ServiceException e) {
                showError("Failed to load templates: " + e.getMessage());
            }
        }
        if (!bodyTemplateList.isEmpty()) {
            bodyTemplateTable.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleDuplicateTemplate() {
        BodyTemplateItem selected = bodyTemplateTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a template to duplicate."); // Assuming showError handles mainViewModel status
            return;
        }
        String newName = selected.getName() + " (copy)";
        // We should add a check for name uniqueness if required by business logic.

        try {
            BodyTemplate newTemplate = new BodyTemplate(newName, selected.getContent(), selected.getDescription(), ProjectContext.getCurrentProjectId());
            bodyTemplateService.createBodyTemplate(newTemplate);
            loadBodyTemplates();
            showSuccess("Template duplicated and added successfully.");
        } catch (ServiceException e) {
            showError("Failed to duplicate template: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddBodyTemplate() {
        String name = bodyTemplateNameField.getText().trim();
        String content = bodyTemplateContentArea.getText().trim();
        String description = bodyTemplateDescriptionArea.getText().trim();
        if (name.isEmpty() || content.isEmpty()) {
            showError("Name and Content are required.");
            return;
        }
        
        // Validate before adding
        TemplateValidator.ValidationResult validationResult = TemplateValidator.validate(content, bodyFormatComboBox.getValue());
        if (!validationResult.isValid) {
            showError("Invalid Template: " + validationResult.errorMessage);
            return;
        }

        try {
            BodyTemplate t = new BodyTemplate(name, content, description, ProjectContext.getCurrentProjectId());
            bodyTemplateService.createBodyTemplate(t);
            loadBodyTemplates();
            clearFields();
            showSuccess("Template added successfully.");
        } catch (ServiceException e) {
            showError("Failed to add template: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateBodyTemplate() {
        BodyTemplateItem selected = bodyTemplateTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a template to update.");
            return;
        }
        
        String content = bodyTemplateContentArea.getText().trim();
        
        // Validate before updating
        TemplateValidator.ValidationResult validationResult = TemplateValidator.validate(content, bodyFormatComboBox.getValue());
        if (!validationResult.isValid) {
            showError("Invalid Template: " + validationResult.errorMessage);
            return;
        }

        try {
            BodyTemplate t = new BodyTemplate(selected.getId(), bodyTemplateNameField.getText().trim(), content, bodyTemplateDescriptionArea.getText().trim(), ProjectContext.getCurrentProjectId());
            bodyTemplateService.updateBodyTemplate(t);
            loadBodyTemplates();
            clearFields();
            showSuccess("Template updated successfully.");
        } catch (ServiceException e) {
            showError("Failed to update template: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteBodyTemplate() {
        BodyTemplateItem selected = bodyTemplateTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                bodyTemplateService.deleteBodyTemplate(selected.getId());
                loadBodyTemplates();
                clearFields();
                showSuccess("Template deleted successfully.");
            } catch (ServiceException e) {
                showError("Failed to delete template: " + e.getMessage());
            }
        } else {
            showError("Please select a template to delete.");
        }
    }

    @FXML
    private void handleClearBodyTemplateForm() {
        clearFields();
    }

    @FXML
    private void handleFormatTemplate() {
        String format = bodyFormatComboBox.getValue();
        String content = bodyTemplateContentArea.getText();
        if (format == null || content.isEmpty()) return;

        // Validate before formatting
        TemplateValidator.ValidationResult validationResult = TemplateValidator.validate(content, format);
        if (!validationResult.isValid) {
            showError("Validation Error: " + validationResult.errorMessage);
            return;
        }

        try {
            // Unify formatting logic: always format in place and show status.
            String formatted = TemplateValidator.format(content, format);
            bodyTemplateContentArea.setText(formatted);
            showSuccess("Format Success: Content formatted as " + format + ".");
        } catch (Exception e) {
            showError("Format Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleValidateTemplate() {
        String format = bodyFormatComboBox.getValue();
        String content = bodyTemplateContentArea.getText();
        if (format == null || content.isEmpty()) return;

        TemplateValidator.ValidationResult result = TemplateValidator.validate(content, format);

        if (result.isValid) {
            showSuccess("Validation Success: Content is valid " + format + ".");
        } else {
            showError("Validation Error: " + result.errorMessage);
        }
    }
    
    // --- Helper & UI Methods ---

    private void clearFields() {
        bodyTemplateNameField.clear();
        bodyTemplateContentArea.clear();
        bodyTemplateDescriptionArea.clear();
        bodyFormatComboBox.getSelectionModel().selectFirst();
        bodyTemplateTable.getSelectionModel().clearSelection();
    }
    
    private void setupContentColumnCellFactory() {
        bodyTemplateContentColumn.setCellFactory(col -> new TableCell<BodyTemplateItem, String>() {
            private static final int MAX_LENGTH = 200;
            private final Tooltip tooltip = new Tooltip();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.length() > MAX_LENGTH ? item.substring(0, MAX_LENGTH) + "..." : item);
                    tooltip.setText(item);
                    setTooltip(tooltip);
                }
            }
        });
    }

    private void showContentInPopup(BodyTemplateItem item) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Body Content Detail");
        alert.setHeaderText("Template: " + item.getName());
        TextArea area = new TextArea(item.getContent());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(800, 600);
        alert.getDialogPane().setContent(area);
        setDialogIcon(alert);
        alert.getButtonTypes().setAll(new ButtonType("Close", ButtonBar.ButtonData.OK_DONE));
        alert.showAndWait();
    }


    private void showSuccess(String message) {
        if (mainViewModel != null) {
            mainViewModel.updateStatus(message, MainViewModel.StatusType.SUCCESS);
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Success", message);
        }
    }

    private void showError(String message) {
        if (mainViewModel != null) {
            mainViewModel.updateStatus(message, MainViewModel.StatusType.ERROR);
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", message);
        }
    }
    
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        setDialogIcon(alert);
        alert.showAndWait();
    }
    
    private void setDialogIcon(Alert alert) {
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon/favicon.ico")));
        } catch (Exception e) {
            // ignore
        }
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void refresh() {
        loadBodyTemplates();
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    // Renamed inner class for clarity
    public static class BodyTemplateItem {
        private final int id;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty content;
        private final javafx.beans.property.SimpleStringProperty description;
        private final Integer projectId;

        public BodyTemplateItem(int id, String name, String content, String description, Integer projectId) {
            this.id = id;
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.content = new javafx.beans.property.SimpleStringProperty(content);
            this.description = new javafx.beans.property.SimpleStringProperty(description);
            this.projectId = projectId;
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public String getContent() { return content.get(); }
        public javafx.beans.property.StringProperty contentProperty() { return content; }
        public String getDescription() { return description.get(); }
        public javafx.beans.property.StringProperty descriptionProperty() { return description; }
        public Integer getProjectId() { return projectId; }
    }
} 