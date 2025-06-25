package com.qa.app.ui.view;

import com.qa.app.model.reports.*;
import com.qa.app.ui.vm.FunctionalTestReportViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FunctionalTestReportStage extends Stage {

    private final FunctionalTestReportViewModel viewModel;

    public FunctionalTestReportStage(String reportPath) {
        this.viewModel = new FunctionalTestReportViewModel(reportPath);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/qa/app/ui/view/functional_test_report_view.fxml"));
            AnchorPane root = loader.load();

            // Bind top-level labels
            ((Label) lookup(root, "#originTcidLabel")).textProperty().bind(viewModel.originTcidProperty());
            ((Label) lookup(root, "#suiteLabel")).textProperty().bind(viewModel.suiteProperty());

            // Setup TreeTableView
            TreeTableView<Object> treeTableView = (TreeTableView<Object>) lookup(root, "#requestsTreeTableView");
            setupTreeTableView(treeTableView);

            // Setup detail panes
            bindDetailControls(root);

            // Add selection listener
            treeTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    viewModel.updateDetails(newSelection.getValue());
                }
            });

            Scene scene = new Scene(root);
            this.setTitle("Functional Test Report: " + viewModel.originTcidProperty().get());
            this.setScene(scene);
            this.initModality(Modality.APPLICATION_MODAL);
            this.show();

        } catch (IOException e) {
            e.printStackTrace();
            // Show an error alert
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not load the report view FXML.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void setupTreeTableView(TreeTableView<Object> treeTableView) {
        treeTableView.setRoot(viewModel.getRootNode());
        treeTableView.setShowRoot(false);

        TreeTableColumn<Object, String> nameColumn = (TreeTableColumn<Object, String>) treeTableView.getColumns().get(0);
        nameColumn.setCellValueFactory(param -> {
            Object value = param.getValue().getValue();
            if (value instanceof ModeGroup) {
                return new SimpleStringProperty(((ModeGroup) value).getMode().name());
            } else if (value instanceof CaseReport) {
                return new SimpleStringProperty(((CaseReport) value).getTcid());
            } else if (value instanceof RequestReport) {
                return new SimpleStringProperty(((RequestReport) value).getRequestName());
            }
            return new SimpleStringProperty("");
        });

        TreeTableColumn<Object, Integer> statusColumn = (TreeTableColumn<Object, Integer>) treeTableView.getColumns().get(1);
        statusColumn.setCellValueFactory(param -> {
            if (param.getValue().getValue() instanceof RequestReport) {
                ResponseInfo res = ((RequestReport) param.getValue().getValue()).getResponse();
                return new javafx.beans.property.SimpleObjectProperty(res.getStatus());
            }
            return null;
        });

        TreeTableColumn<Object, Text> resultColumn = (TreeTableColumn<Object, Text>) treeTableView.getColumns().get(2);
        resultColumn.setCellValueFactory(param -> {
            Object value = param.getValue().getValue();
            Boolean passed = null;
            if (value instanceof CaseReport) {
                passed = ((CaseReport) value).isPassed();
            } else if (value instanceof RequestReport) {
                RequestReport rr = (RequestReport) value;
                passed = rr.getChecks().stream().allMatch(CheckReport::isPassed);
            }

            if (passed != null) {
                return new javafx.beans.property.SimpleObjectProperty<>(FunctionalTestReportViewModel.getStyledTextForResult(passed));
            }
            return null;
        });
    }

    private void bindDetailControls(AnchorPane root) {
        // Request Tab
        ((Label) lookup(root, "#methodLabel")).textProperty().bind(viewModel.methodProperty());
        ((Label) lookup(root, "#urlLabel")).textProperty().bind(viewModel.urlProperty());
        ((TextArea) lookup(root, "#requestHeadersTextArea")).textProperty().bind(viewModel.requestHeadersProperty());
        ((TextArea) lookup(root, "#requestBodyTextArea")).textProperty().bind(viewModel.requestBodyProperty());

        // Response Tab
        ((Label) lookup(root, "#responseStatusLabel")).textProperty().bind(viewModel.responseStatusProperty());
        ((Label) lookup(root, "#latencyLabel")).textProperty().bind(viewModel.latencyProperty());
        ((Label) lookup(root, "#sizeLabel")).textProperty().bind(viewModel.sizeProperty());
        ((TextArea) lookup(root, "#responseHeadersTextArea")).textProperty().bind(viewModel.responseHeadersProperty());
        ((TextArea) lookup(root, "#responseBodyTextArea")).textProperty().bind(viewModel.responseBodyProperty());

        // Checks Tab
        TableView<CheckReport> checksTable = (TableView<CheckReport>) lookup(root, "#checksTableView");
        checksTable.setItems(viewModel.getChecks());
        ((TableColumn<CheckReport, String>) checksTable.getColumns().get(0)).setCellValueFactory(new PropertyValueFactory<>("type"));
        ((TableColumn<CheckReport, String>) checksTable.getColumns().get(1)).setCellValueFactory(new PropertyValueFactory<>("expression"));
        ((TableColumn<CheckReport, String>) checksTable.getColumns().get(2)).setCellValueFactory(new PropertyValueFactory<>("operator"));
        ((TableColumn<CheckReport, String>) checksTable.getColumns().get(3)).setCellValueFactory(new PropertyValueFactory<>("expect"));
        ((TableColumn<CheckReport, String>) checksTable.getColumns().get(4)).setCellValueFactory(new PropertyValueFactory<>("actual"));
        TableColumn<CheckReport, Text> checkResultCol = (TableColumn<CheckReport, Text>) checksTable.getColumns().get(5);
        checkResultCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(
                FunctionalTestReportViewModel.getStyledTextForResult(param.getValue().isPassed())
        ));
    }


    private Node lookup(Node parent, String selector) {
        Node node = parent.lookup(selector);
        Objects.requireNonNull(node, "Could not find node with selector: " + selector);
        return node;
    }
} 