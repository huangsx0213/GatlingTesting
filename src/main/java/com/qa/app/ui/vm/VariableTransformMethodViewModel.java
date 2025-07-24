package com.qa.app.ui.vm;

import com.qa.app.model.VariableTransformMethod;
import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IVariableTransformMethodService;
import com.qa.app.service.impl.VariableTransformMethodServiceImpl;
import com.qa.app.service.util.BuiltInVariableConverter;
import com.qa.app.ui.util.ClickableTooltipTableCell;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Callback;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * JavaFX controller for managing variable transform methods.
 */
public class VariableTransformMethodViewModel implements Initializable {

    // --- FXML Bindings for Custom Methods ---
    @FXML private TableView<VariableTransformMethod> methodsTableView;
    @FXML private TableColumn<VariableTransformMethod, String> nameColumn;
    @FXML private TableColumn<VariableTransformMethod, String> descColumn;
    @FXML private TableColumn<VariableTransformMethod, Boolean> enabledColumn;

    @FXML private TextField nameField;
    @FXML private TextArea scriptArea;
    @FXML private TextField descriptionField;
    @FXML private TextField paramSpecField;
    @FXML private TextField sampleUsageField;
    @FXML private CheckBox enabledCheckBox;

    // test conversion
    @FXML private TextField testInputField;
    @FXML private TextField testParamsField;
    @FXML private Button testRunButton;
    @FXML private Label testResultLabel;

    // --- FXML Bindings for Built-in Converters ---
    @FXML private TableView<BuiltInVariableConverter> builtinTableView;
    @FXML private TableColumn<BuiltInVariableConverter, String> biNameCol;
    @FXML private TableColumn<BuiltInVariableConverter, String> biDescCol;
    @FXML private TableColumn<BuiltInVariableConverter, String> biParamCol;
    @FXML private TableColumn<BuiltInVariableConverter, String> biSampleCol;

    @FXML private TextField biExpressionField;
    @FXML private Label biTestResultLabel;

    // --- ViewModel State ---
    private final ObservableList<VariableTransformMethod> methods = FXCollections.observableArrayList();
    private final IVariableTransformMethodService service = new VariableTransformMethodServiceImpl();
    private final ObservableList<BuiltInVariableConverter> builtinMethods = FXCollections.observableArrayList();

    private VariableTransformMethod editing;
    private MainViewModel mainViewModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        methodsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        descColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        enabledColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleBooleanProperty(c.getValue().isEnabled()));
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));
        loadMethods();

        methodsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                populateForm(newV);
            }
        });

        // built-in converters table setup
        builtinMethods.setAll(BuiltInVariableConverter.values());
        if (builtinTableView != null) { // FXML tab might not be visible at startup
            builtinTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            builtinTableView.setItems(builtinMethods);
            biNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
            biDescCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
            biParamCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getParamSpec()));
            biSampleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSampleUsage()));

            // Custom cell factory for tooltips on long text
            Callback<TableColumn<BuiltInVariableConverter, String>, TableCell<BuiltInVariableConverter, String>> cellFactory =
                    param -> new ClickableTooltipTableCell<>();

            biDescCol.setCellFactory(cellFactory);
            biSampleCol.setCellFactory(cellFactory);
            biParamCol.setCellFactory(cellFactory);

            // Add context menu for copying sample usage
            ContextMenu contextMenu = new ContextMenu();
            MenuItem copySampleItem = new MenuItem("Copy Sample Usage");
            copySampleItem.setOnAction(event -> {
                BuiltInVariableConverter selected = builtinTableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    final Clipboard clipboard = Clipboard.getSystemClipboard();
                    final ClipboardContent content = new ClipboardContent();
                    content.putString(selected.getSampleUsage());
                    clipboard.setContent(content);
                    updateStatus("Sample usage copied to clipboard", MainViewModel.StatusType.SUCCESS);
                }
            });
            contextMenu.getItems().add(copySampleItem);
            builtinTableView.setContextMenu(contextMenu);
        }
    }

    public void refresh() {
        loadMethods();
    }

    private void loadMethods() {
        try {
            methods.setAll(service.findAll());
            methodsTableView.setItems(methods);
            if (!methods.isEmpty()) {
                methodsTableView.getSelectionModel().selectFirst();
            } else {
                clearForm();
            }
        } catch (ServiceException e) {
            updateStatus("Failed to load methods: " + e.getMessage(), MainViewModel.StatusType.ERROR);
        }
    }

    private void populateForm(VariableTransformMethod m) {
        editing = m;
        nameField.setText(m.getName());
        descriptionField.setText(m.getDescription());
        scriptArea.setText(m.getScript());
        paramSpecField.setText(m.getParamSpec());
        sampleUsageField.setText(m.getSampleUsage());
        enabledCheckBox.setSelected(m.isEnabled());
    }

    @FXML
    private void handleAdd() {
        VariableTransformMethod m = new VariableTransformMethod();
        if (!fillModelFromForm(m, true)) return;
        try {
            service.create(m);
            loadMethods();
            updateStatus("Added method '" + m.getName() + "'", MainViewModel.StatusType.SUCCESS);
        } catch (ServiceException e) {
            updateStatus("Add failed: " + e.getMessage(), MainViewModel.StatusType.ERROR);
        }
    }

    @FXML
    private void handleSave() {
        if (editing == null) {
            updateStatus("No selection to save", MainViewModel.StatusType.ERROR);
            return;
        }
        if (!fillModelFromForm(editing, false)) return;
        try {
            service.update(editing);
            loadMethods();
            updateStatus("Updated method", MainViewModel.StatusType.SUCCESS);
        } catch (ServiceException e) {
            updateStatus("Update failed: " + e.getMessage(), MainViewModel.StatusType.ERROR);
        }
    }

    @FXML
    private void handleDelete() {
        if (editing == null) {
            updateStatus("No selection to delete", MainViewModel.StatusType.ERROR);
            return;
        }
        try {
            service.delete(editing.getId());
            loadMethods();
            updateStatus("Deleted", MainViewModel.StatusType.SUCCESS);
        } catch (ServiceException e) {
            updateStatus("Delete failed: " + e.getMessage(), MainViewModel.StatusType.ERROR);
        }
    }

    @FXML
    private void handleClear() {
        editing = null;
        clearForm();
    }

    private void clearForm() {
        nameField.clear();
        descriptionField.clear();
        scriptArea.clear();
        paramSpecField.clear();
        sampleUsageField.clear();
        enabledCheckBox.setSelected(true);
    }

    private boolean fillModelFromForm(VariableTransformMethod m, boolean isNew) {
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            updateStatus("Name required", MainViewModel.StatusType.ERROR);
            return false;
        }
        m.setName(name.trim());
        m.setDescription(descriptionField.getText());
        m.setScript(scriptArea.getText());
        m.setParamSpec(paramSpecField.getText());
        m.setSampleUsage(sampleUsageField.getText());
        m.setEnabled(enabledCheckBox.isSelected());
        return true;
    }

    @FXML
    private void handleTestRun() {
        if (editing == null) {
            updateStatus("Select or save method first", MainViewModel.StatusType.ERROR);
            return;
        }
        String input = testInputField.getText();
        List<String> params = Arrays.stream(testParamsField.getText().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        try {
            Object result = service.apply(editing.getName(), input, params);
            testResultLabel.setText(String.valueOf(result));
        } catch (ServiceException e) {
            testResultLabel.setText("ERROR: " + e.getMessage());
        }
    }

    @FXML
    private void handleBuiltInTestRun() {
        if (biExpressionField == null) return;
        String expr = biExpressionField.getText();
        if (expr == null || expr.isBlank()) {
            updateStatus("Enter an expression like TRIM(' abc ')", MainViewModel.StatusType.ERROR);
            return;
        }

        // parse expression NAME(arg1, arg2 ...)
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*\\((.*)\\)\\s*$");
        java.util.regex.Matcher m = p.matcher(expr);
        if (!m.matches()) {
            biTestResultLabel.setText("ERROR: Invalid expression format");
            return;
        }
        String converterName = m.group(1);
        String argsPart = m.group(2).trim();

        BuiltInVariableConverter converter = BuiltInVariableConverter.from(converterName);
        if (converter == null) {
            biTestResultLabel.setText("ERROR: Unknown converter '" + converterName + "'");
            return;
        }

        // highlight corresponding row in table if present
        if (builtinTableView != null) {
            builtinTableView.getSelectionModel().select(converter);
        }

        List<String> allArgs;
        if (argsPart.isEmpty()) {
            allArgs = Collections.emptyList();
        } else {
            allArgs = Arrays.stream(argsPart.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        Object value = null;
        if (!allArgs.isEmpty()) {
            String firstArg = allArgs.get(0);
            if (firstArg.matches("^[0-9]+$")) {
                try {
                    value = Long.parseLong(firstArg);
                } catch (NumberFormatException e) {
                    value = stripQuotes(firstArg); // Fallback for very large numbers
                }
            } else {
                value = stripQuotes(firstArg);
            }
        }
        List<String> params = allArgs.size() > 1
                ? allArgs.subList(1, allArgs.size()).stream().map(this::stripQuotes).collect(Collectors.toList())
                : Collections.emptyList();

        try {
            Object result = converter.convert(value, params);
            biTestResultLabel.setText(String.valueOf(result));
        } catch (Exception ex) {
            biTestResultLabel.setText("ERROR: " + ex.getMessage());
        }
    }

    private void updateStatus(String msg, MainViewModel.StatusType type) {
        if (mainViewModel != null) {
            mainViewModel.updateStatus(msg, type);
        }
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
} 