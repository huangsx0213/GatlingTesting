package com.qa.app.dao.impl;

import com.qa.app.dao.api.IDbConnectionDao;
import com.qa.app.dao.util.DBUtil;
import com.qa.app.model.DbConnection;
import com.qa.app.model.DbType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DbConnectionDaoImpl implements IDbConnectionDao {

    @Override
    public void add(DbConnection connection) {
        String sql = "INSERT INTO db_connections(alias, db_type, host, port, db_name, schema_name, service_name, username, password, pool_size, project_id, environment_id, description) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            mapDbConnectionToStatement(connection, pstmt);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(DbConnection connection) {
        String sql = "UPDATE db_connections SET alias = ?, db_type = ?, host = ?, port = ?, db_name = ?, schema_name = ?, service_name = ?, username = ?, password = ?, pool_size = ?, project_id = ?, environment_id = ?, description = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            mapDbConnectionToStatement(connection, pstmt);
            pstmt.setLong(14, connection.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(DbConnection connection) {
        String sql = "DELETE FROM db_connections WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, connection.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public DbConnection get(Long id) {
        String sql = "SELECT * FROM db_connections WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToDbConnection(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<DbConnection> getByProject(Integer projectId) {
        List<DbConnection> connections = new ArrayList<>();
        String sql = "SELECT * FROM db_connections WHERE project_id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                connections.add(mapRowToDbConnection(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connections;
    }

    @Override
    public DbConnection getByAlias(String alias) {
        String sql = "SELECT * FROM db_connections WHERE alias = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, alias);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToDbConnection(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<DbConnection> findAll() {
        List<DbConnection> connections = new ArrayList<>();
        String sql = "SELECT * FROM db_connections";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                connections.add(mapRowToDbConnection(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connections;
    }

    private DbConnection mapRowToDbConnection(ResultSet rs) throws SQLException {
        DbConnection connection = new DbConnection();
        connection.setId(rs.getLong("id"));
        connection.setAlias(rs.getString("alias"));
        connection.setDbType(DbType.valueOf(rs.getString("db_type").toUpperCase()));
        connection.setHost(rs.getString("host"));
        connection.setPort(rs.getInt("port"));
        connection.setDbName(rs.getString("db_name"));
        connection.setSchemaName(rs.getString("schema_name"));
        connection.setServiceName(rs.getString("service_name"));
        connection.setUsername(rs.getString("username"));
        connection.setPassword(rs.getString("password"));
        connection.setPoolSize(rs.getInt("pool_size"));
        connection.setProjectId(rs.getInt("project_id"));
        connection.setEnvironmentId(rs.getInt("environment_id"));
        connection.setDescription(rs.getString("description"));
        return connection;
    }

    private void mapDbConnectionToStatement(DbConnection connection, PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, connection.getAlias());
        pstmt.setString(2, connection.getDbType().name());
        pstmt.setString(3, connection.getHost());
        pstmt.setInt(4, connection.getPort());
        pstmt.setString(5, connection.getDbName());
        pstmt.setString(6, connection.getSchemaName());
        pstmt.setString(7, connection.getServiceName());
        pstmt.setString(8, connection.getUsername());
        pstmt.setString(9, connection.getPassword());
        pstmt.setInt(10, connection.getPoolSize());
        pstmt.setInt(11, connection.getProjectId());
        pstmt.setInt(12, connection.getEnvironmentId());
        pstmt.setString(13, connection.getDescription());
    }
} 