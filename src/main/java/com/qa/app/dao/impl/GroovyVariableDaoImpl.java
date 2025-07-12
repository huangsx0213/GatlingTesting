package com.qa.app.dao.impl;

import com.qa.app.dao.api.IGroovyVariableDao;
import com.qa.app.dao.util.DBUtil;
import com.qa.app.model.GroovyVariable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroovyVariableDaoImpl implements IGroovyVariableDao {

    @Override
    public void add(GroovyVariable variable) throws SQLException {
        String sql = "INSERT INTO groovy_variables (name, value, description, environment_id, project_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, variable.getName());
            pstmt.setString(2, variable.getValue());
            pstmt.setString(3, variable.getDescription());
            if (variable.getEnvironmentId() == null) {
                pstmt.setNull(4, Types.INTEGER);
            } else {
                pstmt.setInt(4, variable.getEnvironmentId());
            }
            if (variable.getProjectId() != null) {
                pstmt.setInt(5, variable.getProjectId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    variable.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(GroovyVariable variable) throws SQLException {
        String sql = "UPDATE groovy_variables SET name = ?, value = ?, description = ?, environment_id = ?, project_id = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, variable.getName());
            pstmt.setString(2, variable.getValue());
            pstmt.setString(3, variable.getDescription());
            if (variable.getEnvironmentId() == null) {
                pstmt.setNull(4, Types.INTEGER);
            } else {
                pstmt.setInt(4, variable.getEnvironmentId());
            }
            if (variable.getProjectId() != null) {
                pstmt.setInt(5, variable.getProjectId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            pstmt.setInt(6, variable.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM groovy_variables WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM groovy_variables";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    @Override
    public GroovyVariable getById(int id) throws SQLException {
        String sql = "SELECT * FROM groovy_variables WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createFromResultSet(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<GroovyVariable> getByProjectId(Integer projectId) throws SQLException {
        String sql = "SELECT * FROM groovy_variables WHERE project_id = ? ORDER BY id";
        List<GroovyVariable> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, projectId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(createFromResultSet(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<GroovyVariable> getAll() throws SQLException {
        String sql = "SELECT * FROM groovy_variables ORDER BY id";
        List<GroovyVariable> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(createFromResultSet(rs));
            }
        }
        return list;
    }

    private GroovyVariable createFromResultSet(ResultSet rs) throws SQLException {
        return new GroovyVariable(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("value"),
                rs.getString("description"),
                rs.getObject("environment_id") == null ? null : rs.getInt("environment_id"),
                rs.getObject("project_id") == null ? null : rs.getInt("project_id")
        );
    }
} 