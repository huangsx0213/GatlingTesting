package com.qa.app.service;

import com.qa.app.common.listeners.AppConfigChangeListener;
import com.qa.app.model.Environment;
import com.qa.app.service.api.IEnvironmentService;
import com.qa.app.service.impl.EnvironmentServiceImpl;
import com.qa.app.util.AppConfig;

/**
 * Holds the active environment resolved from application.properties (key: current.env).
 * Works similarly to {@link ProjectContext}.  Whenever application properties are reloaded
 * the cached environment info is refreshed automatically.
 */
public class EnvironmentContext implements AppConfigChangeListener {

    private static volatile Integer currentEnvironmentId;
    private static volatile String currentEnvironmentName;

    private static final IEnvironmentService envService = new EnvironmentServiceImpl();

    static {
        // Initial load
        refresh();
        // Subscribe to changes so we always reflect the latest "current.env" value.
        AppConfig.addChangeListener(new EnvironmentContext());
    }

    private static void refresh() {
        currentEnvironmentName = AppConfig.getProperty("current.env", null);
        if (currentEnvironmentName == null) {
            currentEnvironmentId = null;
            return;
        }
        try {
            for (Environment env : envService.findAllEnvironments()) {
                if (currentEnvironmentName.equals(env.getName())) {
                    currentEnvironmentId = env.getId();
                    return;
                }
            }
            // Not found – leave id null so callers can handle gracefully
            currentEnvironmentId = null;
        } catch (Exception e) {
            currentEnvironmentId = null;
        }
    }

    @Override
    public void onConfigChanged() {
        refresh();
    }

    public static Integer getCurrentEnvironmentId() {
        return currentEnvironmentId;
    }

    public static String getCurrentEnvironmentName() {
        return currentEnvironmentName;
    }
} 