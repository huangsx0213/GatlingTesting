package com.qa.app.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;

/**
 * Domain model representing a user-defined variable transform method.
 * A transform method contains a Groovy script which will be executed to convert
 * an incoming value with optional parameters.
 *
 * Signature inside the script:
 *   Object value   – original value to be converted
 *   List<String> params – parameters parsed from the expression
 *
 * The script MUST return the converted value.
 */
public class VariableTransformMethod {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty script = new SimpleStringProperty();
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);
    private final StringProperty paramSpec = new SimpleStringProperty();
    private final StringProperty sampleUsage = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> createTime = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updateTime = new SimpleObjectProperty<>();

    public VariableTransformMethod() {
    }

    public VariableTransformMethod(String name, String description, String script) {
        this.name.set(name);
        this.description.set(description);
        this.script.set(script);
    }

    // ------------------------------------------------------------
    // Getters / setters (JavaFX properties for UI binding)
    // ------------------------------------------------------------
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }

    public String getScript() { return script.get(); }
    public void setScript(String script) { this.script.set(script); }
    public StringProperty scriptProperty() { return script; }

    public boolean isEnabled() { return enabled.get(); }
    public void setEnabled(boolean enabled) { this.enabled.set(enabled); }
    public BooleanProperty enabledProperty() { return enabled; }

    public String getParamSpec() { return paramSpec.get(); }
    public void setParamSpec(String paramSpec) { this.paramSpec.set(paramSpec); }
    public StringProperty paramSpecProperty() { return paramSpec; }

    public String getSampleUsage() { return sampleUsage.get(); }
    public void setSampleUsage(String sampleUsage) { this.sampleUsage.set(sampleUsage); }
    public StringProperty sampleUsageProperty() { return sampleUsage; }

    public LocalDateTime getCreateTime() { return createTime.get(); }
    public void setCreateTime(LocalDateTime createTime) { this.createTime.set(createTime); }
    public ObjectProperty<LocalDateTime> createTimeProperty() { return createTime; }

    public LocalDateTime getUpdateTime() { return updateTime.get(); }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime.set(updateTime); }
    public ObjectProperty<LocalDateTime> updateTimeProperty() { return updateTime; }

    @Override
    public String toString() {
        return name.get();
    }
} 