package com.example.app.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ConditionRow {
    private final StringProperty prefix;
    private final ListProperty<String> tcids;

    public ConditionRow(String prefix, ObservableList<String> tcids) {
        this.prefix = new SimpleStringProperty(prefix);
        this.tcids = new SimpleListProperty<>(tcids);
    }

    public ConditionRow(String prefix, String tcid) {
        this(prefix, FXCollections.observableArrayList(tcid));
    }

    public String getPrefix() {
        return prefix.get();
    }

    public void setPrefix(String prefix) {
        this.prefix.set(prefix);
    }

    public StringProperty prefixProperty() {
        return prefix;
    }

    public ObservableList<String> getTcids() {
        return tcids.get();
    }

    public void setTcids(ObservableList<String> tcids) {
        this.tcids.set(tcids);
    }

    public ListProperty<String> tcidsProperty() {
        return tcids;
    }

    public String getTcid() {
        return tcids.isEmpty() ? "" : tcids.get(0);
    }

    public void setTcid(String tcid) {
        this.tcids.set(FXCollections.observableArrayList(tcid));
    }
} 