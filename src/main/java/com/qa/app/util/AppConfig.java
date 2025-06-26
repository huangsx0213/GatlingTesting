package com.qa.app.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;
import com.qa.app.model.Project;
import com.qa.app.dao.impl.ProjectDaoImpl;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import com.qa.app.common.listeners.AppConfigChangeListener;

public class AppConfig {
    private static final Properties props = new Properties();
    private static Project currentProject;
    private static final String PROPERTIES_FILE_PATH = "application.properties";
    private static final List<AppConfigChangeListener> listeners = new ArrayList<>();

    static {
        try (InputStream in = new FileInputStream(PROPERTIES_FILE_PATH)) {
            props.load(in);
        } catch (FileNotFoundException e) {
            // If the file doesn't exist, we can just continue with empty properties.
            // The file will be created on first save.
            System.err.println("application.properties not found, starting with empty configuration.");
        } catch (IOException e) {
            System.err.println("Failed to load application.properties: " + e.getMessage());
        }
        // auto set currentProject by current.project.name
        String projectName = props.getProperty("current.project.name");
        if (projectName != null && !projectName.isEmpty()) {
            try {
                ProjectDaoImpl projectDao = new ProjectDaoImpl();
                Project project = projectDao.getProjectByName(projectName.trim());
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

    public static void addChangeListener(AppConfigChangeListener listener) {
        listeners.add(listener);
    }

    public static void removeChangeListener(AppConfigChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (AppConfigChangeListener listener : listeners) {
            listener.onConfigChanged();
        }
    }

    public static String getCurrentEnv() {
        return props.getProperty("current.env", "dev"); // default dev
    }

    public static Integer getCurrentProjectId() {
        return currentProject != null ? currentProject.getId() : null;
    }

    public static Project getCurrentProject() {
        return currentProject;
    }

    public static void setCurrentProject(Project project) {
        currentProject = project;
    }

    public static void reload() {
        try (InputStream in = new FileInputStream(PROPERTIES_FILE_PATH)) {
            props.clear();
            props.load(in);
        } catch (FileNotFoundException e) {
            System.err.println("application.properties not found, cannot reload.");
        } catch (IOException e) {
            System.err.println("Failed to reload application.properties: " + e.getMessage());
        }
        // reset currentProject
        String projectName = props.getProperty("current.project.name");
        if (projectName != null && !projectName.isEmpty()) {
            try {
                ProjectDaoImpl projectDao = new ProjectDaoImpl();
                Project project = projectDao.getProjectByName(projectName.trim());
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
        notifyListeners();
    }

    public static Properties getProperties() {
        return props;
    }

    public static void saveProperties(Properties newProps) {
        props.clear();
        props.putAll(newProps);
        try (OutputStream output = new FileOutputStream(PROPERTIES_FILE_PATH)) {
            props.store(output, "Updated from application");
            reload();
        } catch (IOException e) {
            System.err.println("Failed to save application.properties: " + e.getMessage());
            com.qa.app.ui.vm.MainViewModel.showGlobalStatus("Failed to save properties: " + e.getMessage(), com.qa.app.ui.vm.MainViewModel.StatusType.ERROR);
        }
    }
} 