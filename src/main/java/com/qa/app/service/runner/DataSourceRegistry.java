package com.qa.app.service.runner;

import com.qa.app.model.DbConnection;
import com.qa.app.model.DbType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataSourceRegistry {
    private static final ConcurrentMap<String, DataSource> CACHE = new ConcurrentHashMap<>();

    public static void evict(String alias) {
        DataSource ds = CACHE.remove(alias);
        if (ds instanceof HikariDataSource) {
            ((HikariDataSource) ds).close();
        }
    }

    private static final java.util.Map<String, String> DRIVER_MAP = java.util.Map.ofEntries(
            java.util.Map.entry("postgres", "org.postgresql.Driver"),
            java.util.Map.entry("postgresql", "org.postgresql.Driver"),
            java.util.Map.entry("mysql", "com.mysql.cj.jdbc.Driver"),
            java.util.Map.entry("mariadb", "org.mariadb.jdbc.Driver"),
            java.util.Map.entry("oracle", "oracle.jdbc.driver.OracleDriver"),
            java.util.Map.entry("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
    );

    public static String buildJdbcUrl(DbConnection cfg) {
        if (cfg.getDbType() == null) {
            return cfg.getJdbcUrl();
        }
        return switch (cfg.getDbType()) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s", cfg.getHost(), cfg.getPort(), cfg.getDatabase());
            case POSTGRESQL -> {
                String base = String.format("jdbc:postgresql://%s:%d/%s", cfg.getHost(), cfg.getPort(), cfg.getDatabase());
                yield (cfg.getSchema() == null || cfg.getSchema().isBlank()) ? base : base + "?currentSchema=" + cfg.getSchema();
            }
            case SQLSERVER -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s", cfg.getHost(), cfg.getPort(), cfg.getDatabase());
            case ORACLE -> String.format("jdbc:oracle:thin:@//%s:%d/%s", cfg.getHost(), cfg.getPort(), cfg.getServiceName());
        };
    }

    public static DataSource get(DbConnection cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("DbConnection configuration cannot be null.");
        }
        return CACHE.computeIfAbsent(cfg.getAlias(), alias -> {
            if (cfg.getDbType() == null) {
                throw new IllegalStateException("Database Type is not configured for connection: " + alias);
            }
            String driverClass = DRIVER_MAP.get(cfg.getDbType().key());
            if (driverClass == null) {
                throw new IllegalStateException("Unsupported DB type for driver resolution: " + cfg.getDbType());
            }

            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("JDBC Driver not found: " + driverClass, e);
            }
            HikariConfig hc = new HikariConfig();
            String url = buildJdbcUrl(cfg);
            hc.setJdbcUrl(url);
            hc.setUsername(cfg.getUsername());
            hc.setPassword(cfg.getPassword());
            hc.setMaximumPoolSize(cfg.getPoolSize());
            hc.setDriverClassName(driverClass);
            return new HikariDataSource(hc);
        });
    }
    
    public static String executeQuery(String connectionAlias, String sql) throws Exception {
        if (!CACHE.containsKey(connectionAlias)) {
            throw new IllegalArgumentException("Database connection not found: " + connectionAlias);
        }
        
        DataSource ds = CACHE.get(connectionAlias);
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            // For SELECT queries, return formatted results
            if (sql.trim().toLowerCase().startsWith("select")) {
                return formatResultSet(rs);
            } 
            // For non-SELECT queries, return affected row count
            else {
                int count = stmt.getUpdateCount();
                return String.valueOf(count);
            }
        }
    }
    
    private static String formatResultSet(ResultSet rs) throws Exception {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // For single column, single row results, return just the value
        if (columnCount == 1 && rs.next()) {
            String value = rs.getString(1);
            return value != null ? value : "null";
        }
        
        // For more complex results, return JSON-like format
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        boolean hasRows = false;
        while (rs.next()) {
            if (hasRows) sb.append(",");
            hasRows = true;
            
            sb.append("{");
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                String value = rs.getString(i);
                
                if (i > 1) sb.append(",");
                sb.append("\"").append(columnName).append("\":");
                sb.append(value == null ? "null" : "\"" + value.replace("\"", "\\\"") + "\"");
            }
            sb.append("}");
        }
        
        sb.append("]");
        return hasRows ? sb.toString() : "[]";
    }

    public static void shutdown() {
        CACHE.values().forEach(ds -> {
            if (ds instanceof HikariDataSource) {
                ((HikariDataSource) ds).close();
            }
        });
        CACHE.clear();
    }
} 