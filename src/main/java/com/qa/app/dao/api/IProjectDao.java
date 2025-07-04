package com.qa.app.dao.api;

import com.qa.app.model.Project;

import java.sql.SQLException;
import java.util.List;

public interface IProjectDao {
    void addProject(Project project) throws SQLException;
    void updateProject(Project project) throws SQLException;
    void deleteProject(Integer id) throws SQLException;
    Project getProject(Integer id) throws SQLException;
    List<Project> getAllProjects() throws SQLException;
    Project getProjectByName(String name) throws SQLException;
} 