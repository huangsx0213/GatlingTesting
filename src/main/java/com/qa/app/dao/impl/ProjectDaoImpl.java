package com.qa.app.dao.impl;

import com.qa.app.dao.api.IProjectDao;
import com.qa.app.model.Project;
import com.qa.app.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDaoImpl implements IProjectDao {
    @Override
    public void addProject(Project project) {
        String sql = "INSERT INTO project (name, description) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add project", e);
        }
    }

    @Override
    public void updateProject(Project project) {
        String sql = "UPDATE project SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setInt(3, project.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update project", e);
        }
    }

    @Override
    public void deleteProject(int id) {
        String sql = "DELETE FROM project WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete project", e);
        }
    }

    @Override
    public Project getProjectById(int id) {
        String sql = "SELECT * FROM project WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Project(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get project by id", e);
        }
        return null;
    }

    @Override
    public List<Project> getAllProjects() {
        String sql = "SELECT * FROM project ORDER BY id DESC";
        List<Project> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Project(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all projects", e);
        }
        return list;
    }

    @Override
    public Project getProjectByName(String name) {
        String sql = "SELECT * FROM project WHERE name = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Project(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get project by name", e);
        }
        return null;
    }
} 