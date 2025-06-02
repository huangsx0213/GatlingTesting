package com.qa.app.ui.vm.gatling;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import com.qa.app.model.DynamicVariable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateHandler {
    private final ComboBox<String> templateComboBox;
    private final ObservableList<DynamicVariable> variables;
    private final Map<String, String> templates;
    private final Map<Integer, String> templateIdNameMap;
    private final TableView<DynamicVariable> varsTable;
    private final TableColumn<DynamicVariable, String> keyColumn;
    private final TableColumn<DynamicVariable, String> valueColumn;
    private final TextArea generatedArea;

    public TemplateHandler(
        ComboBox<String> templateComboBox,
        ObservableList<DynamicVariable> variables,
        Map<String, String> templates,
        Map<Integer, String> templateIdNameMap,
        TableView<DynamicVariable> varsTable,
        TableColumn<DynamicVariable, String> keyColumn,
        TableColumn<DynamicVariable, String> valueColumn,
        TextArea generatedArea
    ) {
        this.templateComboBox = templateComboBox;
        this.variables = variables;
        this.templates = templates;
        this.templateIdNameMap = templateIdNameMap;
        this.varsTable = varsTable;
        this.keyColumn = keyColumn;
        this.valueColumn = valueColumn;
        this.generatedArea = generatedArea;
    }

    public void setup() {
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        varsTable.setItems(variables);
        varsTable.setEditable(true);
        valueColumn.setCellFactory(col -> new TextFieldTableCell<>(new javafx.util.converter.DefaultStringConverter()) {
            @Override
            public void startEdit() {
                super.startEdit();
                TextField textField = (TextField) getGraphic();
                if (textField != null) {
                    textField.focusedProperty().addListener((obs, was, isNow) -> {
                        if (!isNow && isEditing()) {
                            commitEdit(textField.getText());
                        }
                    });
                }
            }
        });
        valueColumn.setOnEditCommit(event -> {
            DynamicVariable editedVar = event.getTableView().getItems().get(event.getTablePosition().getRow());
            editedVar.setValue(event.getNewValue());
            updateGenerated();
        });
        templateComboBox.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    populateDynamicVariables(templates.get(newValue));
                } else {
                    variables.clear();
                }
                updateGenerated();
            });
        variables.addListener((javafx.collections.ListChangeListener<DynamicVariable>) c -> updateGenerated());
        templateComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(templateComboBox.getPromptText());
                } else {
                    setText(item);
                }
            }
        });
    }

    public void populateDynamicVariables(String template) {
        variables.clear();
        if (template != null) {
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(template);
            while (matcher.find()) {
                variables.add(new DynamicVariable(matcher.group(1), ""));
            }
        }
    }

    public String buildContent() {
        String selectedTemplateName = templateComboBox.getSelectionModel().getSelectedItem();
        if (selectedTemplateName == null || !templates.containsKey(selectedTemplateName)) {
            return null;
        }
        String template = templates.get(selectedTemplateName);
        String content = template;
        for (DynamicVariable var : variables) {
            content = content.replace("${" + var.getKey() + "}", var.getValue());
        }
        return content;
    }

    public void updateGenerated() {
        String content = buildContent();
        generatedArea.setText(content == null ? "" : content);
    }

    public int getTemplateIdByName(String name) {
        for (Map.Entry<Integer, String> entry : templateIdNameMap.entrySet()) {
            if (entry.getValue().equals(name)) return entry.getKey();
        }
        return 0;
    }
} 