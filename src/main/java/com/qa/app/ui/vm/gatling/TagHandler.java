package com.qa.app.ui.vm.gatling;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;

public class TagHandler {
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final FlowPane tagsFlowPane;
    private final TextField tagsInputField;

    public TagHandler(FlowPane tagsFlowPane, TextField tagsInputField) {
        this.tagsFlowPane = tagsFlowPane;
        this.tagsInputField = tagsInputField;
        updateTagsFlowPane();
    }

    public ObservableList<String> getTags() {
        return tags;
    }

    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !tags.contains(tag.trim())) {
            tags.add(tag.trim());
            updateTagsFlowPane();
            tagsInputField.clear();
        }
    }

    public void removeTag(String tag) {
        tags.remove(tag);
        updateTagsFlowPane();
    }

    private void addTagChip(String tag) {
        HBox chip = new HBox();
        chip.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 4 8; -fx-border-radius: 4; -fx-background-radius: 4; -fx-alignment: center;");
        Label label = new Label(tag);
        label.setStyle("-fx-font-size: 12px;");
        Label close = new Label("  ×");
        close.setStyle("-fx-text-fill: #888; -fx-cursor: hand; -fx-font-size: 14px;");
        close.setOnMouseClicked(e -> removeTag(tag));
        chip.getChildren().addAll(label, close);
        tagsFlowPane.getChildren().add(chip);
    }

    public void updateTagsFlowPane() {
        tagsFlowPane.getChildren().clear();
        for (String tag : tags) {
            addTagChip(tag);
        }
        tagsFlowPane.getChildren().add(tagsInputField);
    }

    public String getTagsString() {
        return String.join(",", tags);
    }

    public void setTagsFromString(String tagsString) {
        tags.clear();
        if (tagsString != null && !tagsString.isEmpty()) {
            for (String tag : tagsString.split(",")) {
                if (!tag.trim().isEmpty()) tags.add(tag.trim());
            }
        }
        updateTagsFlowPane();
    }
} 