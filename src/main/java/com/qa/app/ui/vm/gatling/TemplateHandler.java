package com.qa.app.ui.vm.gatling;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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

import javafx.collections.FXCollections;
import javafx.scene.control.cell.ComboBoxTableCell;
import com.qa.app.service.runner.RuntimeTemplateProcessor;
import com.qa.app.service.util.VariableGenerator;

import javafx.stage.Stage;
import javafx.scene.image.Image;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.event.ActionEvent;
import java.util.List;
import java.util.ArrayList;

public class TemplateHandler {
    private final ComboBox<String> templateComboBox;
    private final ObservableList<DynamicVariable> variables;
    private final Map<String, String> templates;
    private final Map<Integer, String> templateIdNameMap;
    private final TableView<DynamicVariable> varsTable;
    private final TableColumn<DynamicVariable, String> keyColumn;
    private final TableColumn<DynamicVariable, String> valueColumn;
    private final TableColumn<DynamicVariable, Void> actionColumn;
    private final TextArea generatedArea;
    private final Configuration freemarkerCfg = new Configuration(new Version("2.3.32"));
    private final ObjectMapper mapper = new ObjectMapper();

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
        TableColumn<DynamicVariable, Void> actionColumn,
        TextArea generatedArea
    ) {
        this.templateComboBox = templateComboBox;
        this.variables = variables;
        this.templates = templates;
        this.templateIdNameMap = templateIdNameMap;
        this.varsTable = varsTable;
        this.keyColumn = keyColumn;
        this.valueColumn = valueColumn;
        this.actionColumn = actionColumn;
        this.generatedArea = generatedArea;
        // FreeMarker configuration
        freemarkerCfg.setDefaultEncoding("UTF-8");
        freemarkerCfg.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        freemarkerCfg.setOutputFormat(JSONOutputFormat.INSTANCE);
        // Change the syntax to square brackets to avoid conflicts with JSON syntax
        freemarkerCfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        freemarkerCfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        // Compatible mode: treat missing or null variables as empty strings instead of throwing errors
        freemarkerCfg.setClassicCompatible(true);
    }

    public void setup() {
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        varsTable.setItems(variables);
        varsTable.setEditable(true);

        // Fix: Explicitly set the column resize policy to constrained to prevent the extra empty column.
        varsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // Custom cell factory: keep combobox suggestions & support large text editing via double-click
        valueColumn.setCellFactory(col -> {
            List<String> suggestions = VariableGenerator.getInstance().getVariableDefinitions().stream()
                .map(def -> def.get("format"))
                .collect(java.util.stream.Collectors.toList());
            ComboBoxTableCell<DynamicVariable, String> cell = new ComboBoxTableCell<>(
                    new javafx.util.converter.DefaultStringConverter(),
                    FXCollections.observableArrayList(suggestions));
            cell.setComboBoxEditable(true);

            // Return configured cell
            return cell;
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

        // Configure action column (already defined in FXML)
        if (actionColumn != null) {
            actionColumn.setSortable(false);
            actionColumn.setCellFactory(col -> new TableCell<>() {
                private final Button editBtn = new Button("Edit");
                {
                    // Transparent button style
                    editBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
                    editBtn.setMaxWidth(Double.MAX_VALUE);
                    editBtn.setOnAction(evt -> {
                        DynamicVariable var = getTableView().getItems().get(getIndex());
                        String edited = showLargeTextEditDialog(var.getKey(), var.getValue());
                        if (edited != null) {
                            var.setValue(edited);
                            TemplateHandler.this.updateGenerated();
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(editBtn);
                    }
                }
            });
        }
    }

    public void setAndFormatVariables(Map<String, String> newVariables) {
        List<DynamicVariable> tempVars = new ArrayList<>();
        if (newVariables != null) {
            newVariables.forEach((key, value) -> tempVars.add(new DynamicVariable(key, value)));
        }

        // Format
        for (DynamicVariable var : tempVars) {
            String value = var.getValue();
            if (value != null && !value.isBlank() && (value.trim().startsWith("{") || value.trim().startsWith("["))) {
                try {
                    Object jsonObject = mapper.readValue(value, Object.class);
                    String formattedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
                    var.setValue(formattedJson);
                } catch (Exception e) {
                    // Ignore if it's not a valid JSON
                }
            }
        }
        
        variables.setAll(tempVars);
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
            String value = VariableGenerator.getInstance().resolveVariables(var.getValue());
            if (value != null && !value.isEmpty()) {
                dataModel.put(var.getKey(), RuntimeTemplateProcessor.convertToModelValue(value));
            }
        }

        try {
            Template template = new Template("jsonTemplate", templateStr, freemarkerCfg);
            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            return out.toString();
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            return "Template rendering error: " + e.getMessage();
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

    /**
     * Opens a resizable dialog with a TextArea for editing long / JSON values.
     * Returns the user input when OK is pressed, or null when cancelled.
     */
    private String showLargeTextEditDialog(String key, String initialValue) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Value - " + key);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType formatJsonButtonType = new ButtonType("Format JSON", ButtonBar.ButtonData.RIGHT);

        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType, formatJsonButtonType);

        TextArea textArea = new TextArea(initialValue);
        textArea.setWrapText(true);
        textArea.setPrefSize(400, 300);
        dialog.getDialogPane().setContent(textArea);
        dialog.setResizable(true);

        // Add action for the Format JSON button without closing the dialog
        final Button formatJsonButton = (Button) dialog.getDialogPane().lookupButton(formatJsonButtonType);
        formatJsonButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Object json = mapper.readValue(textArea.getText(), Object.class);
                String formattedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                textArea.setText(formattedJson);
            } catch (JsonProcessingException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("JSON Format Error");
                alert.setHeaderText("Invalid JSON");
                alert.setContentText("The text could not be formatted as JSON. Please check the syntax.");
                alert.showAndWait();
            }
            event.consume();
        });

        dialog.setResultConverter(btn -> btn == okButtonType ? textArea.getText() : null);

        // Set Icon
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon/favicon.png")));

        java.util.Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
} 