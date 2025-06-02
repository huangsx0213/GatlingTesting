package com.qa.app.dao.impl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.qa.app.dao.api.IEndpointDao;
import com.qa.app.model.Endpoint;
import com.qa.app.util.DBUtil;

public class EndpointDaoImpl implements IEndpointDao {
    @Override
    public void addEndpoint(Endpoint endpoint) throws SQLException {
        String sql = "INSERT INTO endpoints (name, method, url, environment_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, endpoint.getName());
            pstmt.setString(2, endpoint.getMethod());
            pstmt.setString(3, endpoint.getUrl());
            if (endpoint.getEnvironmentId() == null) {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(4, endpoint.getEnvironmentId());
            }
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    endpoint.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateEndpoint(Endpoint endpoint) throws SQLException {
        String sql = "UPDATE endpoints SET name = ?, method = ?, url = ?, environment_id = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, endpoint.getName());
            pstmt.setString(2, endpoint.getMethod());
            pstmt.setString(3, endpoint.getUrl());
            if (endpoint.getEnvironmentId() == null) {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(4, endpoint.getEnvironmentId());
            }
            pstmt.setInt(5, endpoint.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteEndpoint(int id) throws SQLException {
        String sql = "DELETE FROM endpoints WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public Endpoint getEndpointById(int id) throws SQLException {
        String sql = "SELECT * FROM endpoints WHERE id = ?";
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
    public Endpoint getEndpointByName(String name) throws SQLException {
        String sql = "SELECT * FROM endpoints WHERE name = ?";
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
    public List<Endpoint> getAllEndpoints() throws SQLException {
        String sql = "SELECT * FROM endpoints ORDER BY id";
        List<Endpoint> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(createFromResultSet(rs));
            }
        }
        return list;
    }

    private Endpoint createFromResultSet(ResultSet rs) throws SQLException {
        return new Endpoint(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("method"),
                rs.getString("url"),
                rs.getObject("environment_id") == null ? null : rs.getInt("environment_id")
        );
    }
} 