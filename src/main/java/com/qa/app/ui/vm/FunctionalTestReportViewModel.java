package com.qa.app.ui.vm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.reports.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

public class FunctionalTestReportViewModel {

    private final StringProperty originTcid = new SimpleStringProperty();
    private final StringProperty suite = new SimpleStringProperty();
    private TreeItem<Object> rootNode;
    private FunctionalTestReport report;

    // Properties for detail view
    private final StringProperty method = new SimpleStringProperty("-");
    private final StringProperty url = new SimpleStringProperty("-");
    private final StringProperty requestHeaders = new SimpleStringProperty("-");
    private final StringProperty requestBody = new SimpleStringProperty("-");

    private final StringProperty responseStatus = new SimpleStringProperty("-");
    private final StringProperty latency = new SimpleStringProperty("-");
    private final StringProperty size = new SimpleStringProperty("-");
    private final StringProperty responseHeaders = new SimpleStringProperty("-");
    private final StringProperty responseBody = new SimpleStringProperty("-");

    private final ObservableList<CheckReport> checks = FXCollections.observableArrayList();

    public FunctionalTestReportViewModel(String reportPath) {
        try {
            this.report = new ObjectMapper().readValue(new File(reportPath), FunctionalTestReport.class);
            populate();
        } catch (IOException e) {
            System.err.println("Failed to load or parse report: " + reportPath);
            e.printStackTrace();
            // Handle error state, maybe show an error dialog
        }
    }

    private void populate() {
        if (report == null) {
            return;
        }

        originTcid.set(report.getOriginTcid());
        suite.set(report.getSuite());

        rootNode = new TreeItem<>(); // Invisible root
        for (ModeGroup modeGroup : report.getGroups()) {
            TreeItem<Object> modeItem = new TreeItem<>(modeGroup);
            for (CaseReport caseReport : modeGroup.getCases()) {
                TreeItem<Object> caseItem = new TreeItem<>(caseReport);
                for (RequestReport requestReport : caseReport.getItems()) {
                    caseItem.getChildren().add(new TreeItem<>(requestReport));
                }
                modeItem.getChildren().add(caseItem);
            }
            rootNode.getChildren().add(modeItem);
        }
    }

    public void updateDetails(Object selectedItem) {
        if (selectedItem instanceof RequestReport) {
            RequestReport rr = (RequestReport) selectedItem;
            RequestInfo req = rr.getRequest();
            ResponseInfo res = rr.getResponse();

            method.set(req.getMethod());
            url.set(req.getUrl());
            requestHeaders.set(req.getHeaders().entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n")));
            requestBody.set(req.getBody());

            responseStatus.set(String.valueOf(res.getStatus()));
            latency.set(String.valueOf(res.getLatencyMs()));
            size.set(String.valueOf(res.getSizeBytes()));
            responseHeaders.set(res.getHeaders().entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n")));
            responseBody.set(res.getBodySample());

            checks.setAll(rr.getChecks());

        } else {
            // Clear details if a non-request item is selected
            method.set("-");
            url.set("-");
            requestHeaders.set("");
            requestBody.set("");
            responseStatus.set("-");
            latency.set("-");
            size.set("-");
            responseHeaders.set("");
            responseBody.set("");
            checks.clear();
        }
    }

    public static Text getStyledTextForResult(boolean passed) {
        Text text;
        if (passed) {
            text = new Text("✓ PASSED");
            text.setFill(Color.GREEN);
        } else {
            text = new Text("✗ FAILED");
            text.setFill(Color.RED);
        }
        return text;
    }


    // Getters for properties
    public StringProperty originTcidProperty() {
        return originTcid;
    }

    public StringProperty suiteProperty() {
        return suite;
    }

    public TreeItem<Object> getRootNode() {
        return rootNode;
    }

    public StringProperty methodProperty() { return method; }
    public StringProperty urlProperty() { return url; }
    public StringProperty requestHeadersProperty() { return requestHeaders; }
    public StringProperty requestBodyProperty() { return requestBody; }
    public StringProperty responseStatusProperty() { return responseStatus; }
    public StringProperty latencyProperty() { return latency; }
    public StringProperty sizeProperty() { return size; }
    public StringProperty responseHeadersProperty() { return responseHeaders; }
    public StringProperty responseBodyProperty() { return responseBody; }

    public ObservableList<CheckReport> getChecks() {
        return checks;
    }
} 