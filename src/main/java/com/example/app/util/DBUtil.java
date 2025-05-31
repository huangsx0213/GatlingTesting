package com.example.app.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtil {
    private static final String URL = "jdbc:sqlite:gatling_testing_system.db";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(URL);
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // Create users table if it doesn't exist (keeping for backward compatibility)
            String usersSql = "CREATE TABLE IF NOT EXISTS users ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL,"
                    + " email TEXT UNIQUE"
                    + ");";
            stmt.execute(usersSql);
            
            // Create gatling_tests table if it doesn't exist with the new schema
            String testsSql = "CREATE TABLE IF NOT EXISTS gatling_tests ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " is_run BOOLEAN NOT NULL DEFAULT 0,"
                    + " suite TEXT NOT NULL,"
                    + " tcid TEXT NOT NULL UNIQUE,"
                    + " descriptions TEXT,"
                    + " conditions TEXT,"
                    + " body_override TEXT,"
                    + " exp_status TEXT,"
                    + " exp_result TEXT,"
                    + " save_fields TEXT,"
                    + " endpoint TEXT NOT NULL,"
                    + " headers TEXT,"
                    + " body_template TEXT,"
                    + " tags TEXT,"
                    + " wait_time INTEGER DEFAULT 0,"
                    + " body_template_name TEXT,"
                    + " dynamic_variables TEXT"
                    + ");";
            stmt.execute(testsSql);
            
            // Perform schema migration if necessary
            checkAndMigrateSchema(conn);
            
            // Create body_templates table if it doesn't exist
            String bodyTemplateSql = "CREATE TABLE IF NOT EXISTS body_templates ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " content TEXT NOT NULL"
                    + ");";
            stmt.execute(bodyTemplateSql);

            // Create headers_templates table if it doesn't exist
            String headersTemplateSql = "CREATE TABLE IF NOT EXISTS headers_templates ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " content TEXT NOT NULL"
                    + ");";
            stmt.execute(headersTemplateSql);
            
            System.out.println("Database initialized with users, gatling_tests, body_templates, and headers_templates tables created (if not exists).");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    private static void checkAndMigrateSchema(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();

        // Check for body_default column (old schema indicator)
        try (ResultSet rs = metaData.getColumns(null, null, "gatling_tests", "body_default")) {
            if (rs.next()) {
                // body_default column exists, perform full migration
                System.out.println("Old schema detected. Migrating gatling_tests table...");
                try (Statement stmt = conn.createStatement()) {
                    // 1. Rename old table
                    stmt.execute("ALTER TABLE gatling_tests RENAME TO gatling_tests_old;");

                    // 2. Create new table with updated schema (already done by initializeDatabase)
                    // This is handled by initializeDatabase() already, so we just need to ensure the new table definition is correct.
                    // No explicit CREATE TABLE statement needed here.

                    // 3. Copy data from old to new table
                    String copySql = "INSERT INTO gatling_tests (id, is_run, suite, tcid, descriptions, " +
                                     "conditions, body_override, exp_status, exp_result, save_fields, " +
                                     "endpoint, headers, body_template, tags, wait_time, body_template_name, dynamic_variables) " +
                                     "SELECT id, is_run, suite, tcid, descriptions, conditions, body_override, " +
                                     "exp_status, exp_result, save_fields, endpoint, headers, body_template, " +
                                     "tags, wait_time, NULL, NULL FROM gatling_tests_old;"; // Set new columns to NULL initially
                    stmt.execute(copySql);

                    // 4. Drop old table
                    stmt.execute("DROP TABLE gatling_tests_old;");
                    System.out.println("Database migration completed successfully.");
                }
            } else {
                // body_default does not exist, check for new columns
                System.out.println("Checking for new columns...");
                try (Statement stmt = conn.createStatement()) {
                    // Check and add body_template_name if missing
                    try (ResultSet rsBodyTemplateName = metaData.getColumns(null, null, "gatling_tests", "body_template_name")) {
                        if (!rsBodyTemplateName.next()) {
                            stmt.execute("ALTER TABLE gatling_tests ADD COLUMN body_template_name TEXT;");
                            System.out.println("Added column body_template_name to gatling_tests.");
                        }
                    }
                    // Check and add dynamic_variables if missing
                    try (ResultSet rsDynamicVariables = metaData.getColumns(null, null, "gatling_tests", "dynamic_variables")) {
                        if (!rsDynamicVariables.next()) {
                            stmt.execute("ALTER TABLE gatling_tests ADD COLUMN dynamic_variables TEXT;");
                            System.out.println("Added column dynamic_variables to gatling_tests.");
                        }
                    }
                }
            }
        }
    }

    // Main method to initialize the database when the application starts or for testing
    public static void main(String[] args) {
        initializeDatabase();
    }
}