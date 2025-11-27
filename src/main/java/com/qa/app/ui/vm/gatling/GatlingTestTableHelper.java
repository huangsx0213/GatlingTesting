package com.qa.app.ui.vm.gatling;

import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingTest;
import com.qa.app.ui.util.ClickableTooltipTableCell;
import com.qa.app.ui.vm.MainViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Handler for the test TableView (testTable)
 */
public class GatlingTestTableHelper {

    private final TableView<GatlingTest> testTable;
    private final TableColumn<GatlingTest, Boolean> isEnabledColumn;
    private final TableColumn<GatlingTest, String> suiteColumn;
    private final TableColumn<GatlingTest, String> testTcidColumn;
    private final TableColumn<GatlingTest, String> descriptionsColumn;
    private final TableColumn<GatlingTest, String> endpointColumn;
    private final TableColumn<GatlingTest, String> tagsColumn;
    private final TableColumn<GatlingTest, Integer> waitTimeColumn;
    private final TableColumn<GatlingTest, String> headersTemplateNameColumn;
    private final TableColumn<GatlingTest, String> bodyTemplateNameColumn;

    private final ObservableList<GatlingTest> testList = FXCollections.observableArrayList();
    private final Map<GatlingTest, BooleanProperty> selectionMap = new HashMap<>();
    private final CheckBox selectAllCheckBox = new CheckBox();
    private boolean isUpdatingSelection = false;

    private ObservableList<Endpoint> endpointList;
    private Map<Integer, String> bodyTemplateIdNameMap;
    private Map<Integer, String> headersTemplateIdNameMap;
    private Consumer<GatlingTest> onTestSelected;

    private MainViewModel mainViewModel;

    public GatlingTestTableHelper(
            TableView<GatlingTest> testTable,
            TableColumn<GatlingTest, Boolean> isEnabledColumn,
            TableColumn<GatlingTest, String> suiteColumn,
            TableColumn<GatlingTest, String> testTcidColumn,
            TableColumn<GatlingTest, String> descriptionsColumn,
            TableColumn<GatlingTest, String> endpointColumn,
            TableColumn<GatlingTest, String> tagsColumn,
            TableColumn<GatlingTest, Integer> waitTimeColumn,
            TableColumn<GatlingTest, String> headersTemplateNameColumn,
            TableColumn<GatlingTest, String> bodyTemplateNameColumn,
            CheckBox selectAllCheckBox) { // Added CheckBox to constructor if needed, but I'll ignore it if I create my
                                          // own
        this.testTable = testTable;
        this.isEnabledColumn = isEnabledColumn;
        this.suiteColumn = suiteColumn;
        this.testTcidColumn = testTcidColumn;
        this.descriptionsColumn = descriptionsColumn;
        this.endpointColumn = endpointColumn;
        this.tagsColumn = tagsColumn;
        this.waitTimeColumn = waitTimeColumn;
        this.headersTemplateNameColumn = headersTemplateNameColumn;
        this.bodyTemplateNameColumn = bodyTemplateNameColumn;
        // If the VM passes a checkbox, we could use it, but here I'm using my own
        // internal one for the column header.
        // The VM passed one, so let's sync or just use the internal one.
        // For simplicity, I'll stick to the internal one for the column header logic.
    }

    public ObservableList<GatlingTest> getTestList() {
        return testList;
    }

    public Map<GatlingTest, BooleanProperty> getSelectionMap() {
        return selectionMap;
    }

    public CheckBox getSelectAllCheckBox() {
        return selectAllCheckBox;
    }

    public void setDependencies(ObservableList<Endpoint> endpointList,
            Map<Integer, String> bodyTemplateIdNameMap,
            Map<Integer, String> headersTemplateIdNameMap) {
        this.endpointList = endpointList;
        this.bodyTemplateIdNameMap = bodyTemplateIdNameMap;
        this.headersTemplateIdNameMap = headersTemplateIdNameMap;
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    public void setOnTestSelected(Consumer<GatlingTest> onTestSelected) {
        this.onTestSelected = onTestSelected;
    }

    public void initialize() {
        setupTestTableColumns();
        testTable.setItems(testList);
        testTable.setEditable(true);
        testTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setupTestTableListeners();
    }

    private void setupTestTableColumns() {
        // Selection column
        TableColumn<GatlingTest, Boolean> selectColumn = new TableColumn<>();
        selectColumn.setGraphic(selectAllCheckBox);
        selectColumn.setPrefWidth(40);
        selectColumn.setSortable(false);
        selectColumn.setCellValueFactory(cellData -> {
            GatlingTest test = cellData.getValue();
            return selectionMap.computeIfAbsent(test, k -> new SimpleBooleanProperty(false));
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        testTable.getColumns().add(0, selectColumn);

        // Other columns
        isEnabledColumn.setCellValueFactory(new PropertyValueFactory<>("isEnabled"));
        isEnabledColumn.setCellFactory(column -> {
            TableCell<GatlingTest, Boolean> cell = new TableCell<>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item ? "Y" : "N");
                    }
                }
            };
            cell.setStyle("-fx-alignment: CENTER;");
            return cell;
        });
        suiteColumn.setCellValueFactory(new PropertyValueFactory<>("suite"));
        testTcidColumn.setCellValueFactory(new PropertyValueFactory<>("tcid"));
        descriptionsColumn.setCellValueFactory(new PropertyValueFactory<>("descriptions"));

        // PopOver for descriptions
        descriptionsColumn.setCellFactory(column -> new TableCell<GatlingTest, String>() {
            private final org.controlsfx.control.PopOver pop = new org.controlsfx.control.PopOver();
            private final javafx.scene.web.WebView web = new javafx.scene.web.WebView();
            private final javafx.animation.PauseTransition showDelay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(200));
            private final javafx.animation.PauseTransition hideDelay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(200));

            {
                web.setPrefSize(300, 200);
                pop.setContentNode(web);
                pop.setDetachable(false);
                pop.setArrowLocation(org.controlsfx.control.PopOver.ArrowLocation.RIGHT_TOP);

                showDelay.setOnFinished(e -> {
                    if (getItem() != null) {
                        web.getEngine().loadContent(getItem());
                        pop.show(this);
                    }
                });
                hideDelay.setOnFinished(e -> pop.hide());

                this.setOnMouseEntered(e -> {
                    hideDelay.stop();
                    showDelay.playFromStart();
                });
                this.setOnMouseExited(e -> {
                    showDelay.stop();
                    hideDelay.playFromStart();
                });
                pop.setOnHidden(e -> hideDelay.stop());
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    pop.hide();
                } else {
                    setText(item.replaceAll("<[^>]*>", ""));
                }
            }
        });

        endpointColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getEndpointName();
            Endpoint ep = null;
            if (endpointList != null) {
                for (Endpoint e : endpointList) {
                    if (e.getName().equals(name)) {
                        ep = e;
                        break;
                    }
                }
            }
            String display = "";
            if (ep != null) {
                display = ep.getName() + " [ " + ep.getMethod() + " " + ep.getUrl() + " ]";
            }
            return new javafx.beans.property.SimpleStringProperty(display);
        });
        endpointColumn.setCellFactory(param -> new ClickableTooltipTableCell<>());

        tagsColumn.setCellValueFactory(new PropertyValueFactory<>("tags"));
        waitTimeColumn.setCellValueFactory(new PropertyValueFactory<>("waitTime"));

        headersTemplateNameColumn.setCellValueFactory(cellData -> {
            int id = cellData.getValue().getHeadersTemplateId();
            String name = headersTemplateIdNameMap != null ? headersTemplateIdNameMap.getOrDefault(id, "") : "";
            return new javafx.beans.property.SimpleStringProperty(name);
        });
        bodyTemplateNameColumn.setCellValueFactory(cellData -> {
            int id = cellData.getValue().getBodyTemplateId();
            String name = bodyTemplateIdNameMap != null ? bodyTemplateIdNameMap.getOrDefault(id, "") : "";
            return new javafx.beans.property.SimpleStringProperty(name);
        });
    }

    private void setupTestTableListeners() {
        testTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (onTestSelected != null) {
                        onTestSelected.accept(newValue);
                    }
                });

        testTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<GatlingTest>) c -> {
            if (isUpdatingSelection)
                return;
            isUpdatingSelection = true;
            syncSelectionProperties();
            updateSelectAllCheckBoxState();
            isUpdatingSelection = false;
        });

        selectAllCheckBox.setOnAction(e -> {
            if (selectAllCheckBox.isSelected()) {
                testTable.getSelectionModel().selectAll();
            } else {
                testTable.getSelectionModel().clearSelection();
            }
            testTable.refresh();
        });
    }

    private void syncSelectionProperties() {
        isUpdatingSelection = true;
        for (GatlingTest t : testList) {
            BooleanProperty prop = selectionMap.get(t);
            if (prop != null) {
                boolean shouldBeSelected = testTable.getSelectionModel().getSelectedItems().contains(t);
                if (prop.get() != shouldBeSelected) {
                    prop.set(shouldBeSelected);
                }
            }
        }
        isUpdatingSelection = false;
    }

    public void updateSelectAllCheckBoxState() {
        int selectedCount = testTable.getSelectionModel().getSelectedItems().size();
        if (selectedCount == 0) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
        } else if (selectedCount == testList.size() && !testList.isEmpty()) {
            selectAllCheckBox.setSelected(true);
            selectAllCheckBox.setIndeterminate(false);
        } else {
            selectAllCheckBox.setIndeterminate(true);
        }
    }

    public void setTestList(List<GatlingTest> tests) {
        selectionMap.clear();
        testList.setAll(tests);
        for (GatlingTest test : tests) {
            BooleanProperty selected = new SimpleBooleanProperty(false);
            selected.addListener((obs, wasSelected, isSelected) -> {
                if (isUpdatingSelection)
                    return;
                if (isSelected) {
                    testTable.getSelectionModel().select(test);
                } else {
                    testTable.getSelectionModel().clearSelection(testList.indexOf(test));
                }
            });
            selectionMap.put(test, selected);
        }
        updateSelectAllCheckBoxState();
    }

    public TableView.TableViewSelectionModel<GatlingTest> getSelectionModel() {
        return testTable.getSelectionModel();
    }

    public GatlingTest getSelectedTest() {
        return testTable.getSelectionModel().getSelectedItem();
    }

    public ObservableList<GatlingTest> getSelectedTests() {
        return testTable.getSelectionModel().getSelectedItems();
    }
}
