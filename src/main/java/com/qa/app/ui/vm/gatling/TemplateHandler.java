package com.qa.app.ui.vm.gatling;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import com.qa.app.model.DynamicVariable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import java.io.IOException;
import java.io.StringWriter;
import freemarker.core.JSONOutputFormat;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.StringReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;

public class TemplateHandler {
    private final ComboBox<String> templateComboBox;
    private final ObservableList<DynamicVariable> variables;
    private final Map<String, String> templates;
    private final Map<Integer, String> templateIdNameMap;
    private final TableView<DynamicVariable> varsTable;
    private final TableColumn<DynamicVariable, String> keyColumn;
    private final TableColumn<DynamicVariable, String> valueColumn;
    private final TextArea generatedArea;
    private final Configuration freemarkerCfg = new Configuration(new Version("2.3.32"));
    private static final ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final Configuration staticFreemarkerCfg;

    static {
        staticFreemarkerCfg = new Configuration(new Version("2.3.32"));
        staticFreemarkerCfg.setDefaultEncoding("UTF-8");
        staticFreemarkerCfg.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        staticFreemarkerCfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        staticFreemarkerCfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        staticFreemarkerCfg.setClassicCompatible(true);
    }

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
        // FreeMarker 配置
        freemarkerCfg.setDefaultEncoding("UTF-8");
        freemarkerCfg.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        freemarkerCfg.setOutputFormat(JSONOutputFormat.INSTANCE);
        // 将语法更改为方括号，以避免与JSON语法冲突
        freemarkerCfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        freemarkerCfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        // 兼容模式：将不存在或为null的变量视为空字符串，而不是抛出错误
        freemarkerCfg.setClassicCompatible(true);
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
        if (template == null || template.isBlank()) {
            return;
        }

        // Final, user-driven approach: Only extract variables explicitly defined with @{...}
        // This syntax is unique and avoids conflicts with system property placeholders like ${...}.
        Pattern pattern = Pattern.compile("\\@\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(template);
        Set<String> foundVars = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            foundVars.add(matcher.group(1).trim());
        }

        for (String varName : foundVars) {
            variables.add(new DynamicVariable(varName, ""));
        }
    }

    public String buildContent() {
        String selectedTemplateName = templateComboBox.getSelectionModel().getSelectedItem();
        if (selectedTemplateName == null || !templates.containsKey(selectedTemplateName)) {
            return null;
        }
        String templateStr = templates.get(selectedTemplateName);

        // Bridge Step: Convert user's @{...} syntax to FreeMarker's [=...] syntax
        // This allows user-friendly variable definition while retaining a conflict-free engine syntax.
        if (templateStr != null) {
             templateStr = preprocessForFreeMarker(templateStr);
        }

        // Pre-existing fix: handle unintentionally escaped quotes
        if (templateStr != null) {
            templateStr = templateStr.replace("\\\"", "\"");
        }
        
        // Build the data model
        Map<String, Object> dataModel = new java.util.HashMap<>();
        for (DynamicVariable var : variables) {
            String value = var.getValue();
            // 关键修复：只有当变量有实际值时，才将其加入数据模型。
            // 这样，值为空的变量在FreeMarker看来才是"缺失"的，从而使默认值"!"能够生效。
            if (value != null && !value.isEmpty()) {
                dataModel.put(var.getKey(), value);
            }
        }

        try {
            Template template = new Template("jsonTemplate", templateStr, freemarkerCfg);
            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            return out.toString();
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            return "模板渲染错误: " + e.getMessage();
        }
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

    private static String preprocessForFreeMarker(String content) {
        if (content == null) return null;
        // The [@...] syntax is used for FreeMarker's built-in features (e.g., list iteration).
        // By replacing user-defined @{...} with [=...], we avoid syntax collisions.
        // ${...} is reserved for system/environment properties, so we maintain that separation.
        return content.replaceAll("@\\{([^}]+)\\}", "[=$1]");
    }
} 