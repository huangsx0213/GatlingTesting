package com.qa.app.ui.vm.gatling;

import com.qa.app.model.DynamicVariable;
import com.qa.app.model.Endpoint;
import com.qa.app.model.Environment;
import com.qa.app.service.api.IEndpointService;
import com.qa.app.service.api.IEnvironmentService;
import com.qa.app.service.impl.EndpointServiceImpl;
import com.qa.app.service.impl.EnvironmentServiceImpl;
import com.qa.app.service.util.VariableGenerator;
import com.qa.app.service.util.VariableUtil;
import com.qa.app.ui.util.DialogHelper;
import com.qa.app.ui.vm.MainViewModel;
import com.qa.app.util.AppConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.converter.DefaultStringConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler for Endpoint selection and URL dynamic variables
 */
public class GatlingTestEndpointHandler {

    private final ComboBox<String> endpointComboBox;
    private final TableView<DynamicVariable> urlDynamicVarsTable;
    private final TableColumn<DynamicVariable, String> urlDynamicKeyColumn;
    private final TableColumn<DynamicVariable, String> urlDynamicValueColumn;
    private final TableColumn<DynamicVariable, Void> urlDynamicActionColumn;
    private final TextArea generatedUrlArea;

    private final IEndpointService endpointService = new EndpointServiceImpl();
    private final IEnvironmentService environmentService = new EnvironmentServiceImpl();
    private final ObservableList<Endpoint> endpointList = FXCollections.observableArrayList();
    private final ObservableList<DynamicVariable> urlDynamicVariables = FXCollections.observableArrayList();
    private final Map<Integer, Endpoint> endpointIdMap = new HashMap<>();

    private MainViewModel mainViewModel;

    public GatlingTestEndpointHandler(
            ComboBox<String> endpointComboBox,
            TableView<DynamicVariable> urlDynamicVarsTable,
            TableColumn<DynamicVariable, String> urlDynamicKeyColumn,
            TableColumn<DynamicVariable, String> urlDynamicValueColumn,
            TableColumn<DynamicVariable, Void> urlDynamicActionColumn,
            TextArea generatedUrlArea) {
        this.endpointComboBox = endpointComboBox;
        this.urlDynamicVarsTable = urlDynamicVarsTable;
        this.urlDynamicKeyColumn = urlDynamicKeyColumn;
        this.urlDynamicValueColumn = urlDynamicValueColumn;
        this.urlDynamicActionColumn = urlDynamicActionColumn;
        this.generatedUrlArea = generatedUrlArea;
    }

    public ObservableList<Endpoint> getEndpointList() {
        return endpointList;
    }

    public ObservableList<DynamicVariable> getUrlDynamicVariables() {
        return urlDynamicVariables;
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void initialize() {
        setupEndpointComboBox();
        setupUrlDynamicVariablesTable();
    }

    private void setupEndpointComboBox() {
        endpointComboBox.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String name) {
                return name == null ? "" : name;
            }

            @Override
            public String fromString(String s) {
                return s;
            }
        });
        endpointComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(endpointComboBox.getPromptText());
                } else {
                    setText(item);
                }
            }
        });

        endpointComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            populateUrlDynamicVariables(newVal);
            updateGeneratedUrl();
        });
    }

    private void setupUrlDynamicVariablesTable() {
        if (urlDynamicVarsTable == null)
            return;
        urlDynamicKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        urlDynamicValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        urlDynamicVarsTable.setItems(urlDynamicVariables);
        urlDynamicVarsTable.setEditable(true);

        urlDynamicValueColumn.setCellFactory(col -> {
            List<String> suggestions = VariableGenerator.getInstance().getVariableDefinitions().stream()
                    .map(def -> def.get("format"))
                    .collect(Collectors.toList());
            ComboBoxTableCell<DynamicVariable, String> cell = new ComboBoxTableCell<>(
                    new DefaultStringConverter(),
                    FXCollections.observableArrayList(suggestions));
            cell.setComboBoxEditable(true);
            return cell;
        });
        urlDynamicValueColumn.setOnEditCommit(e -> {
            e.getRowValue().setValue(e.getNewValue());
            updateGeneratedUrl();
        });

        if (urlDynamicActionColumn != null) {
            urlDynamicActionColumn.setSortable(false);
            urlDynamicActionColumn.setCellFactory(col -> new TableCell<>() {
                private final Button editBtn = new Button("Edit");

                {
                    editBtn.setStyle(
                            "-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");
                    editBtn.setMaxWidth(Double.MAX_VALUE);
                    editBtn.setOnAction(evt -> {
                        DynamicVariable var = getTableView().getItems().get(getIndex());
                        String edited = DialogHelper.showLargeTextEditor(
                                "Edit Value - " + var.getKey(),
                                var.getValue(),
                                editBtn,
                                false);
                        if (edited != null) {
                            var.setValue(edited);
                            updateGeneratedUrl();
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

        urlDynamicVariables
                .addListener((javafx.collections.ListChangeListener<DynamicVariable>) c -> updateGeneratedUrl());
    }

    public void loadEndpoints() {
        endpointList.clear();
        endpointIdMap.clear();
        try {
            String envName = AppConfig.getProperty("current.env", "dev");
            Integer envId = null;
            for (Environment env : environmentService.findAllEnvironments()) {
                if (env.getName().equals(envName)) {
                    envId = env.getId();
                    break;
                }
            }
            if (envId != null) {
                for (Endpoint ep : endpointService.getAllEndpoints()) {
                    if (envId.equals(ep.getEnvironmentId())) {
                        endpointList.add(ep);
                        endpointIdMap.put(ep.getId(), ep);
                    }
                }
            }
        } catch (Exception e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load endpoints: " + e.getMessage(),
                        MainViewModel.StatusType.ERROR);
            }
        }
        endpointComboBox.setItems(FXCollections.observableArrayList(
                endpointList.stream().map(ep -> ep.getName() + " [ " + ep.getMethod() + " " + ep.getUrl() + " ]")
                        .toList()));
    }

    public void populateUrlDynamicVariables(String endpointDisplay) {
        if (endpointDisplay == null || endpointDisplay.isEmpty())
            return;

        String url = endpointDisplay.substring(endpointDisplay.indexOf("[ ") + 2, endpointDisplay.lastIndexOf(" ]"))
                .split(" ")[1];

        Set<String> vars = VariableUtil.extractDynamicVars(url);
        urlDynamicVariables.clear();
        for (String var : vars) {
            urlDynamicVariables.add(new DynamicVariable(var, ""));
        }
    }

    public String buildGeneratedUrl() {
        String endpointDisplay = endpointComboBox == null ? null : endpointComboBox.getValue();
        Endpoint ep = getEndpointByName(endpointDisplay);
        if (ep == null || ep.getUrl() == null)
            return "";

        String url = ep.getUrl();
        for (DynamicVariable dv : urlDynamicVariables) {
            String value = VariableGenerator.getInstance().resolveVariables(dv.getValue());
            if (value == null)
                value = "";
            url = url.replaceAll("@\\{" + java.util.regex.Pattern.quote(dv.getKey()) + "\\}",
                    java.util.regex.Matcher.quoteReplacement(value));
        }
        return url;
    }

    public void updateGeneratedUrl() {
        if (generatedUrlArea != null) {
            String u = buildGeneratedUrl();
            generatedUrlArea.setText(u == null ? "" : u);
        }
    }

    public Endpoint getEndpointByName(String displayString) {
        if (displayString == null || displayString.isBlank()) {
            return null;
        }
        String endpointName = displayString.split(" \\[")[0].trim();
        return endpointList.stream()
                .filter(e -> e.getName().equals(endpointName))
                .findFirst()
                .orElse(null);
    }

    public void selectEndpoint(String name) {
        if (name == null || name.isEmpty()) {
            endpointComboBox.setValue(null);
            return;
        }

        for (String item : endpointComboBox.getItems()) {
            if (item.startsWith(name + " [")) {
                endpointComboBox.setValue(item);
                return;
            }
        }
        endpointComboBox.setValue(null);
    }

    public String getSelectedEndpointName() {
        String display = endpointComboBox.getValue();
        if (display == null)
            return "";
        return display.split(" \\[")[0].trim();
    }

    public Map<String, String> getEndpointDynamicVariables() {
        Map<String, String> map = new HashMap<>();
        for (DynamicVariable dv : urlDynamicVariables) {
            map.put(dv.getKey(), dv.getValue());
        }
        return map;
    }

    public void setEndpointDynamicVariables(Map<String, String> vars) {
        urlDynamicVariables.clear();
        if (vars != null) {
            vars.forEach((k, v) -> urlDynamicVariables.add(new DynamicVariable(k, v)));
        }
    }

    public void clear() {
        endpointComboBox.getSelectionModel().clearSelection();
        endpointComboBox.setValue(null);
        urlDynamicVariables.clear();
        if (generatedUrlArea != null)
            generatedUrlArea.clear();
    }
}
