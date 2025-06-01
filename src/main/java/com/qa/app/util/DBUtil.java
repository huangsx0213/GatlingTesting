package com.qa.app.util;

import java.sql.Connection;
import java.sql.DriverManager;
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
            
            // Create gatling_tests table if it doesn't exist with the new schema
            String testsSql = "CREATE TABLE IF NOT EXISTS gatling_tests ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " is_enabled BOOLEAN NOT NULL DEFAULT 0,"
                    + " suite TEXT NOT NULL,"
                    + " tcid TEXT NOT NULL UNIQUE,"
                    + " descriptions TEXT,"
                    + " conditions TEXT,"
                    + " exp_status TEXT,"
                    + " exp_result TEXT,"
                    + " save_fields TEXT,"
                    + " endpoint TEXT NOT NULL,"
                    + " http_method TEXT NOT NULL DEFAULT 'GET',"
                    + " tags TEXT,"
                    + " wait_time INTEGER DEFAULT 0,"
                    + " headers TEXT,"
                    + " body TEXT,"
                    + " headers_template_name TEXT,"
                    + " body_template_name TEXT,"
                    + " headers_dynamic_variables TEXT,"
                    + " body_dynamic_variables TEXT"
                    + ");";
            stmt.execute(testsSql);

            
            // Create body_templates table if it doesn't exist
            String bodyTemplateSql = "CREATE TABLE IF NOT EXISTS body_templates ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " content TEXT NOT NULL,"
                    + " environment_id INTEGER,"
                    + " FOREIGN KEY(environment_id) REFERENCES environments(id) ON DELETE SET NULL ON UPDATE CASCADE"
                    + ");";
            stmt.execute(bodyTemplateSql);

            // Create headers_templates table if it doesn't exist
            String headersTemplateSql = "CREATE TABLE IF NOT EXISTS headers_templates ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " content TEXT NOT NULL,"
                    + " environment_id INTEGER,"
                    + " FOREIGN KEY(environment_id) REFERENCES environments(id) ON DELETE SET NULL ON UPDATE CASCADE"
                    + ");";
            stmt.execute(headersTemplateSql);

            // Create environments table if it doesn't exist
            String environmentSql = "CREATE TABLE IF NOT EXISTS environments ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " description TEXT"
                    + ");";
            stmt.execute(environmentSql);
            
            System.out.println("Database initialized with gatling_tests, body_templates, and headers_templates tables created (if not exists).");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }


    // Main method to initialize the database when the application starts or for testing
    public static void main(String[] args) {
        initializeDatabase();
    }
}