package com.qa.app.model;

/**
 * Supported database types. The enum name is used as canonical key for driver resolution
 * and JDBC URL building.
 */
public enum DbType {
    MYSQL,
    POSTGRESQL,
    ORACLE,
    SQLSERVER;

    /**
     * Convert from lowercase key (e.g. "postgres") to enum constant.
     */
    public static DbType fromKey(String key) {
        if (key == null) return null;
        return switch (key.toLowerCase()) {
            case "mysql" -> MYSQL;
            case "postgres", "postgresql" -> POSTGRESQL;
            case "oracle" -> ORACLE;
            case "sqlserver", "mssql" -> SQLSERVER;
            default -> throw new IllegalArgumentException("Unsupported DB type: " + key);
        };
    }

    public String key() {
        return switch (this) {
            case MYSQL -> "mysql";
            case POSTGRESQL -> "postgresql";
            case ORACLE -> "oracle";
            case SQLSERVER -> "sqlserver";
        };
    }
} 