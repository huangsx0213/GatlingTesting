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

    // --- Merged from TemplateValidator ---

    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }

    public static ValidationResult validate(String content, String format) {
        if (content == null || content.isBlank()) {
            return new ValidationResult(true, null); // Empty is always valid
        }
        switch (format) {
            case "FTL":
                return validateFtl(content);
            case "JSON":
                return validateJson(content);
            case "XML":
                return validateXml(content);
            case "TEXT":
            default:
                return new ValidationResult(true, null);
        }
    }

    public static String format(String content, String format) throws Exception {
        if (content == null || content.isBlank()) {
            return "";
        }
        switch (format) {
            case "JSON":
                return formatJson(content);
            case "XML":
                return formatXml(content);
            case "FTL":
                return formatFtl(content);
            default:
                return content; // No change for TEXT
        }
    }

    // --- FTL Specific Logic ---

    private static ValidationResult validateFtl(String templateContent) {
        String processed = preprocessForFreeMarker(templateContent);
        try {
            new Template("ftl-validation", processed, staticFreemarkerCfg);
            // We could add a dummy render check here as well for deeper validation if needed.
            return new ValidationResult(true, null);
        } catch (IOException e) {
            return new ValidationResult(false, "FTL syntax error: " + e.getMessage());
        }
    }

    private static String formatFtl(String ftlContent) throws Exception {
        // This is a best-effort, intelligent formatter.
        // For FTL/XML, it uses a robust, regex-based tokenizer to provide indentation
        // without performing strict validation, thus avoiding parser crashes on complex templates.
        // For JSON, it uses the standard pretty-printer.

        String trimmedContent = ftlContent.trim();
        boolean isXml = !trimmedContent.isEmpty() && trimmedContent.startsWith("<");

        if (isXml) {
            return prettyPrintXmlFreemarker(ftlContent);
        } else {
            return prettyPrintJsonFreemarker(ftlContent);
        }
    }

    private static String prettyPrintJsonFreemarker(String source) {
        StringBuilder result = new StringBuilder();
        int indent = 0;
        String indentString = "  ";

        // Regex to split lines while respecting FTL comments
        String[] lines = source.split("\\R");

        for (String line : lines) {
            String trimmedLine = line.trim();

            // If the line is just a FreeMarker comment, indent it based on the current context and move on.
            if (trimmedLine.startsWith("[#--") && trimmedLine.endsWith("--]")) {
                if (!result.toString().isEmpty() && !result.toString().endsWith(System.lineSeparator())) {
                    result.append(System.lineSeparator());
                }
                result.append(indentString.repeat(indent));
                result.append(trimmedLine);
                result.append(System.lineSeparator()); 
                continue;
            }

            String contentPart = line;
            String commentPart = "";
            int commentStart = line.indexOf("[#--");
            if (commentStart != -1) {
                contentPart = line.substring(0, commentStart);
                commentPart = line.substring(commentStart);
            }

            Pattern pattern = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|\\[/?#?[\\s\\S]*?\\]|[\\{\\}\\[\\],:]|[^\\s\"\\{\\}\\[\\],:]+");
            Matcher matcher = pattern.matcher(contentPart);
            
            boolean needsIndent = true;
            String lastToken = result.toString().trim().endsWith("{") || result.toString().trim().endsWith("[") ? "" : "dummy";

            while (matcher.find()) {
                String token = matcher.group().trim();
                if (token.isEmpty()) continue;

                if (token.equals("}") || token.equals("]")) {
                    indent = Math.max(0, indent - 1);
                    if (!lastToken.isEmpty() && !result.toString().trim().endsWith("{") && !result.toString().trim().endsWith("[")) {
                        result.append(System.lineSeparator());
                    }
                    result.append(indentString.repeat(indent));
                } else if (needsIndent) {
                    if (!result.toString().trim().isEmpty() && !result.toString().trim().endsWith("\n")) {
                         result.append(System.lineSeparator());
                    }
                    result.append(indentString.repeat(indent));
                }

                result.append(token);

                if (token.equals("{") || token.equals("[")) {
                    indent++;
                    needsIndent = true;
                } else if (token.equals(",")) {
                    needsIndent = true;
                } else if (token.equals(":")) {
                    result.append(" ");
                    needsIndent = false;
                } else {
                    needsIndent = false;
                }
                lastToken = token;
            }

            if (!commentPart.isEmpty()) {
                result.append(" ").append(commentPart.trim());
            }

            if (matcher.find(0) || !commentPart.isEmpty()) { // Check if the line had any tokens or a comment
                 if (!result.toString().endsWith(System.lineSeparator())) {
                    result.append(System.lineSeparator());
                }
            }
        }
        String raw = result.toString();
        // Remove all empty or whitespace-only lines
        return java.util.Arrays.stream(raw.split("\\R"))
                .filter(line -> !line.trim().isEmpty())
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("");
    }

    private static String prettyPrintXmlFreemarker(String source) {
        StringBuilder result = new StringBuilder();
        int indent = 0;
        String indentString = "  ";
        // Regex to tokenize into: tags, FTL directives (including comments), and text content.
        Pattern pattern = Pattern.compile("<!--[\\s\\S]*?-->|\\[/?#?[\\s\\S]*?\\]|<[^>]+>|[^<]+");
        Matcher matcher = pattern.matcher(source);

        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.isEmpty()) continue;

            String contentPart = token;
            String commentPart = "";
            int ftlCommentStart = token.indexOf("[#--");
            
            // Handle FTL comments attached to XML tags
            if (ftlCommentStart > 0 && token.startsWith("<")) {
                contentPart = token.substring(0, ftlCommentStart).trim();
                commentPart = token.substring(ftlCommentStart).trim();
            }

            boolean isClosingTag = contentPart.startsWith("</");
            boolean isSelfClosingTag = contentPart.endsWith("/>");
            boolean isProcessingInstruction = contentPart.startsWith("<?");
            boolean isDoctype = contentPart.startsWith("<!DOCTYPE");
            boolean isFtlDirective = contentPart.startsWith("[#") || contentPart.startsWith("[/#") || contentPart.startsWith("[@");
            boolean isXmlComment = contentPart.startsWith("<!--");
            boolean isTextNode = !contentPart.startsWith("<");

            if (isClosingTag || (isFtlDirective && contentPart.startsWith("[/#"))) {
                indent = Math.max(0, indent - 1);
            }

            result.append(indentString.repeat(indent));
            result.append(contentPart);
            
            if (!commentPart.isEmpty()) {
                result.append(" ").append(commentPart);
            }

            result.append(System.lineSeparator());

            if (!isClosingTag && !isSelfClosingTag && !isProcessingInstruction && !isDoctype && !isFtlDirective && !isXmlComment && !isTextNode) {
                indent++;
            } else if (isFtlDirective && !contentPart.startsWith("[/#") && !contentPart.contains("]") ) { 
                // A simple way to handle block directives like [#if] vs inline ones like [=@...].
                // This is not perfect. A better approach would be a proper FTL parser.
                 String directiveName = contentPart.substring(2, contentPart.indexOf(" ")).trim();
                 if (!directiveName.equals("assign") && !directiveName.equals("include") && !directiveName.equals("import")) {
                    indent++;
                 }
            }
        }
        // Remove trailing newlines and return
        return result.toString().trim();
    }
    
    // This is a simplified preprocessor. The real one in FreeMarker is more complex.
    // It replaces custom variable syntax with something FreeMarker understands.
    private static String preprocessForFreeMarker(String content) {
        // Standardize user variables (@{...}) to FreeMarker syntax ([=...])
        // And also handle system property placeholders (${...}) by treating them as raw strings for now.
        // A more advanced solution would resolve these system properties if needed.
        return content.replaceAll("\\@\\{([^}]+)\\}", "[=$1]");
    }
    
    // --- JSON Specific Logic ---

    private static ValidationResult validateJson(String json) {
        try {
            jsonMapper.readTree(json);
            return new ValidationResult(true, null);
        } catch (IOException e) {
            return new ValidationResult(false, "Invalid JSON: " + e.getMessage());
        }
    }

    private static String formatJson(String json) throws IOException {
        Object jsonObj = jsonMapper.readValue(json, Object.class);
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
    }

    // --- XML Specific Logic ---

    private static ValidationResult validateXml(String xml) {
        try {
            // Use a non-validating parser just to check for well-formedness
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            return new ValidationResult(true, null);
        } catch (Exception e) {
            return new ValidationResult(false, "Invalid XML: " + e.getMessage());
        }
    }

    private static String formatXml(String xml) throws Exception {
        Source xmlInput = new StreamSource(new StringReader(xml));
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 4);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(xmlInput, xmlOutput);
        return xmlOutput.getWriter().toString();
    }
} 