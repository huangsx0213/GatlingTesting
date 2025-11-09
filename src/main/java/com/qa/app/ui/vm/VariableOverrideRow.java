package com.qa.app.ui.vm;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class VariableOverrideRow {
    private final StringProperty name;
    private final StringProperty originalValue;
    private final StringProperty overrideValue;
    private final BooleanProperty newlyAdded = new SimpleBooleanProperty(false);

    public VariableOverrideRow(String name, String originalValue, String overrideValue) {
        this.name = new SimpleStringProperty(name);
        this.originalValue = new SimpleStringProperty(originalValue);
        this.overrideValue = new SimpleStringProperty(overrideValue);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getOriginalValue() {
        return originalValue.get();
    }

    public StringProperty originalValueProperty() {
        return originalValue;
    }

    public String getOverrideValue() {
        return overrideValue.get();
    }

    public StringProperty overrideValueProperty() {
        return overrideValue;
    }

    public void setOverrideValue(String value) {
        this.overrideValue.set(value);
    }

    public boolean isNewlyAdded() {
        return newlyAdded.get();
    }

    public void setNewlyAdded(boolean v) {
        newlyAdded.set(v);
    }
}
