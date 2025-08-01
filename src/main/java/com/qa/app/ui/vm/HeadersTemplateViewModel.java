package com.qa.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

import org.yaml.snakeyaml.Yaml;

import com.qa.app.model.HeadersTemplate;
import com.qa.app.service.ProjectContext;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IHeadersTemplateService;
import com.qa.app.service.impl.HeadersTemplateServiceImpl;
import com.qa.app.ui.util.ClickableTooltipTableCell;
import com.qa.app.util.AppConfig;

import org.yaml.snakeyaml.DumperOptions;

import com.qa.app.common.listeners.AppConfigChangeListener;

public class HeadersTemplateViewModel implements Initializable, AppConfigChangeListener {
    @FXML
    private TextField headersTemplateNameField;
    @FXML
    private TextArea headersTemplateContentArea;
    @FXML
    private TextArea headersTemplateDescriptionArea;
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
    private Button formatButton;
    @FXML
    private Button validateButton;
    @FXML
    private TableView<HeadersTemplateItem> headersTemplateTable;
    @FXML
    private TableColumn<HeadersTemplateItem, String> headersTemplateNameColumn;
    @FXML
    private TableColumn<HeadersTemplateItem, String> headersTemplateDescriptionColumn;
    @FXML
    private TableColumn<HeadersTemplateItem, String> headersTemplateContentColumn;

    @FXML
    private Button moveUpButton;

    @FXML
    private Button moveDownButton;

    private final ObservableList<HeadersTemplateItem> headersTemplateList = FXCollections.observableArrayList();
    private final IHeadersTemplateService headersTemplateService = new HeadersTemplateServiceImpl();
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        headersTemplateNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        headersTemplateDescriptionColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        headersTemplateContentColumn.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        headersTemplateTable.setItems(headersTemplateList);
        headersTemplateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        headersTemplateTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                headersTemplateContentArea.setText(newSelection.getContent());
                headersTemplateNameField.setText(newSelection.getName());
                headersTemplateDescriptionArea.setText(newSelection.getDescription());
            }
        });
        loadTemplates();
        AppConfig.addChangeListener(this);

        // double click row to show content in popup
        headersTemplateTable.setRowFactory(tv -> {
            TableRow<HeadersTemplateItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    HeadersTemplateItem item = row.getItem();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Headers Content Detail");
                    alert.setHeaderText("Template: " + item.getName());
                    TextArea area = new TextArea(item.getContent());
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.setPrefWidth(800);
                    area.setPrefHeight(600);
                    alert.getDialogPane().setContent(area);
                    // set icon
                    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                    try {
                        stage.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon/favicon.png")));
                    } catch (Exception e) {
                        // ignore
                    }
                    // set button to Close
                    alert.getButtonTypes().setAll(new ButtonType("Close", ButtonBar.ButtonData.OK_DONE));
                    alert.showAndWait();
                }
            });
            return row;
        });

        headersTemplateDescriptionColumn.setCellFactory(param -> new ClickableTooltipTableCell<>());
        headersTemplateContentColumn.setCellFactory(param -> new ClickableTooltipTableCell<>());

        boolean disabled = headersTemplateList.isEmpty();
        if (moveUpButton != null) moveUpButton.setDisable(disabled);
        if (moveDownButton != null) moveDownButton.setDisable(disabled);
    }

    @Override
    public void onConfigChanged() {
        loadTemplates();
    }

    private void loadTemplates() {
        headersTemplateList.clear();
        Integer projectId = ProjectContext.getCurrentProjectId();
        if (projectId != null) {
            try {
                for (HeadersTemplate t : headersTemplateService.getHeadersTemplatesByProjectId(projectId)) {
                    headersTemplateList.add(new HeadersTemplateItem(t.getId(), t.getName(), t.getContent(), t.getDescription()));
                }
            } catch (ServiceException e) {
                // add error hint
            }
        }
        if (!headersTemplateList.isEmpty()) {
            headersTemplateTable.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleDuplicateTemplate() {
        HeadersTemplateItem selected = headersTemplateTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Please select a template to duplicate.", MainViewModel.StatusType.ERROR);
            }
            return;
        }

        String newName = selected.getName() + " (copy)";
        // Note: We might need a better way to ensure uniqueness if needed.

        try {
            HeadersTemplate newTemplate = new HeadersTemplate(newName, selected.getContent(), selected.getDescription(), ProjectContext.getCurrentProjectId());
            headersTemplateService.addHeadersTemplate(newTemplate);
            loadTemplates();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Template duplicated and added successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to duplicate template: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleAddHeadersTemplate() {
        String name = headersTemplateNameField.getText().trim();
        String content = headersTemplateContentArea.getText().trim();
        String description = headersTemplateDescriptionArea.getText().trim();
        if (name.isEmpty() || content.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Name and Content are required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        // Validate YAML before saving
        try {
            validateYaml(content);
        } catch (Exception ex) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Validation Error: Invalid YAML. " + ex.getMessage(), MainViewModel.StatusType.ERROR);
            } else {
                showAlert("Validation Error", "Invalid YAML: " + ex.getMessage());
            }
            return;
        }

        try {
            HeadersTemplate t = new HeadersTemplate(name, content, description, ProjectContext.getCurrentProjectId());
            headersTemplateService.addHeadersTemplate(t);
            loadTemplates();
            clearFields();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Template added successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to add template: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleUpdateHeadersTemplate() {
        HeadersTemplateItem selected = headersTemplateTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select a template to update.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        // Validate YAML before updating
        String updatedContent = headersTemplateContentArea.getText().trim();
        try {
            validateYaml(updatedContent);
        } catch (Exception ex) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Validation Error: Invalid YAML. " + ex.getMessage(), MainViewModel.StatusType.ERROR);
            } else {
                showAlert("Validation Error", "Invalid YAML: " + ex.getMessage());
            }
            return;
        }

        try {
            HeadersTemplate t = new HeadersTemplate(selected.getId(), headersTemplateNameField.getText().trim(), updatedContent, headersTemplateDescriptionArea.getText().trim(), ProjectContext.getCurrentProjectId());
            headersTemplateService.updateHeadersTemplate(t);
            loadTemplates();
            clearFields();
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Template updated successfully.", MainViewModel.StatusType.SUCCESS);
            }
        } catch (ServiceException e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to update template: " + e.getMessage(), MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleDeleteHeadersTemplate() {
        HeadersTemplateItem selected = headersTemplateTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                headersTemplateService.deleteHeadersTemplate(selected.getId());
                loadTemplates();
                clearFields();
                if (mainViewModel != null) {
                    mainViewModel.updateStatus("Template deleted successfully.", MainViewModel.StatusType.SUCCESS);
                }
            } catch (ServiceException e) {
                if (mainViewModel != null) {
                    mainViewModel.updateStatus("Failed to delete template: " + e.getMessage(), MainViewModel.StatusType.ERROR);
                }
            }
        } else {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select a template to delete.", MainViewModel.StatusType.ERROR);
            }
        }
    }

    @FXML
    private void handleClearHeadersTemplateForm() {
        clearFields();
    }

    @FXML
    private void handleFormatTemplate() {
        String content = headersTemplateContentArea.getText();
        if (content == null || content.isEmpty()) return;
        try {
            // 先校验，后格式化
            validateYaml(content);
            String formatted = formatYaml(content);
            headersTemplateContentArea.setText(formatted);
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Format Success: Content formatted as YAML.", MainViewModel.StatusType.SUCCESS);
            } else {
                showAlert("Format Success", "Content formatted as YAML.");
            }
        } catch (Exception e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Format Error: Failed to format content.", MainViewModel.StatusType.ERROR);
            } else {
                showAlert("Format Error", "Failed to format content: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleValidateTemplate() {
        String content = headersTemplateContentArea.getText();
        if (content == null || content.isEmpty()) return;
        try {
            validateYaml(content);
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Validation Success: Content is valid YAML.", MainViewModel.StatusType.SUCCESS);
            } else {
                showAlert("Validation Success", "Content is valid YAML.");
            }
        } catch (Exception e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Validation Error: Invalid YAML. " + e.getMessage(), MainViewModel.StatusType.ERROR);
            } else {
                showAlert("Validation Error", "Invalid YAML: " + e.getMessage());
            }
        }
    }

    private String formatYaml(String yamlStr) {
        Yaml yaml = new Yaml();
        Object obj = yaml.load(yamlStr);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(1);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Yaml prettyYaml = new Yaml(options);
        return prettyYaml.dump(obj);
    }

    private void validateYaml(String yamlStr) {
        Yaml yaml = new Yaml();
        yaml.load(yamlStr); // throws exception if invalid
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void clearFields() {
        headersTemplateNameField.clear();
        headersTemplateContentArea.clear();
        headersTemplateDescriptionArea.clear();
        headersTemplateTable.getSelectionModel().clearSelection();
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void refresh() {
        loadTemplates();
    }

    public static class HeadersTemplateItem {
        private final int id;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty content;
        private final javafx.beans.property.SimpleStringProperty description;

        public HeadersTemplateItem(int id, String name, String content, String description) {
            this.id = id;
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.content = new javafx.beans.property.SimpleStringProperty(content);
            this.description = new javafx.beans.property.SimpleStringProperty(description);
        }

        public HeadersTemplateItem(String name, String content, String description) {
            this.id = 0;
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.content = new javafx.beans.property.SimpleStringProperty(content);
            this.description = new javafx.beans.property.SimpleStringProperty(description);
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public void setName(String n) { name.set(n); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public String getContent() { return content.get(); }
        public void setContent(String c) { content.set(c); }
        public javafx.beans.property.StringProperty contentProperty() { return content; }
        public String getDescription() { return description.get(); }
        public void setDescription(String d) { description.set(d); }
        public javafx.beans.property.StringProperty descriptionProperty() { return description; }
    }

    // ================= Move Up / Down =================
    @FXML
    private void handleMoveUp() {
        HeadersTemplateItem selected = headersTemplateTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int idx = headersTemplateList.indexOf(selected);
        if (idx > 0) {
            HeadersTemplateItem above = headersTemplateList.get(idx - 1);
            headersTemplateList.set(idx - 1, selected);
            headersTemplateList.set(idx, above);
            headersTemplateTable.getSelectionModel().select(idx - 1);
        }
    }

    @FXML
    private void handleMoveDown() {
        HeadersTemplateItem selected = headersTemplateTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int idx = headersTemplateList.indexOf(selected);
        if (idx < headersTemplateList.size() - 1) {
            HeadersTemplateItem below = headersTemplateList.get(idx + 1);
            headersTemplateList.set(idx + 1, selected);
            headersTemplateList.set(idx, below);
            headersTemplateTable.getSelectionModel().select(idx + 1);
        }
    }
} 