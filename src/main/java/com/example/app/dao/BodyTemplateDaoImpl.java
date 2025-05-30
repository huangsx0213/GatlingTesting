package com.example.app.dao;

import com.example.app.model.BodyTemplate;
import com.example.app.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BodyTemplateDaoImpl implements IBodyTemplateDao {
    @Override
    public void addTemplate(BodyTemplate template) throws SQLException {
        String sql = "INSERT INTO body_templates (name, content) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, template.getName());
            pstmt.setString(2, template.getContent());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    template.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateTemplate(BodyTemplate template) throws SQLException {
        String sql = "UPDATE body_templates SET name = ?, content = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, template.getName());
            pstmt.setString(2, template.getContent());
            pstmt.setInt(3, template.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteTemplate(int id) throws SQLException {
        String sql = "DELETE FROM body_templates WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public BodyTemplate getTemplateById(int id) throws SQLException {
        String sql = "SELECT * FROM body_templates WHERE id = ?";
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
    public BodyTemplate getTemplateByName(String name) throws SQLException {
        String sql = "SELECT * FROM body_templates WHERE name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createFromResultSet(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<BodyTemplate> getAllTemplates() throws SQLException {
        String sql = "SELECT * FROM body_templates ORDER BY id";
        List<BodyTemplate> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(createFromResultSet(rs));
            }
        }
        return list;
    }

    private BodyTemplate createFromResultSet(ResultSet rs) throws SQLException {
        return new BodyTemplate(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("content")
        );
    }
} 