package com.qa.app.service;

import com.qa.app.model.Project;

public class ProjectContext {
    private static Project currentProject;

    public static Project getCurrentProject() {
        return currentProject;
    }

    public static void setCurrentProject(Project project) {
        currentProject = project;
    }

    public static void clearCurrentProject() {
        currentProject = null;
    }

    public static Integer getCurrentProjectId() {
        return currentProject != null ? currentProject.getId() : null;
    }
} 