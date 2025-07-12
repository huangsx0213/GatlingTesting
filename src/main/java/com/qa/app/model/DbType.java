package com.qa.app.model;

/**
 * Supported database types. Each enum value has an associated string key used for
 * driver resolution and database operations.
 */
public enum DbType {
    MYSQL("mysql"),
    POSTGRESQL("postgresql"),
    ORACLE("oracle"),
    SQLSERVER("sqlserver");
    
    private final String key;
    
    DbType(String key) {
        this.key = key;
    }
    
    /**
     * Convert from lowercase key to enum constant.
     */
    public static DbType fromKey(String key) {
        if (key == null) return null;
        return switch (key.toLowerCase()) {
            case "mysql" -> MYSQL;
            case "postgresql" -> POSTGRESQL;
            case "oracle" -> ORACLE;
            case "sqlserver" -> SQLSERVER;
            default -> throw new IllegalArgumentException("Unsupported DB type: " + key);
        };
    }

    /**
     * Returns the canonical string key for this database type.
     */
    public String key() {
        return key;
    }
} 