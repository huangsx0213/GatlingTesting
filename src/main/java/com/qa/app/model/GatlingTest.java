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
    private final StringProperty responseChecks = new SimpleStringProperty();
    // private final IntegerProperty endpointId = new SimpleIntegerProperty(); // Deprecated
    private final StringProperty headers = new SimpleStringProperty();
    private final StringProperty body = new SimpleStringProperty();
    private final StringProperty tags = new SimpleStringProperty();
    private final IntegerProperty waitTime = new SimpleIntegerProperty();
    private final IntegerProperty bodyTemplateId = new SimpleIntegerProperty();
    private final IntegerProperty headersTemplateId = new SimpleIntegerProperty();
    private final StringProperty endpointName = new SimpleStringProperty();
    private final IntegerProperty projectId = new SimpleIntegerProperty();
    private int displayOrder;
    private Map<String, String> dynamicVariables = new HashMap<>();
    private Map<String, String> headersDynamicVariables = new HashMap<>();
    private Map<String, String> endpointDynamicVariables = new HashMap<>();
    private String reportPath;
    private Boolean lastRunPassed;

    // Deprecated fields removed: expStatus, saveFields

    public GatlingTest() {
        // This constructor should be empty or initialize properties with default values.
        // The erroneous line has been removed.
    }

    public GatlingTest(String suite, String tcid, String descriptions, String endpointName, Integer projectId) {
        this.suite.set(suite == null ? "" : suite);
        this.tcid.set(tcid);
        this.descriptions.set(descriptions);
        this.endpointName.set(endpointName);
        this.isEnabled.set(false);
        this.waitTime.set(0);
        if (projectId != null) {
            this.projectId.set(projectId);
        }
    }

    public GatlingTest(int id, boolean isEnabled, String suite, String tcid, String descriptions,
                      String conditions, String responseChecks,
                      String headers, String body,
                      String tags, int waitTime, int bodyTemplateId, int headersTemplateId, Map<String, String> dynamicVariables, Map<String, String> headersDynamicVariables, Map<String, String> endpointDynamicVariables) {
        this.id.set(id);
        this.isEnabled.set(isEnabled);
        this.suite.set(suite);
        this.tcid.set(tcid);
        this.descriptions.set(descriptions);
        this.conditions.set(conditions);
        this.responseChecks.set(responseChecks);
        this.headers.set(headers);
        this.body.set(body);
        this.tags.set(tags);
        this.waitTime.set(waitTime);
        this.bodyTemplateId.set(bodyTemplateId);
        this.headersTemplateId.set(headersTemplateId);
        this.dynamicVariables = dynamicVariables;
        this.headersDynamicVariables = headersDynamicVariables;
        this.endpointDynamicVariables = endpointDynamicVariables;
    }

    /**
     * Copy constructor for duplication.
     * @param other The GatlingTest to copy.
     */
    public GatlingTest(GatlingTest other) {
        // Do not copy ID to allow for new insertion
        this.isEnabled.set(false); // Default to disabled
        this.suite.set(other.getSuite());
        this.tcid.set(other.getTcid() + "_copy");
        this.descriptions.set(other.getDescriptions());
        this.conditions.set(other.getConditions());
        this.responseChecks.set(other.getResponseChecks());
        this.headers.set(other.getHeaders());
        this.body.set(other.getBody());
        this.tags.set(other.getTags());
        this.waitTime.set(other.getWaitTime());
        this.bodyTemplateId.set(other.getBodyTemplateId());
        this.headersTemplateId.set(other.getHeadersTemplateId());
        this.endpointName.set(other.getEndpointName());
        this.projectId.set(other.getProjectId());
        // Deep copy maps to avoid shared references
        this.dynamicVariables = new HashMap<>(other.getBodyDynamicVariables());
        this.headersDynamicVariables = new HashMap<>(other.getHeadersDynamicVariables());
        this.endpointDynamicVariables = new HashMap<>(other.getEndpointDynamicVariables());
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

    public String getResponseChecks() { return responseChecks.get(); }
    public void setResponseChecks(String responseChecks) { this.responseChecks.set(responseChecks); }
    public StringProperty responseChecksProperty() { return responseChecks; }

    /* Deprecated
    public int getEndpointId() { return endpointId.get(); }
    public void setEndpointId(int endpointId) { this.endpointId.set(endpointId); }
    public IntegerProperty endpointIdProperty() { return endpointId; }
    */

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

    public Map<String, String> getEndpointDynamicVariables() { return endpointDynamicVariables; }
    public void setEndpointDynamicVariables(Map<String, String> endpointDynamicVariables) { this.endpointDynamicVariables = endpointDynamicVariables; }

    public String getEndpointName() { return endpointName.get(); }
    public void setEndpointName(String endpointName) { this.endpointName.set(endpointName); }
    public StringProperty endpointNameProperty() { return endpointName; }

    public Integer getProjectId() {
        return projectId.get();
    }
    public void setProjectId(Integer projectId) {
        if (projectId != null) {
            this.projectId.set(projectId);
        }
    }
    public IntegerProperty projectIdProperty() {
        return projectId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public Boolean getLastRunPassed() {
        return lastRunPassed;
    }

    public void setLastRunPassed(Boolean lastRunPassed) {
        this.lastRunPassed = lastRunPassed;
    }

    @Override
    public String toString() {
        return "GatlingTest{" +
                "id=" + getId() +
                ", isEnabled=" + isEnabled() +
                ", suite='" + getSuite() + '\'' +
                ", tcid='" + getTcid() + '\'' +
                ", descriptions='" + getDescriptions() + '\'' +
                ", endpointName='" + getEndpointName() + '\'' +
                '}';
    }
}