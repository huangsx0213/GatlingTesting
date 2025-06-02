package com.qa.app.model;

import javafx.beans.property.*;

import java.util.HashMap;
import java.util.Map;

public class GatlingTest {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final BooleanProperty isEnabled = new SimpleBooleanProperty();
    private final StringProperty suite = new SimpleStringProperty();
    private final StringProperty tcid = new SimpleStringProperty();
    private final StringProperty descriptions = new SimpleStringProperty();
    private final StringProperty conditions = new SimpleStringProperty();
    private final StringProperty expStatus = new SimpleStringProperty();
    private final StringProperty expResult = new SimpleStringProperty();
    private final StringProperty saveFields = new SimpleStringProperty();
    private final IntegerProperty endpointId = new SimpleIntegerProperty();
    private final StringProperty headers = new SimpleStringProperty();
    private final StringProperty body = new SimpleStringProperty();
    private final StringProperty tags = new SimpleStringProperty();
    private final IntegerProperty waitTime = new SimpleIntegerProperty();
    private final IntegerProperty bodyTemplateId = new SimpleIntegerProperty();
    private final IntegerProperty headersTemplateId = new SimpleIntegerProperty();
    private Map<String, String> dynamicVariables = new HashMap<>();
    private Map<String, String> headersDynamicVariables = new HashMap<>();

    public GatlingTest() {
    }

    public GatlingTest(String suite, String tcid, String descriptions, int endpointId) {
        this.suite.set(suite == null ? "" : suite);
        this.tcid.set(tcid);
        this.descriptions.set(descriptions);
        this.endpointId.set(endpointId);
        this.isEnabled.set(false);
        this.waitTime.set(0);
    }

    public GatlingTest(int id, boolean isEnabled, String suite, String tcid, String descriptions,
                      String conditions, String expStatus, String expResult,
                      String saveFields, int endpointId, String headers, String body,
                      String tags, int waitTime, int bodyTemplateId, int headersTemplateId, Map<String, String> dynamicVariables, Map<String, String> headersDynamicVariables) {
        this.id.set(id);
        this.isEnabled.set(isEnabled);
        this.suite.set(suite);
        this.tcid.set(tcid);
        this.descriptions.set(descriptions);
        this.conditions.set(conditions);
        this.expStatus.set(expStatus);
        this.expResult.set(expResult);
        this.saveFields.set(saveFields);
        this.endpointId.set(endpointId);
        this.headers.set(headers);
        this.body.set(body);
        this.tags.set(tags);
        this.waitTime.set(waitTime);
        this.bodyTemplateId.set(bodyTemplateId);
        this.headersTemplateId.set(headersTemplateId);
        this.dynamicVariables = dynamicVariables;
        this.headersDynamicVariables = headersDynamicVariables;
    }

    // Getters and Setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public boolean isEnabled() { return isEnabled.get(); }
    public void setEnabled(boolean isEnabled) { this.isEnabled.set(isEnabled); }
    public BooleanProperty isEnabledProperty() { return isEnabled; }

    public String getSuite() { return suite.get(); }
    public void setSuite(String suite) { this.suite.set(suite == null ? "" : suite); }
    public StringProperty suiteProperty() { return suite; }

    public String getTcid() { return tcid.get(); }
    public void setTcid(String tcid) { this.tcid.set(tcid); }
    public StringProperty tcidProperty() { return tcid; }

    public String getDescriptions() { return descriptions.get(); }
    public void setDescriptions(String descriptions) { this.descriptions.set(descriptions); }
    public StringProperty descriptionsProperty() { return descriptions; }

    public String getConditions() { return conditions.get(); }
    public void setConditions(String conditions) { this.conditions.set(conditions); }
    public StringProperty conditionsProperty() { return conditions; }

    public String getExpStatus() { return expStatus.get(); }
    public void setExpStatus(String expStatus) { this.expStatus.set(expStatus); }
    public StringProperty expStatusProperty() { return expStatus; }

    public String getExpResult() { return expResult.get(); }
    public void setExpResult(String expResult) { this.expResult.set(expResult); }
    public StringProperty expResultProperty() { return expResult; }

    public String getSaveFields() { return saveFields.get(); }
    public void setSaveFields(String saveFields) { this.saveFields.set(saveFields); }
    public StringProperty saveFieldsProperty() { return saveFields; }

    public int getEndpointId() { return endpointId.get(); }
    public void setEndpointId(int endpointId) { this.endpointId.set(endpointId); }
    public IntegerProperty endpointIdProperty() { return endpointId; }

    public String getHeaders() { return headers.get(); }
    public void setHeaders(String headers) { this.headers.set(headers); }
    public StringProperty headersProperty() { return headers; }

    public String getBody() { return body.get(); }
    public void setBody(String bodyTemplate) { this.body.set(bodyTemplate); }
    public StringProperty bodyTemplateProperty() { return body; }

    public String getTags() { return tags.get(); }
    public void setTags(String tags) { this.tags.set(tags); }
    public StringProperty tagsProperty() { return tags; }

    public int getWaitTime() { return waitTime.get(); }
    public void setWaitTime(int waitTime) { this.waitTime.set(waitTime); }
    public IntegerProperty waitTimeProperty() { return waitTime; }

    public int getBodyTemplateId() { return bodyTemplateId.get(); }
    public void setBodyTemplateId(int bodyTemplateId) { this.bodyTemplateId.set(bodyTemplateId); }
    public IntegerProperty bodyTemplateIdProperty() { return bodyTemplateId; }

    public int getHeadersTemplateId() { return headersTemplateId.get(); }
    public void setHeadersTemplateId(int headersTemplateId) { this.headersTemplateId.set(headersTemplateId); }
    public IntegerProperty headersTemplateIdProperty() { return headersTemplateId; }

    public Map<String, String> getBodyDynamicVariables() { return dynamicVariables; }
    public void setDynamicVariables(Map<String, String> dynamicVariables) { this.dynamicVariables = dynamicVariables; }

    public Map<String, String> getHeadersDynamicVariables() { return headersDynamicVariables; }
    public void setHeadersDynamicVariables(Map<String, String> headersDynamicVariables) { this.headersDynamicVariables = headersDynamicVariables; }

    @Override
    public String toString() {
        return "GatlingTest{" +
                "id=" + getId() +
                ", isEnabled=" + isEnabled() +
                ", suite='" + getSuite() + '\'' +
                ", tcid='" + getTcid() + '\'' +
                ", descriptions='" + getDescriptions() + '\'' +
                ", endpointId=" + getEndpointId() +
                '}';
    }
}