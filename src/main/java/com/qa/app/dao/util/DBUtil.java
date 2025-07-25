package com.qa.app.dao.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtil {
    private static final String URL = "jdbc:sqlite:gatling_testing_system.db";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(URL);
            // enable SQLite foreign key constraints
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            return conn;
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
                    + " tags TEXT,"
                    + " wait_time INTEGER DEFAULT 0,"
                    + " conditions TEXT,"
                    + " descriptions TEXT,"
                    + " endpoint_name TEXT,"
                    + " headers_template_id INTEGER,"
                    + " body_template_id INTEGER,"
                    + " endpoint_dynamic_variables TEXT,"
                    + " headers_dynamic_variables TEXT,"
                    + " body_dynamic_variables TEXT,"
                    + " response_checks TEXT,"
                    + " project_id INTEGER,"
                    + " report_path TEXT,"
                    + " last_run_passed BOOLEAN,"
                    + " display_order INT,"
                    + " FOREIGN KEY(project_id) REFERENCES project(id) ON DELETE SET NULL ON UPDATE CASCADE,"
                    + " FOREIGN KEY(headers_template_id) REFERENCES headers_templates(id) ON DELETE SET NULL ON UPDATE CASCADE,"
                    + " FOREIGN KEY(body_template_id) REFERENCES body_templates(id) ON DELETE SET NULL ON UPDATE CASCADE"
                    + ");";
            stmt.execute(testsSql);

            
            // Create body_templates table if it doesn't exist
            String bodyTemplateSql = "CREATE TABLE IF NOT EXISTS body_templates ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " content TEXT NOT NULL,"
                    + " description TEXT,"
                    + " project_id INTEGER,"
                    + " FOREIGN KEY(project_id) REFERENCES project(id) ON DELETE SET NULL ON UPDATE CASCADE"
                    + ");";
            stmt.execute(bodyTemplateSql);

            // Create headers_templates table if it doesn't exist
            String headersTemplateSql = "CREATE TABLE IF NOT EXISTS headers_templates ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " content TEXT NOT NULL,"
                    + " description TEXT,"
                    + " project_id INTEGER,"
                    + " FOREIGN KEY(project_id) REFERENCES project(id) ON DELETE SET NULL ON UPDATE CASCADE"
                    + ");";
            stmt.execute(headersTemplateSql);

            // Create environments table if it doesn't exist
            String environmentSql = "CREATE TABLE IF NOT EXISTS environments ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " description TEXT,"
                    + " project_id INTEGER,"
                    + " FOREIGN KEY(project_id) REFERENCES project(id) ON DELETE SET NULL ON UPDATE CASCADE"
                    + ");";
            stmt.execute(environmentSql);

            // Create endpoints table if it doesn't exist
            String endpointSql = "CREATE TABLE IF NOT EXISTS endpoints ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL,"
                    + " method TEXT NOT NULL,"
                    + " url TEXT NOT NULL,"
                    + " environment_id INTEGER,"
                    + " project_id INTEGER,"
                    + " display_order INT,"
                    + " FOREIGN KEY(environment_id) REFERENCES environments(id) ON DELETE RESTRICT ON UPDATE CASCADE,"
                    + " FOREIGN KEY(project_id) REFERENCES project(id) ON DELETE SET NULL ON UPDATE CASCADE,"
                    + " UNIQUE(name, environment_id)"
                    + ");";
            stmt.execute(endpointSql);

            // Create groovy_variables table if it doesn't exist
            String groovyVariableSql = "CREATE TABLE IF NOT EXISTS groovy_variables ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "value TEXT, "
                    + "description TEXT, "
                    + "environment_id INTEGER, "
                    + "project_id INTEGER, "
                    + "FOREIGN KEY (environment_id) REFERENCES environments(id) ON DELETE RESTRICT ON UPDATE CASCADE,"
                    + "FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL ON UPDATE CASCADE,"
                    + " UNIQUE(name, environment_id)"
                    + ");";
            stmt.execute(groovyVariableSql);

            // Create variable_transform_methods table if it doesn't exist
            String transformSql = "CREATE TABLE IF NOT EXISTS variable_transform_methods ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " description TEXT,"
                    + " script TEXT NOT NULL,"
                    + " enabled INTEGER NOT NULL DEFAULT 1,"
                    + " param_spec TEXT,"
                    + " sample_usage TEXT,"
                    + " create_time DATETIME,"
                    + " update_time DATETIME"
                    + ");";
            stmt.execute(transformSql);

            // Create project table if it doesn't exist
            String projectSql = "CREATE TABLE IF NOT EXISTS project ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE,"
                    + " description TEXT"
                    + ");";
            stmt.execute(projectSql);
            
            // Create scenarios table
            String scenarioSql = "CREATE TABLE IF NOT EXISTS scenario (" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " name TEXT NOT NULL UNIQUE," +
                    " desc TEXT," +
                    " thread_group_json TEXT," +
                    " schedule_json TEXT," +
                    " project_id INTEGER," +
                    " display_order INT," +
                    " is_functional_test BOOLEAN NOT NULL DEFAULT 0," +
                    " FOREIGN KEY(project_id) REFERENCES project(id) ON DELETE SET NULL ON UPDATE CASCADE" +
                    ");";
            stmt.execute(scenarioSql);

            // Create scenario_step table
            String stepSql = "CREATE TABLE IF NOT EXISTS scenario_step (" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " scenario_id INTEGER NOT NULL," +
                    " order_index INTEGER NOT NULL," +
                    " test_tcid TEXT NOT NULL," +
                    " wait_time INTEGER DEFAULT 0," +
                    " tags TEXT," +
                    " FOREIGN KEY(scenario_id) REFERENCES scenario(id) ON DELETE CASCADE ON UPDATE CASCADE" +
                    ");";
            stmt.execute(stepSql);

            // Create scenario_schedule table
            String schedSql = "CREATE TABLE IF NOT EXISTS scenario_schedule (" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " scenario_id INTEGER NOT NULL," +
                    " cron_expr TEXT NOT NULL," +
                    " next_run_at TEXT," +
                    " enabled BOOLEAN DEFAULT 1," +
                    " FOREIGN KEY(scenario_id) REFERENCES scenario(id) ON DELETE CASCADE ON UPDATE CASCADE" +
                    ");";
            stmt.execute(schedSql);

            // Create db_connections table if it doesn't exist
            String dbConnectionsSql = "CREATE TABLE IF NOT EXISTS db_connections("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " alias TEXT NOT NULL,"
                    + " description TEXT,"
                    + " db_type TEXT,"
                    + " host TEXT,"
                    + " port INTEGER,"
                    + " db_name TEXT,"
                    + " schema_name TEXT,"
                    + " service_name TEXT,"
                    + " username TEXT,"
                    + " password TEXT,"
                    + " pool_size INTEGER DEFAULT 5,"
                    + " project_id INTEGER,"
                    + " environment_id INTEGER,"
                    + " FOREIGN KEY(project_id) REFERENCES project(id) ON DELETE SET NULL ON UPDATE CASCADE,"
                    + " FOREIGN KEY(environment_id) REFERENCES environments(id) ON DELETE RESTRICT ON UPDATE CASCADE,"
                    + " UNIQUE(alias, environment_id)"
                    + ");";
            stmt.execute(dbConnectionsSql);

            
            System.out.println("Database schema initialized. All tables are up to date.");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    // Main method to initialize the database when the application starts or for testing
    public static void main(String[] args) {
        initializeDatabase();
    }
}