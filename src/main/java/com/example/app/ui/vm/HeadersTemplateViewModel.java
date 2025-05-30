package com.example.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

import com.example.app.model.HeadersTemplate;
import com.example.app.service.impl.HeadersTemplateServiceImpl;
import com.example.app.service.api.IHeadersTemplateService;
import com.example.app.service.ServiceException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

public class HeadersTemplateViewModel implements Initializable {
    @FXML
    private TextField templateNameField;
    @FXML
    private TextArea headersContentArea;
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
    private TableView<TemplateItem> templateTable;
    @FXML
    private TableColumn<TemplateItem, String> nameColumn;
    @FXML
    private TableColumn<TemplateItem, String> contentColumn;

    private final ObservableList<TemplateItem> templateList = FXCollections.observableArrayList();
    private final IHeadersTemplateService headersTemplateService = new HeadersTemplateServiceImpl();
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        contentColumn.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        templateTable.setItems(templateList);
        templateTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> showTemplateDetails(newSel));
        loadTemplates();
    }

    private void loadTemplates() {
        templateList.clear();
        try {
            for (HeadersTemplate t : headersTemplateService.getAllTemplates()) {
                templateList.add(new TemplateItem(t.getId(), t.getName(), t.getContent()));
            }
        } catch (ServiceException e) {
            // 可加错误提示
        }
    }

    private void showTemplateDetails(TemplateItem item) {
        if (item != null) {
            templateNameField.setText(item.getName());
            headersContentArea.setText(item.getContent());
        } else {
            clearFields();
        }
    }

    @FXML
    private void handleAddTemplate() {
        String name = templateNameField.getText().trim();
        String content = headersContentArea.getText().trim();
        if (name.isEmpty() || content.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Name and Content are required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            HeadersTemplate t = new HeadersTemplate(name, content);
            headersTemplateService.addTemplate(t);
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
    private void handleUpdateTemplate() {
        TemplateItem selected = templateTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select a template to update.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            HeadersTemplate t = new HeadersTemplate(selected.getId(), templateNameField.getText().trim(), headersContentArea.getText().trim());
            headersTemplateService.updateTemplate(t);
            loadTemplates();
            // 重新选中当前行
            for (TemplateItem item : templateList) {
                if (item.getId() == t.getId()) {
                    templateTable.getSelectionModel().select(item);
                    break;
                }
            }
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
    private void handleDeleteTemplate() {
        TemplateItem selected = templateTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                headersTemplateService.deleteTemplate(selected.getId());
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
    private void handleClearForm() {
        clearFields();
        templateTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleFormatTemplate() {
        String content = headersContentArea.getText();
        if (content == null || content.isEmpty()) return;
        try {
            String formatted = formatYaml(content);
            headersContentArea.setText(formatted);
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
        String content = headersContentArea.getText();
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
        templateNameField.clear();
        headersContentArea.clear();
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    // 内部类用于表格项
    public static class TemplateItem {
        private final int id;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty content;
        public TemplateItem(int id, String name, String content) {
            this.id = id;
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.content = new javafx.beans.property.SimpleStringProperty(content);
        }
        public TemplateItem(String name, String content) {
            this(0, name, content);
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