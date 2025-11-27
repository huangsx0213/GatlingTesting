package com.qa.app.ui.vm;

import com.qa.app.model.GatlingTest;
import com.qa.app.model.ScenarioStep;
import com.qa.app.model.ResponseCheck;
import com.qa.app.model.CheckType;
import com.qa.app.model.Operator;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IGatlingTestService;
import com.qa.app.service.impl.GatlingTestServiceImpl;
import com.qa.app.service.util.VariableGenerator;
import com.qa.app.ui.util.DialogHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.control.ButtonBar.ButtonData;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;
import java.io.StringWriter;
import com.qa.app.service.runner.RuntimeTemplateProcessor;

import java.util.*;
import java.util.stream.Collectors;

public class StepOverrideEditorViewModel {
    @FXML private TabPane tabPane;
    @FXML private Label titleLabel;
    @FXML private Label headerTemplateLabel;
    @FXML private Label bodyTemplateLabel;
    @FXML private TableView<VariableOverrideRow> bodyVariablesTable;
    @FXML private TableColumn<VariableOverrideRow, String> bodyVarNameCol;
    @FXML private TableColumn<VariableOverrideRow, String> bodyVarOriginalValueCol;
    @FXML private TableColumn<VariableOverrideRow, String> bodyVarOverrideValueCol;
    @FXML private TableView<VariableOverrideRow> headersTable;
    @FXML private TableColumn<VariableOverrideRow, String> headerNameCol;
    @FXML private TableColumn<VariableOverrideRow, String> headerOriginalValueCol;
    @FXML private TableColumn<VariableOverrideRow, String> headerOverrideValueCol;
    @FXML private TextArea headersPreviewArea;
    @FXML private TextArea bodyPreviewArea;
    @FXML private TableColumn<VariableOverrideRow, Void> headerActionCol;
    @FXML private TableColumn<VariableOverrideRow, Void> bodyActionCol;
    //
    @FXML private TableView<ResponseCheck> responseChecksTable;
    @FXML private TableColumn<ResponseCheck, CheckType> checkTypeColumn;
    @FXML private TableColumn<ResponseCheck, String> checkExpressionColumn;
    @FXML private TableColumn<ResponseCheck, Operator> checkOperatorColumn;
    @FXML private TableColumn<ResponseCheck, String> checkExpectColumn;
    @FXML private TableColumn<ResponseCheck, String> checkSaveAsColumn;
    //
    //

    private Stage dialogStage;
    private ScenarioStep step;
    private GatlingTest originalTest;
    private boolean saved;

    private final ObservableList<VariableOverrideRow> bodyVarData = FXCollections.observableArrayList();
    private final ObservableList<VariableOverrideRow> headerVarData = FXCollections.observableArrayList();
    private final ObservableList<ResponseCheck> responseCheckData = FXCollections.observableArrayList();
    private static final List<ResponseCheck> responseCheckClipboard = new ArrayList<>();

    private final IGatlingTestService gatlingTestService = new GatlingTestServiceImpl();

    private final Map<String, VariableOverrideRow> originalBodySnapshot = new LinkedHashMap<>();
    private final Map<String, VariableOverrideRow> originalHeaderSnapshot = new LinkedHashMap<>();

    private Set<String> initialSourceBodyNames = new HashSet<>();
    private Set<String> initialSourceHeaderNames = new HashSet<>();

    private String bodyTemplateContent;
    private String headersTemplateContent;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Configuration freemarkerCfg = new Configuration(new Version("2.3.32"));

    @FXML private void initialize() {
        setupTable(bodyVariablesTable, bodyVarNameCol, bodyVarOriginalValueCol, bodyVarOverrideValueCol, bodyActionCol,
                bodyVarData);
        setupTable(headersTable, headerNameCol, headerOriginalValueCol, headerOverrideValueCol, headerActionCol,
                headerVarData);
        // Add listeners to refresh preview when data changes (override edits already
        // call refreshEverything)
        if (headersPreviewArea != null || bodyPreviewArea != null) {
            // simple periodic refresh through table edits; additional manual trigger below
        }
        if (tabPane != null) {
            tabPane.getSelectionModel().select(1);
        }
        setupResponseChecksTable();
        
        freemarkerCfg.setDefaultEncoding("UTF-8");
        freemarkerCfg.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        freemarkerCfg.setOutputFormat(freemarker.core.JSONOutputFormat.INSTANCE);
        freemarkerCfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        freemarkerCfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        freemarkerCfg.setClassicCompatible(true);
    }

    private void setupTable(TableView<VariableOverrideRow> table,
            TableColumn<VariableOverrideRow, String> nameCol,
            TableColumn<VariableOverrideRow, String> originalCol,
            TableColumn<VariableOverrideRow, String> overrideCol,
            TableColumn<VariableOverrideRow, Void> actionCol,
            ObservableList<VariableOverrideRow> data) {
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        originalCol.setCellValueFactory(c -> c.getValue().originalValueProperty());
        overrideCol.setCellValueFactory(c -> c.getValue().overrideValueProperty());
        // Use ComboBoxTableCell for override values
        overrideCol.setCellFactory(col -> {
            ComboBoxTableCell<VariableOverrideRow, String> cell = new ComboBoxTableCell<>();
            cell.setComboBoxEditable(true);
            cell.getItems().addAll(
                    VariableGenerator.getInstance().getVariableDefinitions().stream().map(
                            def -> def.get("format")).map(String::valueOf)
                            .collect(Collectors.toList()));
            return cell;
        });
        overrideCol.setOnEditCommit(evt -> {
            evt.getRowValue().setOverrideValue(evt.getNewValue());
            refreshEverything();
        });
        if (actionCol != null) {
            actionCol.setCellFactory(col -> new TableCell<>() {
                private final Button editBtn = new Button("Edit");
                {
                    editBtn.setStyle(
                            "-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
                    editBtn.setMaxWidth(Double.MAX_VALUE);
                    editBtn.setOnAction(evt -> {
                        VariableOverrideRow row = getTableView().getItems().get(getIndex());
                        String edited = DialogHelper.showLargeTextEditor(
                                "Edit Override - " + row.getName(),
                                row.getOverrideValue(),
                                editBtn,
                                true,
                                500,
                                360);
                        if (edited != null) {
                            row.setOverrideValue(edited);
                            refreshEverything();
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : editBtn);
                }
            });
        }
        table.setItems(data);
        table.setEditable(true);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(VariableOverrideRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    refreshRowStyle(item, this);
                }
            }
        });
    }

    private void refreshRowStyle(VariableOverrideRow row, TableRow<VariableOverrideRow> tr) {
        if (tr == null)
            return;
        tr.getStyleClass().removeAll("override-deleted", "override-added", "override-modified",
                "override-missing-source");
        if (row.getOverrideValue() != null && !row.getOverrideValue().isBlank()
                && !Objects.equals(row.getOverrideValue(), row.getOriginalValue()))
            tr.getStyleClass().add("override-modified");
        if (!row.isNewlyAdded() && row.getOriginalValue() != null && row.getOriginalValue().isBlank()) {
            tr.getStyleClass().add("override-added");
        }
        if (initialSourceBodyNames.contains(row.getName()) || initialSourceHeaderNames.contains(row.getName())) {
            tr.getStyleClass().add("override-missing-source");
        }
    }

    private void refreshEverything() {
        if (bodyVariablesTable != null)
            bodyVariablesTable.refresh();
        if (headersTable != null)
            headersTable.refresh();
        updatePreview();
    }

    private void updatePreview() {
        if (headersPreviewArea != null)
            headersPreviewArea.setText(buildHeadersPreview());
        if (bodyPreviewArea != null)
            bodyPreviewArea.setText(buildBodyPreview());
    }

    private Map<String, String> mergedHeaderVars() {
        Map<String, String> base = originalTest != null && originalTest.getHeadersDynamicVariables() != null
                ? originalTest.getHeadersDynamicVariables()
                : Collections.emptyMap();
        Map<String, String> merged = new LinkedHashMap<>(base);
        for (VariableOverrideRow r : headerVarData) {
            String ov = normalizeDisplayOverride(r);
            if (ov != null)
                merged.put(r.getName(), ov);
        }
        return merged;
    }

    private Map<String, String> mergedBodyVars() {
        Map<String, String> base = originalTest != null && originalTest.getBodyDynamicVariables() != null
                ? originalTest.getBodyDynamicVariables()
                : Collections.emptyMap();
        Map<String, String> merged = new LinkedHashMap<>(base);
        for (VariableOverrideRow r : bodyVarData) {
            String ov = normalizeDisplayOverride(r);
            if (ov != null)
                merged.put(r.getName(), ov);
        }
        return merged;
    }

    private String buildHeadersPreview() {
        Map<String, String> merged = mergedHeaderVars();
        String templ = headersTemplateContent;
        if (templ == null || templ.isBlank()) {
            // fallback to list
            return merged.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"));
        }
        return renderTemplate(templ, merged);
    }

    private String buildBodyPreview() {
        Map<String, String> merged = mergedBodyVars();
        String templ = bodyTemplateContent;
        if (templ == null || templ.isBlank()) {
            return merged.entrySet().stream().map(e -> e.getKey() + " = " + e.getValue())
                    .collect(Collectors.joining("\n"));
        }
        return renderTemplate(templ, merged);
    }

    private String renderTemplate(String template, Map<String, String> vars) {
        if (template == null) return "";
    
        String templateStr = preprocessForFreeMarker(template);
    
        // Pre-existing fix: handle unintentionally escaped quotes
        if (templateStr != null) {
            templateStr = templateStr.replace("\\\"", "\"");
        }
        
        // Build the data model
        Map<String, Object> dataModel = new java.util.HashMap<>();
        VariableGenerator varGen = VariableGenerator.getInstance();
        for (Map.Entry<String, String> var : vars.entrySet()) {
            String value = varGen.resolveVariables(var.getValue());
            if (value != null && !value.isEmpty()) {
                dataModel.put(var.getKey(), RuntimeTemplateProcessor.convertToModelValue(value));
            }
        }
    
        try {
            Template fmTemplate = new Template("jsonTemplate", templateStr, freemarkerCfg);
            StringWriter out = new StringWriter();
            fmTemplate.process(dataModel, out);
            return out.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Template rendering error: " + e.getMessage();
        }
    }
    
    private static String preprocessForFreeMarker(String content) {
        if (content == null) return null;
        return content.replaceAll("@\\{([^}]+)\\}", "[=$1]");
    }

    private String normalizeDisplayOverride(VariableOverrideRow r) {
        String ov = r.getOverrideValue();
        if (ov == null || ov.isBlank())
            return null; // treat blank as no effective override
        return ov;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        if (dialogStage != null) {
            dialogStage.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon/favicon.png")));
            // Center the dialog on the main window
            if (dialogStage.getOwner() != null) {
                DialogHelper.centerDialogOnOwner(dialogStage.getOwner(), dialogStage);
            }
            try {
                String css = getClass().getResource("/static/css/overrides.css").toExternalForm();
                dialogStage.getScene().getStylesheets().add(css);
            } catch (Exception ignore) {
            }
        }
    }

    public void setStep(ScenarioStep step) {
        this.step = step;
        if (step.getBodyVariableOverrides() == null)
            step.setBodyVariableOverrides(new LinkedHashMap<>());
        if (step.getHeadersVariableOverrides() == null)
            step.setHeadersVariableOverrides(new LinkedHashMap<>());
        try {
            this.originalTest = gatlingTestService.findTestByTcid(step.getTestTcid());
        } catch (ServiceException e) {
            this.originalTest = null;
        }
        titleLabel.setText("Edit Overrides for Step: " + step.getTestTcid());
        if (headerTemplateLabel != null)
            headerTemplateLabel.setText("Header Template: " + resolveHeadersTemplateName());
        if (bodyTemplateLabel != null)
            bodyTemplateLabel.setText("Body Template: " + resolveBodyTemplateName());
        populateTables();
        populateResponseChecks();
        snapshotOriginal();
        loadTemplateContents();
        updatePreview();
        // JSON preview removed
    }

    private String resolveBodyTemplateName() {
        if (originalTest == null)
            return "-";
        int id = originalTest.getBodyTemplateId();
        if (id <= 0)
            return "(none)";
        try {
            var svc = new com.qa.app.service.impl.BodyTemplateServiceImpl();
            var tmpl = svc.findBodyTemplateById(id);
            return tmpl != null ? tmpl.getName() : "#" + id;
        } catch (Exception e) {
            return "#" + id;
        }
    }

    private String resolveHeadersTemplateName() {
        if (originalTest == null)
            return "-";
        int id = originalTest.getHeadersTemplateId();
        if (id <= 0)
            return "(none)";
        try {
            var svc = new com.qa.app.service.impl.HeadersTemplateServiceImpl();
            var tmpl = svc.getHeadersTemplateById(id);
            return tmpl != null ? tmpl.getName() : "#" + id;
        } catch (Exception e) {
            return "#" + id;
        }
    }

    private void populateTables() {
        bodyVarData.clear();
        headerVarData.clear();
        Map<String, String> origBody = originalTest != null && originalTest.getBodyDynamicVariables() != null
                ? originalTest.getBodyDynamicVariables()
                : Collections.emptyMap();
        Map<String, String> origHeaders = originalTest != null && originalTest.getHeadersDynamicVariables() != null
                ? originalTest.getHeadersDynamicVariables()
                : Collections.emptyMap();
        initialSourceBodyNames = new HashSet<>(origBody.keySet());
        initialSourceHeaderNames = new HashSet<>(origHeaders.keySet());
        Map<String, String> bodyOverrides = step.getBodyVariableOverrides() == null ? Collections.emptyMap()
                : step.getBodyVariableOverrides();
        Map<String, String> headerOverrides = step.getHeadersVariableOverrides() == null ? Collections.emptyMap()
                : step.getHeadersVariableOverrides();
        Set<String> bodyKeys = new LinkedHashSet<>();
        bodyKeys.addAll(origBody.keySet());
        bodyKeys.addAll(bodyOverrides.keySet());
        for (String k : bodyKeys)
            bodyVarData.add(buildRow(k, origBody.get(k), bodyOverrides.get(k)));
        Set<String> headerKeys = new LinkedHashSet<>();
        headerKeys.addAll(origHeaders.keySet());
        headerKeys.addAll(headerOverrides.keySet());
        for (String k : headerKeys)
            headerVarData.add(buildRow(k, origHeaders.get(k), headerOverrides.get(k)));
    }

    private void populateResponseChecks() {
        responseCheckData.clear();
        // Clone from original test if step not overridden yet
        List<ResponseCheck> source;
        if (step.getResponseCheckOverrides() != null && !step.getResponseCheckOverrides().isEmpty()) {
            source = step.getResponseCheckOverrides();
        } else if (originalTest != null && originalTest.getResponseChecks() != null
                && !originalTest.getResponseChecks().isBlank()) {
            try {
                var om = new ObjectMapper();
                source = om.readValue(originalTest.getResponseChecks(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<ResponseCheck>>() {
                        });
            } catch (Exception e) {
                source = Collections.emptyList();
            }
        } else {
            source = Collections.emptyList();
        }
        for (ResponseCheck rc : source)
            responseCheckData.add(new ResponseCheck(rc));
    }

    private VariableOverrideRow buildRow(String name, String origVal, String overrideVal) {
        if (origVal == null)
            origVal = "";
        if (overrideVal == null)
            overrideVal = "";
        return new VariableOverrideRow(name, origVal, overrideVal);
    }

    private void snapshotOriginal() {
        originalBodySnapshot.clear();
        for (VariableOverrideRow r : bodyVarData)
            originalBodySnapshot.put(r.getName(), cloneRow(r));
        originalHeaderSnapshot.clear();
        for (VariableOverrideRow r : headerVarData)
            originalHeaderSnapshot.put(r.getName(), cloneRow(r));
    }

    private VariableOverrideRow cloneRow(VariableOverrideRow r) {
        VariableOverrideRow nr = new VariableOverrideRow(r.getName(), r.getOriginalValue(), r.getOverrideValue());
        nr.setNewlyAdded(r.isNewlyAdded());
        return nr;
    }

    @FXML private void handleApply() {
        handleSave();
    }

    @FXML private void handleRevert() {
        bodyVarData.setAll(originalBodySnapshot.values().stream().map(this::cloneRow).collect(Collectors.toList()));
        headerVarData.setAll(originalHeaderSnapshot.values().stream().map(this::cloneRow).collect(Collectors.toList()));
        refreshEverything();
    }

    @FXML private void handleSave() {
        Map<String, String> newBody = bodyVarData.stream().filter(this::filterRowForPersist)
                .collect(Collectors.toMap(VariableOverrideRow::getName, this::normalizePersistValue, (a, b) -> b,
                        LinkedHashMap::new));
        step.setBodyVariableOverrides(newBody.isEmpty() ? null : newBody);

        Map<String, String> newHeaders = headerVarData.stream().filter(this::filterRowForPersist)
                .collect(Collectors.toMap(VariableOverrideRow::getName, this::normalizePersistValue, (a, b) -> b,
                        LinkedHashMap::new));
        step.setHeadersVariableOverrides(newHeaders.isEmpty() ? null : newHeaders);

        // Persist response checks overrides
        step.setResponseCheckOverrides(responseCheckData.isEmpty() ? null : new ArrayList<>(responseCheckData));
        saved = true;
        if (dialogStage != null)
            dialogStage.close();
    }

    private String normalizePersistValue(VariableOverrideRow r) {
        if (r.getOverrideValue() == null)
            return "";
        return r.getOverrideValue();
    }

    private boolean filterRowForPersist(VariableOverrideRow r) {
        String ov = r.getOverrideValue();
        if (ov == null)
            ov = "";
        // Since allowEmptyCheckbox is removed, blank overrides are never persisted
        if (ov.isBlank())
            return false;
        if (r.isNewlyAdded())
            return true;
        return !Objects.equals(ov, r.getOriginalValue());
    }

    @FXML private void handleCancel() {
        saved = false;
        if (dialogStage != null)
            dialogStage.close();
    }

    public boolean isSaved() {
        return saved;
    }

    private void loadTemplateContents() {
        bodyTemplateContent = null;
        headersTemplateContent = null;
        if (originalTest != null) {
            int bId = originalTest.getBodyTemplateId();
            if (bId > 0) {
                try {
                    var svc = new com.qa.app.service.impl.BodyTemplateServiceImpl();
                    var tmpl = svc.findBodyTemplateById(bId);
                    if (tmpl != null)
                        bodyTemplateContent = tmpl.getContent();
                } catch (Exception ignored) {
                }
            } else {
                bodyTemplateContent = originalTest.getBody();
            }

            int hId = originalTest.getHeadersTemplateId();
            if (hId > 0) {
                try {
                    var svc = new com.qa.app.service.impl.HeadersTemplateServiceImpl();
                    var tmpl = svc.getHeadersTemplateById(hId);
                    if (tmpl != null)
                        headersTemplateContent = tmpl.getContent();
                } catch (Exception ignored) {
                }
            } else {
                headersTemplateContent = originalTest.getHeaders();
            }
        }
    }

    private void setupResponseChecksTable() {
        if (responseChecksTable == null)
            return;
        responseChecksTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        responseChecksTable.setItems(responseCheckData);
        responseChecksTable.setEditable(true);

        // Type & Operator Columns
        checkTypeColumn
                .setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getType()));
        checkTypeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(CheckType.values()));
        checkTypeColumn.setOnEditCommit(e -> {
            e.getRowValue().setType(e.getNewValue());
            responseChecksTable.refresh();
        });

        checkOperatorColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getOperator()));
        checkOperatorColumn.setCellFactory(ComboBoxTableCell.forTableColumn(Operator.values()));
        checkOperatorColumn.setOnEditCommit(e -> {
            e.getRowValue().setOperator(e.getNewValue());
        });

        // Expression column uses custom cell like GatlingTestViewModel.ExpressionCell
        checkExpressionColumn.setCellValueFactory(
                cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getExpression()));
        checkExpressionColumn.setCellFactory(col -> new ExpressionCell());
        checkExpressionColumn.setOnEditCommit(evt -> {
            if (evt.getRowValue().getType() != CheckType.DB) {
                evt.getRowValue().setExpression(evt.getNewValue());
            }
        });

        // Expect & SaveAs columns plain editable text
        checkExpectColumn
                .setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getExpect()));
        checkExpectColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        checkExpectColumn.setOnEditCommit(e -> {
            e.getRowValue().setExpect(e.getNewValue());
        });

        checkSaveAsColumn
                .setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getSaveAs()));
        checkSaveAsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        checkSaveAsColumn.setOnEditCommit(e -> {
            e.getRowValue().setSaveAs(e.getNewValue());
        });

        ensureDefaultStatusCheck();
    }

    // Handlers for response checks overrides
    @FXML private void handleAddResponseCheckOverride() {
        ResponseCheck rc = new ResponseCheck();
        responseCheckData.add(rc);
        responseChecksTable.getSelectionModel().clearSelection();
        responseChecksTable.getSelectionModel().select(rc);
        responseChecksTable.scrollTo(rc);
        // JSON preview removed
        ensureDefaultStatusCheck();
    }

    @FXML private void handleRemoveResponseCheckOverride() {
        var sel = responseChecksTable.getSelectionModel().getSelectedItems();
        if (sel != null && !sel.isEmpty()) {
            responseCheckData.removeAll(new ArrayList<>(sel));
            ensureDefaultStatusCheck();
        }
    }

    @FXML private void handleCopyResponseCheckOverride() {
        responseCheckClipboard.clear();
        for (ResponseCheck rc : responseChecksTable.getSelectionModel().getSelectedItems())
            responseCheckClipboard.add(new ResponseCheck(rc));
    }

    @FXML private void handlePasteResponseCheckOverride() {
        if (!responseCheckClipboard.isEmpty())
            for (ResponseCheck rc : responseCheckClipboard)
                responseCheckData.add(new ResponseCheck(rc));
    }

    private void showDbFieldsDialog(ResponseCheck rc) {
        if (rc == null)
            return;
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Edit DB Check");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBtn, cancelBtn);
        GridPane gp = new GridPane();
        gp.setVgap(10);
        gp.setHgap(10);
        TextField aliasField = new TextField(rc.getDbAlias());
        aliasField.setPrefWidth(300);
        TextArea sqlArea = new TextArea(rc.getDbSql());
        sqlArea.setPrefRowCount(10);
        sqlArea.setPrefWidth(300);
        TextField colField = new TextField(rc.getDbColumn());
        colField.setPrefWidth(300);
        gp.addRow(0, new Label("DB Alias"), aliasField);
        gp.addRow(1, new Label("SQL"), sqlArea);
        gp.addRow(2, new Label("Column"), colField);
        dlg.getDialogPane().setContent(gp);
        dlg.setResizable(true);
        // Add icon to dialog window
        Stage s = (Stage) dlg.getDialogPane().getScene().getWindow();
        s.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon/favicon.png")));
        try {
            dlg.setResultConverter(bt -> {
                if (bt == saveBtn) {
                    rc.setDbAlias(aliasField.getText());
                    rc.setDbSql(sqlArea.getText());
                    rc.setDbColumn(colField.getText());
                }
                return null;
            });
            dlg.showAndWait();
            responseChecksTable.refresh();
        } catch (Exception ignored) {
        }
    }

    private void ensureDefaultStatusCheck() {
        if (responseCheckData.stream().noneMatch(rc -> rc.getType() == CheckType.STATUS)) {
            responseCheckData.add(0, new ResponseCheck(CheckType.STATUS,"", Operator.IS, "200", null));
        }
    }

    // Custom cell for expression column (DB summary + edit button)
    private class ExpressionCell extends TableCell<ResponseCheck, String> {
        private TextField textField;
        private final HBox dbBox;
        private final Label dbSummary;
        private final Button editBtn;
        private final Pane spacer;

        ExpressionCell() {
            dbSummary = new Label();
            editBtn = new Button("Edit");
            editBtn.setOnAction(e -> {
                ResponseCheck rc = getTableView().getItems().get(getIndex());
                if (rc != null) {
                    showDbFieldsDialog(rc);
                }
            });
            spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            dbBox = new HBox(5, dbSummary, spacer, editBtn);
            dbBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        @Override public void startEdit() {
            if (isEmpty()) {
                return;
            }
            ResponseCheck rc = getTableView().getItems().get(getIndex());
            if (rc != null && rc.getType() == CheckType.DB)
                return; // no direct edit for DB

            super.startEdit();
            if (textField == null)
                createTextField();
            setText(null);
            setGraphic(textField);
            textField.setText(getItem());
            textField.selectAll();
            textField.requestFocus();
        }

        @Override public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            ResponseCheck rc = (ResponseCheck) getTableRow().getItem();
            if (isEditing()) {
                if (textField != null)
                    textField.setText(getItem());
                setText(null);
                setGraphic(textField);
            } else {
                setText(null);
                if (rc.getType() == CheckType.DB) {
                    String summary = String.format("Alias: %s, Col: %s",
                            rc.getDbAlias() != null ? rc.getDbAlias() : "N/A",
                            rc.getDbColumn() != null ? rc.getDbColumn() : "N/A");
                    dbSummary.setText(summary);
                    setGraphic(dbBox);
                } else {
                    setText(item);
                    setGraphic(null);
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getItem());
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((ov, o, nv) -> {
                if (!nv)
                    commitEdit(textField.getText());
            });
        }
    }

}
