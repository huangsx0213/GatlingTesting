package com.qa.app.dao.impl;

import com.qa.app.dao.api.IEnvironmentDao;
import com.qa.app.dao.util.DBUtil;
import com.qa.app.model.Environment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnvironmentDaoImpl implements IEnvironmentDao {
    @Override
    public void addEnvironment(Environment environment) throws SQLException {
        String sql = "INSERT INTO environments (name, description, project_id) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, environment.getName());
            pstmt.setString(2, environment.getDescription());
            if (environment.getProjectId() != null) {
                pstmt.setInt(3, environment.getProjectId());
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    environment.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateEnvironment(Environment environment) throws SQLException {
        String sql = "UPDATE environments SET name = ?, description = ?, project_id = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, environment.getName());
            pstmt.setString(2, environment.getDescription());
            if (environment.getProjectId() != null) {
                pstmt.setInt(3, environment.getProjectId());
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }
            pstmt.setInt(4, environment.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteEnvironment(int id) throws SQLException {
        String sql = "DELETE FROM environments WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public Environment getEnvironmentById(int id) throws SQLException {
        String sql = "SELECT * FROM environments WHERE id = ?";
        Environment environment = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    environment = createEnvironmentFromResultSet(rs);
                }
            }
        }
        return environment;
    }

    @Override
    public Environment getEnvironmentByName(String name) throws SQLException {
        String sql = "SELECT * FROM environments WHERE name = ?";
        Environment environment = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    environment = createEnvironmentFromResultSet(rs);
                }
            }
        }
        return environment;
    }

    @Override
    public List<Environment> getAllEnvironments() throws SQLException {
        String sql = "SELECT * FROM environments ORDER BY name";
        List<Environment> environments = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                environments.add(createEnvironmentFromResultSet(rs));
            }
        }
        return environments;
    }

    public List<Environment> getEnvironmentsByProjectId(Integer projectId) throws SQLException {
        String sql = "SELECT * FROM environments WHERE project_id = ? ORDER BY name";
        List<Environment> environments = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    environments.add(createEnvironmentFromResultSet(rs));
                }
            }
        }
        return environments;
    }

    private Environment createEnvironmentFromResultSet(ResultSet rs) throws SQLException {
        Environment environment = new Environment();
        environment.setId(rs.getInt("id"));
        environment.setName(rs.getString("name"));
        environment.setDescription(rs.getString("description"));
        Object pid = rs.getObject("project_id");
        if (pid != null) {
            environment.setProjectId((Integer)pid);
        }
        return environment;
    }
} 