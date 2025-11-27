package com.qa.app.ui.vm.gatling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.*;
import com.qa.app.service.api.IDbConnectionService;
import com.qa.app.service.impl.DbConnectionServiceImpl;
import com.qa.app.ui.vm.MainViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handler for Response Checks UI and logic
 */
public class GatlingTestResponseCheckHandler {

    private final TableView<ResponseCheck> responseChecksTable;
    private final TableColumn<ResponseCheck, CheckType> checkTypeColumn;
    private final TableColumn<ResponseCheck, String> checkExpressionColumn;
    private final TableColumn<ResponseCheck, Operator> checkOperatorColumn;
    private final TableColumn<ResponseCheck, String> checkExpectColumn;
    private final TableColumn<ResponseCheck, String> checkSaveAsColumn;

    private final ObservableList<ResponseCheck> responseChecks = FXCollections.observableArrayList();
    private static final List<ResponseCheck> responseCheckClipboard = new ArrayList<>();
    private final IDbConnectionService dbConnectionService = new DbConnectionServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();

    private MainViewModel mainViewModel;

    public GatlingTestResponseCheckHandler(
            TableView<ResponseCheck> responseChecksTable,
            TableColumn<ResponseCheck, CheckType> checkTypeColumn,
            TableColumn<ResponseCheck, String> checkExpressionColumn,
            TableColumn<ResponseCheck, Operator> checkOperatorColumn,
            TableColumn<ResponseCheck, String> checkExpectColumn,
            TableColumn<ResponseCheck, String> checkSaveAsColumn) {
        this.responseChecksTable = responseChecksTable;
        this.checkTypeColumn = checkTypeColumn;
        this.checkExpressionColumn = checkExpressionColumn;
        this.checkOperatorColumn = checkOperatorColumn;
        this.checkExpectColumn = checkExpectColumn;
        this.checkSaveAsColumn = checkSaveAsColumn;
    }

    public ObservableList<ResponseCheck> getResponseChecks() {
        return responseChecks;
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void initialize() {
        setupTable();
    }

    private void setupTable() {
        responseChecksTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (responseChecksTable == null)
            return;
        responseChecksTable.setItems(responseChecks);
        responseChecksTable.setFixedCellSize(javafx.scene.layout.Region.USE_COMPUTED_SIZE);

        // Type column
        checkTypeColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getType()));
        checkTypeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(CheckType.values()));
        checkTypeColumn.setOnEditCommit(e -> {
            e.getRowValue().setType(e.getNewValue());
            responseChecksTable.refresh();
        });

        // Expression column
        checkExpressionColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getExpression()));
        checkExpressionColumn.setCellFactory(column -> new ExpressionCell());
        checkExpressionColumn.setOnEditCommit(event -> {
            ResponseCheck check = event.getRowValue();
            if (check.getType() != CheckType.DB) {
                check.setExpression(event.getNewValue());
            }
        });

        // Operator column
        checkOperatorColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getOperator()));
        checkOperatorColumn.setCellFactory(ComboBoxTableCell.forTableColumn(Operator.values()));
        checkOperatorColumn.setOnEditCommit(e -> e.getRowValue().setOperator(e.getNewValue()));

        // Expect column
        checkExpectColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getExpect()));
        checkExpectColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        checkExpectColumn.setOnEditCommit(event -> event.getRowValue().setExpect(event.getNewValue()));

        checkSaveAsColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSaveAs()));
        checkSaveAsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        checkSaveAsColumn.setOnEditCommit(event -> event.getRowValue().setSaveAs(event.getNewValue()));

        responseChecksTable.setEditable(true);

        ensureDefaultStatusCheck();
    }

    public void handleAddResponseCheck() {
        ResponseCheck newCheck = new ResponseCheck();
        responseChecks.add(newCheck);
        responseChecksTable.getSelectionModel().clearSelection();
        responseChecksTable.getSelectionModel().select(newCheck);
        responseChecksTable.scrollTo(newCheck);
    }

    public void handleRemoveResponseCheck() {
        List<ResponseCheck> selected = new ArrayList<>(responseChecksTable.getSelectionModel().getSelectedItems());
        if (!selected.isEmpty()) {
            responseChecks.removeAll(selected);
        }
        ensureDefaultStatusCheck();
    }

    public void handleCopyResponseCheck() {
        List<ResponseCheck> selected = responseChecksTable.getSelectionModel().getSelectedItems();
        if (!selected.isEmpty()) {
            responseCheckClipboard.clear();
            for (ResponseCheck rc : selected) {
                responseCheckClipboard.add(new ResponseCheck(rc));
            }
            if (mainViewModel != null) {
                mainViewModel.updateStatus(selected.size() + " check(s) copied to clipboard.",
                        MainViewModel.StatusType.INFO);
            }
        }
    }

    public void handlePasteResponseCheck() {
        if (!responseCheckClipboard.isEmpty()) {
            for (ResponseCheck rc : responseCheckClipboard) {
                responseChecks.add(new ResponseCheck(rc));
            }
            if (mainViewModel != null) {
                mainViewModel.updateStatus(responseCheckClipboard.size() + " check(s) pasted.",
                        MainViewModel.StatusType.INFO);
            }
        }
    }

    private void ensureDefaultStatusCheck() {
        if (responseChecks.stream().noneMatch(rc -> rc.getType() == CheckType.STATUS)) {
            String defaultStatus = "200";
            responseChecks.add(0, new ResponseCheck(CheckType.STATUS, "", Operator.IS, defaultStatus, null));
        }
    }

    public void setResponseChecks(String json) {
        responseChecks.clear();
        if (json != null && !json.isEmpty()) {
            try {
                List<ResponseCheck> list = mapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<List<ResponseCheck>>() {
                        });
                responseChecks.addAll(list);
            } catch (Exception ignored) {
            }
        }
        ensureDefaultStatusCheck();
    }

    public String getResponseChecksJson() {
        try {
            return mapper.writeValueAsString(responseChecks);
        } catch (Exception e) {
            return null;
        }
    }

    public void clear() {
        responseChecks.clear();
        ensureDefaultStatusCheck();
        if (responseChecksTable != null) {
            responseChecksTable.refresh();
        }
    }

    private void showDbCheckEditDialog(ResponseCheck check) {
        Dialog<ResponseCheck> dialog = new Dialog<>();
        dialog.setTitle("Edit DB Check");
        dialog.setHeaderText("Configure the database query and result validation.");

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon/favicon.png")));

        dialog.initOwner(responseChecksTable.getScene().getWindow());
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        ComboBox<String> dbAliasComboBox = new ComboBox<>();
        try {
            dbAliasComboBox.setItems(FXCollections.observableArrayList(dbConnectionService.getAllAliases()));
        } catch (Exception e) {
            if (mainViewModel != null) {
                mainViewModel.updateStatus("Failed to load DB aliases: " + e.getMessage(),
                        MainViewModel.StatusType.ERROR);
            }
        }
        dbAliasComboBox.setValue(check.getDbAlias());
        dbAliasComboBox.setPrefWidth(350);

        TextArea dbSqlArea = new TextArea(check.getDbSql());
        dbSqlArea.setPromptText("SQL Query");
        dbSqlArea.setWrapText(true);
        dbSqlArea.setPrefWidth(350);
        dbSqlArea.setPrefHeight(150);

        TextField dbColumnField = new TextField(check.getDbColumn());
        dbColumnField.setPromptText("Result Column to fetch");
        dbColumnField.setPrefWidth(350);

        grid.add(new Label("DB Connection Alias:"), 0, 0);
        grid.add(dbAliasComboBox, 1, 0);
        grid.add(new Label("SQL Query:"), 0, 1);
        grid.add(dbSqlArea, 1, 1);
        grid.add(new Label("Check Column:"), 0, 2);
        grid.add(dbColumnField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResizable(true);

        Platform.runLater(dbAliasComboBox::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                check.setDbAlias(dbAliasComboBox.getValue());
                check.setDbSql(dbSqlArea.getText());
                check.setDbColumn(dbColumnField.getText());
                return check;
            }
            return null;
        });

        Optional<ResponseCheck> result = dialog.showAndWait();
        result.ifPresent(updatedCheck -> responseChecksTable.refresh());
    }

    /**
     * Custom cell for Expression column
     */
    private class ExpressionCell extends TableCell<ResponseCheck, String> {
        private TextField textField;
        private final HBox dbCellBox;
        private final Label dbSummaryLabel;
        private final Button editDbButton;
        private final Tooltip dbSqlTooltip;
        private final Pane spacer;

        public ExpressionCell() {
            super();
            dbSummaryLabel = new Label();
            editDbButton = new Button("Edit");
            dbSqlTooltip = new Tooltip();
            spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            dbCellBox = new HBox(5, dbSummaryLabel, spacer, editDbButton);
            dbCellBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            editDbButton.setOnAction(event -> {
                ResponseCheck check = getTableView().getItems().get(getIndex());
                if (check != null) {
                    showDbCheckEditDialog(check);
                }
            });
        }

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                ResponseCheck check = getTableView().getItems().get(getIndex());
                if (check != null && check.getType() == CheckType.DB) {
                    return;
                }
            }

            super.startEdit();
            if (textField == null) {
                createTextField();
            }
            setText(null);
            setGraphic(textField);
            textField.setText(getItem());
            textField.selectAll();
            textField.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getItem());
                }
                setText(null);
                setGraphic(textField);
            } else {
                ResponseCheck check = (ResponseCheck) getTableRow().getItem();
                if (check.getType() == CheckType.DB) {
                    String summary = String.format("Alias: %s, Col: %s",
                            check.getDbAlias() != null ? check.getDbAlias() : "N/A",
                            check.getDbColumn() != null ? check.getDbColumn() : "N/A");
                    dbSummaryLabel.setText(summary);
                    String sql = check.getDbSql();
                    if (sql != null && !sql.isEmpty()) {
                        dbSqlTooltip.setText(sql);
                        Tooltip.install(dbSummaryLabel, dbSqlTooltip);
                    } else {
                        Tooltip.uninstall(dbSummaryLabel, dbSqlTooltip);
                    }
                    setText(null);
                    setGraphic(dbCellBox);
                } else {
                    setText(item);
                    setGraphic(null);
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getItem());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            textField.setOnAction(evt -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    commitEdit(textField.getText());
                }
            });
        }
    }
}
