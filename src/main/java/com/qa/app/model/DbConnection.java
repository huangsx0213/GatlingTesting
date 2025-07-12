package com.qa.app.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DbConnection {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty alias = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final IntegerProperty poolSize = new SimpleIntegerProperty(5);
    private final IntegerProperty projectId = new SimpleIntegerProperty();
    private final IntegerProperty environmentId = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();

    private final ObjectProperty<DbType> dbType = new SimpleObjectProperty<>();
    private final StringProperty host = new SimpleStringProperty();
    private final IntegerProperty port = new SimpleIntegerProperty();
    private final StringProperty database = new SimpleStringProperty();
    private final StringProperty schema = new SimpleStringProperty();
    private final StringProperty serviceName = new SimpleStringProperty();
    private final StringProperty jdbcUrl = new SimpleStringProperty();

    public DbConnection() {}

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getAlias() {
        return alias.get();
    }

    public StringProperty aliasProperty() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias.set(alias);
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public int getPoolSize() {
        return poolSize.get();
    }

    public IntegerProperty poolSizeProperty() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize.set(poolSize);
    }

    public int getProjectId() {
        return projectId.get();
    }

    public IntegerProperty projectIdProperty() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId.set(projectId);
    }

    public int getEnvironmentId() {
        return environmentId.get();
    }

    public IntegerProperty environmentIdProperty() {
        return environmentId;
    }

    public void setEnvironmentId(int environmentId) {
        this.environmentId.set(environmentId);
    }
    
    public String getUsername() {
        return username.get();
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public String getPassword() {
        return password.get();
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    // ===== DbType =====
    public DbType getDbType() { return dbType.get(); }
    public ObjectProperty<DbType> dbTypeProperty() { return dbType; }
    public void setDbType(DbType type) { this.dbType.set(type); }

    // ===== Host =====
    public String getHost() { return host.get(); }
    public StringProperty hostProperty() { return host; }
    public void setHost(String h) { this.host.set(h); }

    // ===== Port =====
    public int getPort() { return port.get(); }
    public IntegerProperty portProperty() { return port; }
    public void setPort(int p) { this.port.set(p); }

    // ===== Database =====
    public String getDatabase() { return database.get(); }
    public StringProperty databaseProperty() { return database; }
    public void setDatabase(String d) { this.database.set(d); }

    // ===== Schema =====
    public String getSchema() { return schema.get(); }
    public StringProperty schemaProperty() { return schema; }
    public void setSchema(String s) { this.schema.set(s); }

    // ===== ServiceName =====
    public String getServiceName() { return serviceName.get(); }
    public StringProperty serviceNameProperty() { return serviceName; }
    public void setServiceName(String s) { this.serviceName.set(s); }

    // ===== jdbcUrl (generated) =====
    public String getJdbcUrl() { return jdbcUrl.get(); }
    public StringProperty jdbcUrlProperty() { return jdbcUrl; }
    public void setJdbcUrl(String url) { this.jdbcUrl.set(url); }
} 