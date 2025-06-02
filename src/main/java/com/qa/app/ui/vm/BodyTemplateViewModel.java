package com.qa.app.ui.vm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

import com.qa.app.model.BodyTemplate;
import com.qa.app.model.Environment;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IBodyTemplateService;
import com.qa.app.service.api.IEnvironmentService;
import com.qa.app.service.impl.BodyTemplateServiceImpl;
import com.qa.app.service.impl.EnvironmentServiceImpl;

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
    @FXML
    private ComboBox<Environment> environmentComboBox;

    private final ObservableList<BodyTemplateItem> bodyTemplateList = FXCollections.observableArrayList();
    private final IBodyTemplateService bodyTemplateService = new BodyTemplateServiceImpl();
    private final IEnvironmentService environmentService = new EnvironmentServiceImpl();
    private MainViewModel mainViewModel;
    private ObservableList<Environment> environmentList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bodyTemplateNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        bodyTemplateContentColumn.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        bodyTemplateTable.setItems(bodyTemplateList);
        bodyTemplateTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> showBodyTemplateDetails(newSel));
        loadBodyTemplates();
        // 初始化格式下拉框
        bodyFormatComboBox.getItems().addAll("JSON", "XML", "TEXT");
        bodyFormatComboBox.getSelectionModel().selectFirst();
        try {
            environmentList.setAll(environmentService.findAllEnvironments());
        } catch (ServiceException e) {
            environmentList.clear();
        }
        environmentComboBox.setItems(environmentList);
        environmentComboBox.setConverter(new javafx.util.StringConverter<Environment>() {
            @Override
            public String toString(Environment env) {
                return env == null ? "" : env.getName();
            }
            @Override
            public Environment fromString(String s) {
                return environmentList.stream().filter(e -> e.getName().equals(s)).findFirst().orElse(null);
            }
        });
        environmentComboBox.setPromptText("Select Environment");
        environmentComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Environment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(environmentComboBox.getPromptText());
                } else {
                    setText(item.getName());
                }
            }
        });
        environmentComboBox.getSelectionModel().clearSelection();
    }

    private void loadBodyTemplates() {
        bodyTemplateList.clear();
        try {
            for (BodyTemplate t : bodyTemplateService.findAllBodyTemplates()) {
                bodyTemplateList.add(new BodyTemplateItem(t.getId(), t.getName(), t.getContent(), t.getEnvironmentId()));
            }
        } catch (ServiceException e) {
            // 可加错误提示
        }
    }

    private void showBodyTemplateDetails(BodyTemplateItem item) {
        if (item != null) {
            bodyTemplateNameField.setText(item.getName());
            bodyTemplateContentArea.setText(item.getContent());
            // 设置环境下拉
            Environment env = environmentList.stream().filter(e -> e.getId() == item.getEnvironmentId()).findFirst().orElse(null);
            environmentComboBox.setValue(env);
        } else {
            clearFields();
        }
    }

    @FXML
    private void handleAddBodyTemplate() {
        String name = bodyTemplateNameField.getText().trim();
        String content = bodyTemplateContentArea.getText().trim();
        Environment env = environmentComboBox.getValue();
        Integer envId = env != null ? env.getId() : null;
        if (name.isEmpty() || content.isEmpty()) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Name and Content are required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            BodyTemplate t = envId != null ? new BodyTemplate(name, content, envId) : new BodyTemplate(name, content);
            bodyTemplateService.createBodyTemplate(t);
            loadBodyTemplates();
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
    private void handleUpdateBodyTemplate() {
        BodyTemplateItem selected = bodyTemplateTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Selection Error: Please select a template to update.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            Environment env = environmentComboBox.getValue();
            Integer envId = env != null ? env.getId() : null;
            BodyTemplate t = envId != null ? new BodyTemplate(selected.getId(), bodyTemplateNameField.getText().trim(), bodyTemplateContentArea.getText().trim(), envId)
                                           : new BodyTemplate(selected.getId(), bodyTemplateNameField.getText().trim(), bodyTemplateContentArea.getText().trim());
            bodyTemplateService.updateBodyTemplate(t);
            loadBodyTemplates();
            for (BodyTemplateItem item : bodyTemplateList) {
                if (item.getId() == t.getId()) {
                    bodyTemplateTable.getSelectionModel().select(item);
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
    private void handleDeleteBodyTemplate() {
        BodyTemplateItem selected = bodyTemplateTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                bodyTemplateService.deleteBodyTemplate(selected.getId());
                loadBodyTemplates();
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
    private void handleClearBodyTemplateForm() {
        clearFields();
        bodyTemplateTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleFormatTemplate() {
        String format = bodyFormatComboBox.getValue();
        String content = bodyTemplateContentArea.getText();
        if (format == null || content == null || content.isEmpty()) return;
        try {
            String formatted = content;
            if ("JSON".equals(format)) {
                formatted = formatJson(content);
            } else if ("XML".equals(format)) {
                formatted = formatXml(content);
            } // TEXT 不处理
            bodyTemplateContentArea.setText(formatted);
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
        String content = bodyTemplateContentArea.getText();
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
        bodyTemplateNameField.clear();
        bodyTemplateContentArea.clear();
        environmentComboBox.getSelectionModel().clearSelection();
        environmentComboBox.setValue(null);
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void refresh() {
        loadBodyTemplates();
        clearFields();
    }

    // 内部类用于表格项
    public static class BodyTemplateItem {
        private final int id;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty content;
        private final int environmentId;
        public BodyTemplateItem(int id, String name, String content, int environmentId) {
            this.id = id;
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.content = new javafx.beans.property.SimpleStringProperty(content);
            this.environmentId = environmentId;
        }
        public BodyTemplateItem(String name, String content, int environmentId) {
            this(0, name, content, environmentId);
        }
        public int getId() { return id; }
        public String getName() { return name.get(); }
        public void setName(String n) { name.set(n); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public String getContent() { return content.get(); }
        public void setContent(String c) { content.set(c); }
        public javafx.beans.property.StringProperty contentProperty() { return content; }
        public int getEnvironmentId() { return environmentId; }
    }
} 