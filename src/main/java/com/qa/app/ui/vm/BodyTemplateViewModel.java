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
import com.qa.app.ui.vm.gatling.TemplateHandler;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

public class BodyTemplateViewModel implements Initializable {
    @FXML
    private TextField bodyTemplateNameField;
    @FXML
    private TextArea bodyTemplateContentArea;
    @FXML
    private Button addButton;
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
        bodyTemplateContentColumn.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        bodyTemplateTable.setItems(bodyTemplateList);
        bodyTemplateTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> showBodyTemplateDetails(newSel));
        loadBodyTemplates();
        
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
    
    private void loadBodyTemplates() {
        bodyTemplateList.clear();
        Integer projectId = AppConfig.getCurrentProjectId();
        if (projectId != null) {
            try {
                for (BodyTemplate t : bodyTemplateService.findBodyTemplatesByProjectId(projectId)) {
                    bodyTemplateList.add(new BodyTemplateItem(t.getId(), t.getName(), t.getContent(), t.getProjectId()));
                }
            } catch (ServiceException e) {
                showError("Failed to load templates: " + e.getMessage());
            }
        }
    }

    private void showBodyTemplateDetails(BodyTemplateItem item) {
        if (item != null) {
            bodyTemplateNameField.setText(item.getName());
            bodyTemplateContentArea.setText(item.getContent());
        } else {
            clearFields();
        }
    }

    @FXML
    private void handleAddBodyTemplate() {
        String name = bodyTemplateNameField.getText().trim();
        String content = bodyTemplateContentArea.getText().trim();
        if (name.isEmpty() || content.isEmpty()) {
            showError("Name and Content are required.");
            return;
        }
        
        // Validate before adding
        TemplateHandler.ValidationResult validationResult = TemplateHandler.validate(content, bodyFormatComboBox.getValue());
        if (!validationResult.isValid) {
            showError("Invalid Template: " + validationResult.errorMessage);
            return;
        }

        try {
            BodyTemplate t = new BodyTemplate(name, content, AppConfig.getCurrentProjectId());
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
        TemplateHandler.ValidationResult validationResult = TemplateHandler.validate(content, bodyFormatComboBox.getValue());
        if (!validationResult.isValid) {
            showError("Invalid Template: " + validationResult.errorMessage);
            return;
        }

        try {
            BodyTemplate t = new BodyTemplate(selected.getId(), bodyTemplateNameField.getText().trim(), content, AppConfig.getCurrentProjectId());
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

        try {
            // Unify formatting logic: always format in place and show status.
            String formatted = TemplateHandler.format(content, format);
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

        TemplateHandler.ValidationResult result = TemplateHandler.validate(content, format);

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
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon/favicon.ico")));
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
        private final Integer projectId;

        public BodyTemplateItem(int id, String name, String content, Integer projectId) {
            this.id = id;
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.content = new javafx.beans.property.SimpleStringProperty(content);
            this.projectId = projectId;
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public String getContent() { return content.get(); }
        public javafx.beans.property.StringProperty contentProperty() { return content; }
        public Integer getProjectId() { return projectId; }
    }
} 