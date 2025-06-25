package com.qa.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.control.ButtonType;

import java.net.URL;
import java.util.ResourceBundle;

import org.yaml.snakeyaml.Yaml;

import com.qa.app.model.HeadersTemplate;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IHeadersTemplateService;
import com.qa.app.service.impl.HeadersTemplateServiceImpl;

import org.yaml.snakeyaml.DumperOptions;

import com.qa.app.util.AppConfig;

public class HeadersTemplateViewModel implements Initializable {
    @FXML
    private TextField headersTemplateNameField;
    @FXML
    private TextArea headersTemplateContentArea;
    @FXML
    private Button addButton;
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
    private TableColumn<HeadersTemplateItem, String> headersTemplateContentColumn;

    private final ObservableList<HeadersTemplateItem> headersTemplateList = FXCollections.observableArrayList();
    private final IHeadersTemplateService headersTemplateService = new HeadersTemplateServiceImpl();
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        headersTemplateNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        headersTemplateContentColumn.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        headersTemplateTable.setItems(headersTemplateList);
        headersTemplateTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> showHeadersTemplateDetails(newSel));
        loadHeadersTemplates();

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
                        stage.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon/favicon.ico")));
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

        // add Tooltip and max height, length limit to content column
        headersTemplateContentColumn.setCellFactory(col -> new TableCell<HeadersTemplateItem, String>() {
            private static final int MAX_LENGTH = 200;
            private final Tooltip tooltip = new Tooltip();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    if (item.length() > MAX_LENGTH) {
                        setText(item.substring(0, MAX_LENGTH) + "...");
                    } else {
                        setText(item);
                    }
                    tooltip.setText(item);
                    setTooltip(tooltip);
                    setWrapText(false);
                    setPrefHeight(80);
                }
            }
        });
    }

    private void loadHeadersTemplates() {
        headersTemplateList.clear();
        Integer projectId = AppConfig.getCurrentProjectId();
        if (projectId != null) {
            try {
                for (HeadersTemplate t : headersTemplateService.getHeadersTemplatesByProjectId(projectId)) {
                    headersTemplateList.add(new HeadersTemplateItem(t.getId(), t.getName(), t.getContent()));
                }
            } catch (ServiceException e) {
                // add error hint
            }
        }
    }

    private void showHeadersTemplateDetails(HeadersTemplateItem item) {
        if (item != null) {
            headersTemplateNameField.setText(item.getName());
            headersTemplateContentArea.setText(item.getContent());
        } else {
            clearFields();
        }
    }

    @FXML
    private void handleAddHeadersTemplate() {
        String name = headersTemplateNameField.getText().trim();
        String content = headersTemplateContentArea.getText().trim();
        if (name.isEmpty() || content.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Name and Content are required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            HeadersTemplate t = new HeadersTemplate(name, content);
            t.setProjectId(AppConfig.getCurrentProjectId());
            headersTemplateService.addHeadersTemplate(t);
            loadHeadersTemplates();
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
        try {
            HeadersTemplate t = new HeadersTemplate(selected.getId(), headersTemplateNameField.getText().trim(), headersTemplateContentArea.getText().trim(), AppConfig.getCurrentProjectId());
            headersTemplateService.updateHeadersTemplate(t);
            loadHeadersTemplates();
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
                loadHeadersTemplates();
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
            }
            showAlert("Format Error", "Failed to format content: " + e.getMessage());
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
                mainViewModel.updateStatus("Validation Error: Invalid YAML.", MainViewModel.StatusType.ERROR);
            }
            showAlert("Validation Error", "Invalid YAML: " + e.getMessage());
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
        headersTemplateTable.getSelectionModel().clearSelection();
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void refresh() {
        loadHeadersTemplates();
    }

    public static class HeadersTemplateItem {
        private final int id;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty content;

        public HeadersTemplateItem(int id, String name, String content) {
            this.id = id;
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.content = new javafx.beans.property.SimpleStringProperty(content);
        }

        public HeadersTemplateItem(String name, String content) {
            this.id = 0; // Or handle as needed
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.content = new javafx.beans.property.SimpleStringProperty(content);
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public void setName(String n) { name.set(n); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public String getContent() { return content.get(); }
        public void setContent(String c) { content.set(c); }
        public javafx.beans.property.StringProperty contentProperty() { return content; }
    }
} 