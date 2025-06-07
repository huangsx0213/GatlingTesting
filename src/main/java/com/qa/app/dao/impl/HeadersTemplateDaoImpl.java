package com.qa.app.dao.impl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.qa.app.dao.api.IHeadersTemplateDao;
import com.qa.app.model.HeadersTemplate;
import com.qa.app.util.DBUtil;

public class HeadersTemplateDaoImpl implements IHeadersTemplateDao {
    @Override
    public void addHeadersTemplate(HeadersTemplate template) throws SQLException {
        String sql = "INSERT INTO headers_templates (name, content, project_id) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, template.getName());
            pstmt.setString(2, template.getContent());
            if (template.getProjectId() != null) {
                pstmt.setInt(3, template.getProjectId());
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    template.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateHeadersTemplate(HeadersTemplate template) throws SQLException {
        String sql = "UPDATE headers_templates SET name = ?, content = ?, project_id = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, template.getName());
            pstmt.setString(2, template.getContent());
            if (template.getProjectId() != null) {
                pstmt.setInt(3, template.getProjectId());
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }
            pstmt.setInt(4, template.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteHeadersTemplate(int id) throws SQLException {
        String sql = "DELETE FROM headers_templates WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public HeadersTemplate getHeadersTemplateById(int id) throws SQLException {
        String sql = "SELECT * FROM headers_templates WHERE id = ?";
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
    public HeadersTemplate getHeadersTemplateByName(String name) throws SQLException {
        String sql = "SELECT * FROM headers_templates WHERE name = ?";
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
    public List<HeadersTemplate> getAllHeadersTemplates() throws SQLException {
        String sql = "SELECT * FROM headers_templates ORDER BY id";
        List<HeadersTemplate> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(createFromResultSet(rs));
            }
        }
        return list;
    }

    @Override
    public List<HeadersTemplate> getHeadersTemplatesByProjectId(Integer projectId) throws SQLException {
        String sql = "SELECT * FROM headers_templates WHERE project_id = ? ORDER BY id";
        List<HeadersTemplate> list = new ArrayList<>();
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

    private HeadersTemplate createFromResultSet(ResultSet rs) throws SQLException {
        return new HeadersTemplate(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("content"),
                rs.getObject("project_id") == null ? null : rs.getInt("project_id")
        );
    }
} 