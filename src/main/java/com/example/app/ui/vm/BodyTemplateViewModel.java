package com.example.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

import com.example.app.model.BodyTemplate;
import com.example.app.service.impl.BodyTemplateServiceImpl;
import com.example.app.service.api.IBodyTemplateService;
import com.example.app.service.ServiceException;

public class BodyTemplateViewModel implements Initializable {
    @FXML
    private TextField templateNameField;
    @FXML
    private TextArea bodyContentArea;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;
    @FXML
    private TableView<TemplateItem> templateTable;
    @FXML
    private TableColumn<TemplateItem, String> nameColumn;
    @FXML
    private TableColumn<TemplateItem, String> contentColumn;
    @FXML
    private ComboBox<String> bodyFormatComboBox;
    @FXML
    private Button formatButton;
    @FXML
    private Button validateButton;

    private final ObservableList<TemplateItem> templateList = FXCollections.observableArrayList();
    private final IBodyTemplateService bodyTemplateService = new BodyTemplateServiceImpl();
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        contentColumn.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        templateTable.setItems(templateList);
        templateTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> showTemplateDetails(newSel));
        loadTemplates();
        // 初始化格式下拉框
        bodyFormatComboBox.getItems().addAll("JSON", "XML", "TEXT");
        bodyFormatComboBox.getSelectionModel().selectFirst();
    }

    private void loadTemplates() {
        templateList.clear();
        try {
            for (BodyTemplate t : bodyTemplateService.findAllTemplates()) {
                templateList.add(new TemplateItem(t.getId(), t.getName(), t.getContent()));
            }
        } catch (ServiceException e) {
            // 可加错误提示
        }
    }

    private void showTemplateDetails(TemplateItem item) {
        if (item != null) {
            templateNameField.setText(item.getName());
            bodyContentArea.setText(item.getContent());
        } else {
            clearFields();
        }
    }

    @FXML
    private void handleAddTemplate() {
        String name = templateNameField.getText().trim();
        String content = bodyContentArea.getText().trim();
        if (name.isEmpty() || content.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Name and Content are required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            BodyTemplate t = new BodyTemplate(name, content);
            bodyTemplateService.createTemplate(t);
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
            BodyTemplate t = new BodyTemplate(selected.getId(), templateNameField.getText().trim(), bodyContentArea.getText().trim());
            bodyTemplateService.updateTemplate(t);
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
                bodyTemplateService.deleteTemplate(selected.getId());
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
        String format = bodyFormatComboBox.getValue();
        String content = bodyContentArea.getText();
        if (format == null || content == null || content.isEmpty()) return;
        try {
            String formatted = content;
            if ("JSON".equals(format)) {
                formatted = formatJson(content);
            } else if ("XML".equals(format)) {
                formatted = formatXml(content);
            } // TEXT 不处理
            bodyContentArea.setText(formatted);
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Format Success: Content formatted as " + format + ".", MainViewModel.StatusType.SUCCESS);
            } else {
                showAlert("Format Success", "Content formatted as " + format + ".");
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
        String format = bodyFormatComboBox.getValue();
        String content = bodyContentArea.getText();
        if (format == null || content == null || content.isEmpty()) return;
        try {
            if ("JSON".equals(format)) {
                validateJson(content);
            } else if ("XML".equals(format)) {
                validateXml(content);
            } // TEXT 不校验
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Validation Success: Content is valid " + format + ".", MainViewModel.StatusType.SUCCESS);
            } else {
                showAlert("Validation Success", "Content is valid " + format + ".");
            }
        } catch (Exception e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Validation Error: Invalid " + format + ".", MainViewModel.StatusType.ERROR);
            }
            showAlert("Validation Error", "Invalid " + format + ": " + e.getMessage());
        }
    }

    // JSON格式化
    private String formatJson(String json) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Object obj = mapper.readValue(json, Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
    // JSON校验
    private void validateJson(String json) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.readTree(json);
    }
    // XML格式化
    private String formatXml(String xml) throws Exception {
        javax.xml.transform.Source xmlInput = new javax.xml.transform.stream.StreamSource(new java.io.StringReader(xml));
        java.io.StringWriter stringWriter = new java.io.StringWriter();
        javax.xml.transform.stream.StreamResult xmlOutput = new javax.xml.transform.stream.StreamResult(stringWriter);
        javax.xml.transform.Transformer transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(xmlInput, xmlOutput);
        return stringWriter.toString();
    }
    // XML校验
    private void validateXml(String xml) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
    }
    // 弹窗
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void clearFields() {
        templateNameField.clear();
        bodyContentArea.clear();
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void refresh() {
        loadTemplates();
        clearFields();
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