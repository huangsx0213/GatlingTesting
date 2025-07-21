package com.qa.app.dao.impl;

import com.qa.app.dao.api.IVariableTransformMethodDao;
import com.qa.app.dao.util.DBUtil;
import com.qa.app.model.VariableTransformMethod;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation for {@link VariableTransformMethod} persistence.
 */
public class VariableTransformMethodDaoImpl implements IVariableTransformMethodDao {

    @Override
    public void addMethod(VariableTransformMethod method) throws SQLException {
        String sql = "INSERT INTO variable_transform_methods " +
                "(name, description, script, enabled, param_spec, sample_usage, create_time, update_time) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, method.getName());
            ps.setString(2, method.getDescription());
            ps.setString(3, method.getScript());
            ps.setBoolean(4, method.isEnabled());
            ps.setString(5, method.getParamSpec());
            ps.setString(6, method.getSampleUsage());
            ps.setString(7, LocalDateTime.now().toString());
            ps.setString(8, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    method.setId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateMethod(VariableTransformMethod method) throws SQLException {
        String sql = "UPDATE variable_transform_methods SET description=?, script=?, enabled=?, param_spec=?, sample_usage=?, update_time=? WHERE name=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, method.getDescription());
            ps.setString(2, method.getScript());
            ps.setBoolean(3, method.isEnabled());
            ps.setString(4, method.getParamSpec());
            ps.setString(5, method.getSampleUsage());
            ps.setString(6, LocalDateTime.now().toString());
            ps.setString(7, method.getName());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteMethod(int id) throws SQLException {
        String sql = "DELETE FROM variable_transform_methods WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public VariableTransformMethod getMethodById(int id) throws SQLException {
        String sql = "SELECT * FROM variable_transform_methods WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extract(rs);
                }
            }
        }
        return null;
    }

    @Override
    public VariableTransformMethod getMethodByName(String name) throws SQLException {
        String sql = "SELECT * FROM variable_transform_methods WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extract(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<VariableTransformMethod> getAllMethods() throws SQLException {
        String sql = "SELECT * FROM variable_transform_methods ORDER BY name";
        List<VariableTransformMethod> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(extract(rs));
            }
        }
        return list;
    }

    @Override
    public List<VariableTransformMethod> getEnabledMethods() throws SQLException {
        String sql = "SELECT * FROM variable_transform_methods WHERE enabled = 1";
        List<VariableTransformMethod> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(extract(rs));
            }
        }
        return list;
    }

    private VariableTransformMethod extract(ResultSet rs) throws SQLException {
        VariableTransformMethod m = new VariableTransformMethod();
        m.setId(rs.getInt("id"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setScript(rs.getString("script"));
        m.setEnabled(rs.getBoolean("enabled"));
        m.setParamSpec(rs.getString("param_spec"));
        m.setSampleUsage(rs.getString("sample_usage"));
        // For simplicity we keep timestamps as string
        return m;
    }
} 