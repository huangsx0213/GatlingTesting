package com.qa.app.ui.vm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.reports.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class TestReportViewModel implements Initializable {

    //<editor-fold desc="FXML Fields">
    @FXML
    private Label originTcidLabel;
    @FXML
    private Label suiteLabel;
    @FXML
    private TreeTableView<Object> requestsTreeTableView;
    @FXML
    private TreeTableColumn<Object, String> nameColumn;
    @FXML
    private TreeTableColumn<Object, String> statusColumn;
    @FXML
    private TreeTableColumn<Object, String> resultColumn;
    @FXML
    private TabPane detailsTabPane;
    @FXML
    private Label methodLabel;
    @FXML
    private Label urlLabel;
    @FXML
    private TextArea requestHeadersTextArea;
    @FXML
    private TextArea requestBodyTextArea;
    @FXML
    private Label responseStatusLabel;
    @FXML
    private Label latencyLabel;
    @FXML
    private Label sizeLabel;
    @FXML
    private TextArea responseHeadersTextArea;
    @FXML
    private TextArea responseBodyTextArea;
    @FXML
    private TableView<CheckReport> checksTableView;
    @FXML
    private TableColumn<CheckReport, String> checkTypeColumn;
    @FXML
    private TableColumn<CheckReport, String> checkExpressionColumn;
    @FXML
    private TableColumn<CheckReport, String> checkOperatorColumn;
    @FXML
    private TableColumn<CheckReport, String> checkExpectedColumn;
    @FXML
    private TableColumn<CheckReport, String> checkActualColumn;
    @FXML
    private TableColumn<CheckReport, String> checkResultColumn;
    @FXML
    private Label totalLabel;
    @FXML
    private Label passedLabel;
    @FXML
    private Label failedLabel;
    @FXML
    private javafx.scene.control.ComboBox<String> recentFilesCombo;
    //</editor-fold>

    private MainViewModel mainViewModel;

    private final javafx.beans.property.IntegerProperty totalCases = new javafx.beans.property.SimpleIntegerProperty(0);
    private final javafx.beans.property.IntegerProperty passedCases = new javafx.beans.property.SimpleIntegerProperty(0);
    private final javafx.beans.property.IntegerProperty failedCases = new javafx.beans.property.SimpleIntegerProperty(0);

    private java.nio.file.Path lastDir;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupSelectionListener();
        clearDetails();
        bindSummaryLabels();
        loadLastDirAndFiles();
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }
    
    @FXML
    private void handleLoadReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Report File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            try {
                String jsonContent = new String(java.nio.file.Files.readAllBytes(selectedFile.toPath()));
                loadReportData(jsonContent);
                // after load success
                java.nio.file.Path selectedPath = selectedFile.toPath();
                lastDir = selectedPath.getParent();
                java.util.prefs.Preferences.userNodeForPackage(TestReportViewModel.class).put("lastReportDir", lastDir.toString());
                refreshRecentFiles();
            } catch (IOException e) {
                e.printStackTrace();
                // Show error alert
            }
        }
    }

    public void loadReportData(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Determine if the JSON represents a single report or a list of reports
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(json);

            if (rootNode.isArray()) {
                // Aggregated batch file
                java.util.List<FunctionalTestReport> reports = mapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<FunctionalTestReport>>() {});
                populateReports(reports);
            } else {
                // Single report file (backward compatibility)
                FunctionalTestReport report = mapper.readValue(json, FunctionalTestReport.class);
                populateReport(report);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle error, maybe show an alert
        }
    }

    private void setupColumns() {
        nameColumn.setCellValueFactory(p -> {
            Object value = p.getValue().getValue();
            if (value instanceof FunctionalTestReport) {
                return new SimpleStringProperty(((FunctionalTestReport) value).getOriginTcid());
            }
            if (value instanceof ModeGroup) {
                return new SimpleStringProperty(((ModeGroup) value).getMode().toString());
            } else if (value instanceof CaseReport) {
                return new SimpleStringProperty(((CaseReport) value).getTcid());
            } else if (value instanceof RequestReport) {
                return new SimpleStringProperty(((RequestReport) value).getRequestName());
            }
            return new SimpleStringProperty("");
        });

        statusColumn.setCellValueFactory(p -> {
            Object value = p.getValue().getValue();
            if (value instanceof FunctionalTestReport) {
                return new SimpleStringProperty("");
            }
            if (value instanceof RequestReport) {
                return new SimpleStringProperty(((RequestReport) value).getStatus());
            } else if (value instanceof CaseReport cr) {
                if (cr.getItems().size() == 1) {
                    return new SimpleStringProperty(cr.getItems().get(0).getStatus());
                }
            }
            return new SimpleStringProperty("");
        });

        resultColumn.setCellValueFactory(p -> {
            Object value = p.getValue().getValue();
            if (value instanceof FunctionalTestReport) {
                boolean passed = ((FunctionalTestReport) value).getGroups().stream()
                        .flatMap(g -> g.getCases().stream())
                        .allMatch(CaseReport::isPassed);
                return new SimpleStringProperty(passed ? "PASS" : "FAIL");
            }
            if (value instanceof CaseReport) {
                return new SimpleStringProperty(((CaseReport) value).isPassed() ? "PASS" : "FAIL");
            } else if (value instanceof RequestReport) {
                return new SimpleStringProperty(((RequestReport) value).isPassed() ? "PASS" : "FAIL");
            }
            return new SimpleStringProperty("");
        });
        
        checkTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType().toString()));
        checkExpressionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExpression()));
        checkOperatorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOperator().toString()));
        checkExpectedColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExpect()));
        checkActualColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getActual()));
        checkResultColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().isPassed() ? "PASS" : "FAIL"));
    }

    private void setupSelectionListener() {
        requestsTreeTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection == null) {
                clearDetails();
                return;
            }

            Object val = newSelection.getValue();
            if (val instanceof RequestReport) {
                populateDetails((RequestReport) val);
            } else if (val instanceof CaseReport cr) {
                // Auto-show details if only one request exists
                if (cr.getItems() != null && cr.getItems().size() == 1) {
                    populateDetails(cr.getItems().get(0));
                } else {
                    clearDetails();
                }
            } else {
                clearDetails();
            }
        });
    }

    private void populateReport(FunctionalTestReport report) {
        computeSummary(report);

        TreeItem<Object> root = new TreeItem<>(report);
        root.setExpanded(true);
        requestsTreeTableView.setRoot(root);
        requestsTreeTableView.setShowRoot(false);

        for (ModeGroup group : report.getGroups()) {
            TreeItem<Object> groupItem = new TreeItem<>(group);
            groupItem.setExpanded(true);
            root.getChildren().add(groupItem);
            for (CaseReport caseReport : group.getCases()) {
                TreeItem<Object> caseItem = new TreeItem<>(caseReport);
                caseItem.setExpanded(true);
                groupItem.getChildren().add(caseItem);
                if (caseReport.getItems().size() > 1) {
                    for (RequestReport requestReport : caseReport.getItems()) {
                        caseItem.getChildren().add(new TreeItem<>(requestReport));
                    }
                }
            }
        }
    }

    private void populateDetails(RequestReport report) {
        RequestInfo requestInfo = report.getRequest();
        methodLabel.setText(requestInfo.getMethod());
        urlLabel.setText(requestInfo.getUrl());
        requestHeadersTextArea.setText(formatHeaders(requestInfo.getHeaders()));
        requestBodyTextArea.setText(requestInfo.getBody());

        ResponseInfo responseInfo = report.getResponse();
        responseStatusLabel.setText(String.valueOf(responseInfo.getStatus()));
        latencyLabel.setText(String.valueOf(responseInfo.getLatencyMs()));
        sizeLabel.setText(String.valueOf(responseInfo.getSizeBytes()));
        responseHeadersTextArea.setText(formatHeaders(responseInfo.getHeaders()));
        responseBodyTextArea.setText(responseInfo.getBodySample());

        checksTableView.setItems(FXCollections.observableArrayList(report.getChecks()));
    }

    private void clearDetails() {
        methodLabel.setText("-");
        urlLabel.setText("-");
        requestHeadersTextArea.clear();
        requestBodyTextArea.clear();
        responseStatusLabel.setText("-");
        latencyLabel.setText("-");
        sizeLabel.setText("-");
        responseHeadersTextArea.clear();
        responseBodyTextArea.clear();
        checksTableView.getItems().clear();
    }

    private String formatHeaders(Map<String, String> headers) {
        if (headers == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Populate UI with multiple FunctionalTestReport objects (aggregated batch file).
     */
    private void populateReports(java.util.List<FunctionalTestReport> reports) {
        computeSummary(reports);

        if (reports == null || reports.isEmpty()) {
            return;
        }

        // Indicate aggregated view
        originTcidLabel.setText("(Multiple)");
        suiteLabel.setText("-");

        TreeItem<Object> root = new TreeItem<>("ROOT");
        root.setExpanded(true);
        requestsTreeTableView.setRoot(root);
        requestsTreeTableView.setShowRoot(false);

        for (FunctionalTestReport report : reports) {
            TreeItem<Object> reportItem = new TreeItem<>(report);
            reportItem.setExpanded(true);
            root.getChildren().add(reportItem);

            for (ModeGroup group : report.getGroups()) {
                TreeItem<Object> groupItem = new TreeItem<>(group);
                groupItem.setExpanded(true);
                reportItem.getChildren().add(groupItem);

                for (CaseReport caseReport : group.getCases()) {
                    TreeItem<Object> caseItem = new TreeItem<>(caseReport);
                    caseItem.setExpanded(true);
                    groupItem.getChildren().add(caseItem);
                    if (caseReport.getItems().size() > 1) {
                        for (RequestReport requestReport : caseReport.getItems()) {
                            caseItem.getChildren().add(new TreeItem<>(requestReport));
                        }
                    }
                }
            }
        }
    }

    private void bindSummaryLabels() {
        totalLabel.textProperty().bind(totalCases.asString());
        passedLabel.textProperty().bind(passedCases.asString());
        failedLabel.textProperty().bind(failedCases.asString());
    }

    private void computeSummary(FunctionalTestReport report) {
        totalCases.set(1);
        boolean originPassed = report.getGroups().stream()
                .flatMap(g -> g.getCases().stream())
                .allMatch(CaseReport::isPassed);
        passedCases.set(originPassed ? 1 : 0);
        failedCases.set(originPassed ? 0 : 1);
    }

    private void computeSummary(java.util.List<FunctionalTestReport> reports) {
        int total = reports.size();
        int passed = 0;
        for (FunctionalTestReport r : reports) {
            boolean originPassed = r.getGroups().stream()
                    .flatMap(g -> g.getCases().stream())
                    .allMatch(CaseReport::isPassed);
            if (originPassed) passed++;
        }
        int failed = total - passed;
        totalCases.set(total);
        passedCases.set(passed);
        failedCases.set(failed);
    }

    private void loadLastDirAndFiles() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(TestReportViewModel.class);
        String dirStr = prefs.get("lastReportDir", null);
        if (dirStr != null) {
            lastDir = java.nio.file.Paths.get(dirStr);
            refreshRecentFiles();
        }
    }

    private void refreshRecentFiles() {
        recentFilesCombo.getItems().clear();
        if (lastDir != null && java.nio.file.Files.isDirectory(lastDir)) {
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(lastDir)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .map(p -> p.getFileName().toString())
                        .sorted()
                        .forEach(recentFilesCombo.getItems()::add);
            } catch (Exception ignored) {}
        }
    }

    @FXML private void handleRecentSelection() {
        String fileName = recentFilesCombo.getSelectionModel().getSelectedItem();
        if (fileName != null && lastDir != null) {
            java.nio.file.Path path = lastDir.resolve(fileName);
            try {
                String json = java.nio.file.Files.readString(path);
                loadReportData(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} 