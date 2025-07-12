package com.qa.app.util;

import com.qa.app.common.listeners.AppConfigChangeListener;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AppConfig {
    private static final Properties props = new Properties();
    private static final String PROPERTIES_FILE_PATH = "application.properties";
    private static final List<AppConfigChangeListener> listeners = new ArrayList<>();

    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        try (InputStream in = new FileInputStream(PROPERTIES_FILE_PATH)) {
            props.load(in);
        } catch (FileNotFoundException e) {
            System.err.println("application.properties not found, starting with empty configuration.");
        } catch (IOException e) {
            System.err.println("Failed to load application.properties: " + e.getMessage());
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

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public static void removeProperty(String key) {
        props.remove(key);
    }

    public static void reload() {
        props.clear();
        loadProperties();
        notifyListeners();
    }

    public static Properties getProperties() {
        return (Properties) props.clone();
    }

    public static void saveProperties(Properties newProps) {
        props.clear();
        props.putAll(newProps);
        saveProperties();
    }

    public static void saveProperties() {
        try (OutputStream output = new FileOutputStream(PROPERTIES_FILE_PATH)) {
            props.store(output, "Updated from application");
            notifyListeners(); // Notify after saving
        } catch (IOException e) {
            System.err.println("Failed to save application.properties: " + e.getMessage());
            // In a real app, you might want a more robust error handling mechanism
            // that doesn't directly involve UI components.
        }
    }
} 