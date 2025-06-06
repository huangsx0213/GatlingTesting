package com.qa.app.service.api;

import com.qa.app.model.Project;
import java.util.List;

public interface IProjectService {
    void addProject(Project project);
    void updateProject(Project project);
    void deleteProject(int id);
    Project getProjectById(int id);
    List<Project> getAllProjects();
} 