package com.qa.app.dao.impl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.qa.app.dao.api.IBodyTemplateDao;
import com.qa.app.model.BodyTemplate;
import com.qa.app.util.DBUtil;

public class BodyTemplateDaoImpl implements IBodyTemplateDao {
    @Override
    public void addBodyTemplate(BodyTemplate template) throws SQLException {
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
    public void updateBodyTemplate(BodyTemplate template) throws SQLException {
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
    public void deleteBodyTemplate(int id) throws SQLException {
        String sql = "DELETE FROM body_templates WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public BodyTemplate getBodyTemplateById(int id) throws SQLException {
        String sql = "SELECT * FROM body_templates WHERE id = ?";
        BodyTemplate template = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    template = createTemplateFromResultSet(rs);
                }
            }
        }
        return template;
    }

    @Override
    public BodyTemplate getBodyTemplateByName(String name) throws SQLException {
        String sql = "SELECT * FROM body_templates WHERE name = ?";
        BodyTemplate template = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    template = createTemplateFromResultSet(rs);
                }
            }
        }
        return template;
    }

    @Override
    public List<BodyTemplate> getAllBodyTemplates() throws SQLException {
        String sql = "SELECT * FROM body_templates ORDER BY name";
        List<BodyTemplate> templates = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                templates.add(createTemplateFromResultSet(rs));
            }
        }
        return templates;
    }

    private BodyTemplate createTemplateFromResultSet(ResultSet rs) throws SQLException {
        BodyTemplate template = new BodyTemplate();
        template.setId(rs.getInt("id"));
        template.setName(rs.getString("name"));
        template.setContent(rs.getString("content"));
        return template;
    }
} 