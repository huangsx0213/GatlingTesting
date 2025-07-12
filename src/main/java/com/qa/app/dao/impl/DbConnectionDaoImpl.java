package com.qa.app.dao.impl;

import com.qa.app.dao.api.IDbConnectionDao;
import com.qa.app.dao.util.DBUtil;
import com.qa.app.model.DbConnection;
import com.qa.app.model.DbType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DbConnectionDaoImpl implements IDbConnectionDao {

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
            pstmt.setInt(14, connection.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(DbConnection connection) {
        String sql = "DELETE FROM db_connections WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, connection.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private DbConnection mapRowToDbConnection(ResultSet rs) throws SQLException {
        DbConnection conn = new DbConnection();
        conn.setId(rs.getInt("id"));
        conn.setAlias(rs.getString("alias"));
        conn.setDbType(DbType.fromKey(rs.getString("db_type")));
        conn.setHost(rs.getString("host"));
        conn.setPort(rs.getInt("port"));
        conn.setDatabase(rs.getString("db_name"));
        conn.setSchema(rs.getString("schema_name"));
        conn.setServiceName(rs.getString("service_name"));
        // url not persisted; generate dynamically
        conn.setJdbcUrl(com.qa.app.service.runner.DataSourceRegistry.buildJdbcUrl(conn));
        conn.setUsername(rs.getString("username"));
        conn.setPassword(rs.getString("password"));
        conn.setPoolSize(rs.getInt("pool_size"));
        conn.setProjectId(rs.getInt("project_id"));
        conn.setEnvironmentId(rs.getInt("environment_id"));
        conn.setDescription(rs.getString("description"));
        return conn;
    }
    
    private void mapDbConnectionToStatement(DbConnection connection, PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, connection.getAlias());
        pstmt.setString(2, connection.getDbType() == null ? null : connection.getDbType().key());
        pstmt.setString(3, connection.getHost());
        pstmt.setInt(4, connection.getPort());
        pstmt.setString(5, connection.getDatabase());
        pstmt.setString(6, connection.getSchema());
        pstmt.setString(7, connection.getServiceName());
        pstmt.setString(8, connection.getUsername());
        pstmt.setString(9, connection.getPassword());
        pstmt.setInt(10, connection.getPoolSize());

        if (connection.getProjectId() > 0) {
            pstmt.setInt(11, connection.getProjectId());
        } else {
            pstmt.setNull(11, Types.INTEGER);
        }

        if (connection.getEnvironmentId() > 0) {
            pstmt.setInt(12, connection.getEnvironmentId());
        } else {
            pstmt.setNull(12, Types.INTEGER);
        }

        pstmt.setString(13, connection.getDescription());
    }
} 