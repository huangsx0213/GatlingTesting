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
import javafx.scene.control.ButtonType;

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
        // 初始化格式下拉框
        bodyFormatComboBox.getItems().addAll("JSON", "XML", "TEXT");
        bodyFormatComboBox.getSelectionModel().selectFirst();

        // 双击行弹窗显示content
        bodyTemplateTable.setRowFactory(tv -> {
            TableRow<BodyTemplateItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    BodyTemplateItem item = row.getItem();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Body Content Detail");
                    alert.setHeaderText("Template: " + item.getName());
                    TextArea area = new TextArea(item.getContent());
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.setPrefWidth(800);
                    area.setPrefHeight(600);
                    alert.getDialogPane().setContent(area);
                    // 设置icon
                    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                    try {
                        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon/favicon.ico")));
                    } catch (Exception e) {
                        // ignore
                    }
                    // 设置按钮为Close
                    alert.getButtonTypes().setAll(new ButtonType("Close", ButtonBar.ButtonData.OK_DONE));
                    alert.showAndWait();
                }
            });
            return row;
        });

        // 限制content列每行最大高度和内容长度，并加Tooltip
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
                    if (item.length() > MAX_LENGTH) {
                        setText(item.substring(0, MAX_LENGTH) + "...");
                    } else {
                        setText(item);
                    }
                    tooltip.setText(item);
                    setTooltip(tooltip);
                    setWrapText(false);
                    setPrefHeight(80); // 限制最大高度
                }
            }
        });
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
                // 可加错误提示
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
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Input Error: Name and Content are required.", MainViewModel.StatusType.ERROR);
            }
            return;
        }
        try {
            BodyTemplate t = new BodyTemplate(name, content, AppConfig.getCurrentProjectId());
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
            BodyTemplate t = new BodyTemplate(selected.getId(), bodyTemplateNameField.getText().trim(), bodyTemplateContentArea.getText().trim(), AppConfig.getCurrentProjectId());
            bodyTemplateService.updateBodyTemplate(t);
            loadBodyTemplates();
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
        bodyFormatComboBox.getSelectionModel().selectFirst();
        bodyTemplateTable.getSelectionModel().clearSelection();
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

        public BodyTemplateItem(String name, String content) {
            this.id = 0; // Or handle as needed, maybe throw exception or set a default
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.content = new javafx.beans.property.SimpleStringProperty(content);
            this.projectId = null;
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public void setName(String n) { name.set(n); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public String getContent() { return content.get(); }
        public void setContent(String c) { content.set(c); }
        public javafx.beans.property.StringProperty contentProperty() { return content; }
        public Integer getProjectId() { return projectId; }
    }
} 