package com.example.app.model;

import javafx.beans.property.*;

public class GatlingTest {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final BooleanProperty isRun = new SimpleBooleanProperty();
    private final StringProperty suite = new SimpleStringProperty();
    private final StringProperty tcid = new SimpleStringProperty();
    private final StringProperty descriptions = new SimpleStringProperty();
    private final StringProperty conditions = new SimpleStringProperty();
    private final StringProperty bodyOverride = new SimpleStringProperty();
    private final StringProperty expStatus = new SimpleStringProperty();
    private final StringProperty expResult = new SimpleStringProperty();
    private final StringProperty saveFields = new SimpleStringProperty();
    private final StringProperty endpoint = new SimpleStringProperty();
    private final StringProperty headers = new SimpleStringProperty();
    private final StringProperty bodyTemplate = new SimpleStringProperty();
    private final StringProperty bodyDefault = new SimpleStringProperty();
    private final StringProperty tags = new SimpleStringProperty();
    private final IntegerProperty waitTime = new SimpleIntegerProperty();

    public GatlingTest() {
    }

    public GatlingTest(String suite, String tcid, String descriptions, String endpoint) {
        this.suite.set(suite);
        this.tcid.set(tcid);
        this.descriptions.set(descriptions);
        this.endpoint.set(endpoint);
        this.isRun.set(false);
        this.waitTime.set(0);
    }

    public GatlingTest(int id, boolean isRun, String suite, String tcid, String descriptions, 
                      String conditions, String bodyOverride, String expStatus, String expResult,
                      String saveFields, String endpoint, String headers, String bodyTemplate,
                      String bodyDefault, String tags, int waitTime) {
        this.id.set(id);
        this.isRun.set(isRun);
        this.suite.set(suite);
        this.tcid.set(tcid);
        this.descriptions.set(descriptions);
        this.conditions.set(conditions);
        this.bodyOverride.set(bodyOverride);
        this.expStatus.set(expStatus);
        this.expResult.set(expResult);
        this.saveFields.set(saveFields);
        this.endpoint.set(endpoint);
        this.headers.set(headers);
        this.bodyTemplate.set(bodyTemplate);
        this.bodyDefault.set(bodyDefault);
        this.tags.set(tags);
        this.waitTime.set(waitTime);
    }

    // Getters and Setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public boolean isRun() { return isRun.get(); }
    public void setRun(boolean isRun) { this.isRun.set(isRun); }
    public BooleanProperty isRunProperty() { return isRun; }

    public String getSuite() { return suite.get(); }
    public void setSuite(String suite) { this.suite.set(suite); }
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

    public String getBodyOverride() { return bodyOverride.get(); }
    public void setBodyOverride(String bodyOverride) { this.bodyOverride.set(bodyOverride); }
    public StringProperty bodyOverrideProperty() { return bodyOverride; }

    public String getExpStatus() { return expStatus.get(); }
    public void setExpStatus(String expStatus) { this.expStatus.set(expStatus); }
    public StringProperty expStatusProperty() { return expStatus; }

    public String getExpResult() { return expResult.get(); }
    public void setExpResult(String expResult) { this.expResult.set(expResult); }
    public StringProperty expResultProperty() { return expResult; }

    public String getSaveFields() { return saveFields.get(); }
    public void setSaveFields(String saveFields) { this.saveFields.set(saveFields); }
    public StringProperty saveFieldsProperty() { return saveFields; }

    public String getEndpoint() { return endpoint.get(); }
    public void setEndpoint(String endpoint) { this.endpoint.set(endpoint); }
    public StringProperty endpointProperty() { return endpoint; }

    public String getHeaders() { return headers.get(); }
    public void setHeaders(String headers) { this.headers.set(headers); }
    public StringProperty headersProperty() { return headers; }

    public String getBodyTemplate() { return bodyTemplate.get(); }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate.set(bodyTemplate); }
    public StringProperty bodyTemplateProperty() { return bodyTemplate; }

    public String getBodyDefault() { return bodyDefault.get(); }
    public void setBodyDefault(String bodyDefault) { this.bodyDefault.set(bodyDefault); }
    public StringProperty bodyDefaultProperty() { return bodyDefault; }

    public String getTags() { return tags.get(); }
    public void setTags(String tags) { this.tags.set(tags); }
    public StringProperty tagsProperty() { return tags; }

    public int getWaitTime() { return waitTime.get(); }
    public void setWaitTime(int waitTime) { this.waitTime.set(waitTime); }
    public IntegerProperty waitTimeProperty() { return waitTime; }

    @Override
    public String toString() {
        return "GatlingTest{" +
                "id=" + getId() +
                ", isRun=" + isRun() +
                ", suite='" + getSuite() + '\'' +
                ", tcid='" + getTcid() + '\'' +
                ", descriptions='" + getDescriptions() + '\'' +
                ", endpoint='" + getEndpoint() + '\'' +
                '}';
    }
}