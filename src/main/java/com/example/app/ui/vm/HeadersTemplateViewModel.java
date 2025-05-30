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
    private TableView<TemplateItem> templateTable;
    @FXML
    private TableColumn<TemplateItem, String> nameColumn;
    @FXML
    private TableColumn<TemplateItem, String> contentColumn;

    private final ObservableList<TemplateItem> templateList = FXCollections.observableArrayList();
    private final IHeadersTemplateService headersTemplateService = new HeadersTemplateServiceImpl();

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
        if (name.isEmpty() || content.isEmpty()) return;
        try {
            HeadersTemplate t = new HeadersTemplate(name, content);
            headersTemplateService.addTemplate(t);
            loadTemplates();
            clearFields();
        } catch (ServiceException e) {
            // 可加错误提示
        }
    }

    @FXML
    private void handleUpdateTemplate() {
        TemplateItem selected = templateTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            HeadersTemplate t = new HeadersTemplate(selected.getId(), templateNameField.getText().trim(), headersContentArea.getText().trim());
            headersTemplateService.updateTemplate(t);
            loadTemplates();
        } catch (ServiceException e) {
            // 可加错误提示
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
            } catch (ServiceException e) {
                // 可加错误提示
            }
        }
    }

    @FXML
    private void handleClearForm() {
        clearFields();
        templateTable.getSelectionModel().clearSelection();
    }

    private void clearFields() {
        templateNameField.clear();
        headersContentArea.clear();
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