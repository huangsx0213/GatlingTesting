package com.qa.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import com.qa.app.model.Project;
import com.qa.app.dao.impl.ProjectDaoImpl;

public class AppConfig {
    private static final Properties props = new Properties();
    private static Project currentProject;

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Failed to load application.properties: " + e.getMessage());
        }
        // auto set currentProject by current.project.name
        String projectName = props.getProperty("current.project.name");
        if (projectName != null && !projectName.isEmpty()) {
            try {
                ProjectDaoImpl projectDao = new ProjectDaoImpl();
                Project project = projectDao.getProjectByName(projectName);
                if (project != null) {
                    currentProject = project;
                } else {
                    System.err.println("Project with name '" + projectName + "' not found in database.");
                }
            } catch (Exception e) {
                System.err.println("Failed to set currentProject by name: " + e.getMessage());
            }
        }
    }

    public static String getCurrentEnv() {
        return props.getProperty("current.env", "dev"); // default dev
    }

    public static Integer getCurrentProjectId() {
        return currentProject != null ? currentProject.getId() : null;
    }

    public static void setCurrentProject(Project project) {
        currentProject = project;
    }

    public static void reload() {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.clear();
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Failed to reload application.properties: " + e.getMessage());
        }
        // reset currentProject
        String projectName = props.getProperty("current.project.name");
        if (projectName != null && !projectName.isEmpty()) {
            try {
                ProjectDaoImpl projectDao = new ProjectDaoImpl();
                Project project = projectDao.getProjectByName(projectName);
                if (project != null) {
                    currentProject = project;
                } else {
                    currentProject = null;
                    // status bar red hint
                    com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Project '" + projectName + "' not found in database.", com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
                }
            } catch (Exception e) {
                currentProject = null;
                com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Failed to set currentProject: " + e.getMessage(), com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
            }
        }
    }
} 