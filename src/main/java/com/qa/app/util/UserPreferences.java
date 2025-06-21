package com.qa.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.threadgroups.SteppingThreadGroup;
import com.qa.app.model.threadgroups.StandardThreadGroup;
import com.qa.app.model.threadgroups.UltimateThreadGroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple JSON file for persisting user preferences, storing the last entered load configuration,
 * to auto-fill the dialog on next open.
 */
public class UserPreferences {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PREF_FILE_NAME = "gatling_load_prefs.json";
    private static final File PREF_FILE = new File(PREF_FILE_NAME);


    public StandardThreadGroup standard = new StandardThreadGroup();
    public SteppingThreadGroup stepping = new SteppingThreadGroup();
    public UltimateThreadGroup ultimate = new UltimateThreadGroup();

    public static UserPreferences load() {
        try {
            if (PREF_FILE.exists()) {
                return MAPPER.readValue(PREF_FILE, UserPreferences.class);
            }
            // Added: First initialize defaults from properties file
            UserPreferences defaults = new UserPreferences();
            loadDefaultsFromProperties(defaults);
            // Continue reading default JSON file from classpath
            java.io.InputStream in = UserPreferences.class.getClassLoader().getResourceAsStream("gatling_load_prefs.json");
            if (in != null) {
                defaults = MAPPER.readValue(in, UserPreferences.class);
                defaults.save();
            }
            return defaults;
        } catch (IOException ignored) { }
        return new UserPreferences();
    }

    private static void loadDefaultsFromProperties(UserPreferences prefs) {
        try (InputStream in = UserPreferences.class.getClassLoader().getResourceAsStream("gatling.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                // standard
                if (props.getProperty("standard.users") != null) {
                    prefs.standard.setNumThreads(Integer.parseInt(props.getProperty("standard.users")));
                }
                if (props.getProperty("standard.rampUp") != null) {
                    prefs.standard.setRampUp(Integer.parseInt(props.getProperty("standard.rampUp")));
                }
                if (props.getProperty("standard.duration") != null) {
                    prefs.standard.setDuration(Integer.parseInt(props.getProperty("standard.duration")));
                }
                // stepping
                if (props.getProperty("stepping.users") != null) {
                    prefs.stepping.setNumThreads(Integer.parseInt(props.getProperty("stepping.users")));
                }
                if (props.getProperty("stepping.rampUp") != null) {
                    prefs.stepping.setIncrementTime(Integer.parseInt(props.getProperty("stepping.rampUp")));
                }
                if (props.getProperty("stepping.duration") != null) {
                    prefs.stepping.setHoldLoad(Integer.parseInt(props.getProperty("stepping.duration")));
                }
                // ultimate (just a simple example, extend as needed in practice)
                // For example, ultimate.step.0.startTime=0, ultimate.step.0.initialLoad=1 ...
                // Here, only a simple single-step configuration is implemented
                if (prefs.ultimate != null && props.getProperty("ultimate.step.0.startTime") != null) {
                    com.qa.app.model.threadgroups.UltimateThreadGroupStep step = new com.qa.app.model.threadgroups.UltimateThreadGroupStep();
                    step.setStartTime(Integer.parseInt(props.getProperty("ultimate.step.0.startTime")));
                    if (props.getProperty("ultimate.step.0.initialLoad") != null)
                        step.setInitialLoad(Integer.parseInt(props.getProperty("ultimate.step.0.initialLoad")));
                    if (props.getProperty("ultimate.step.0.startupTime") != null)
                        step.setStartupTime(Integer.parseInt(props.getProperty("ultimate.step.0.startupTime")));
                    if (props.getProperty("ultimate.step.0.holdTime") != null)
                        step.setHoldTime(Integer.parseInt(props.getProperty("ultimate.step.0.holdTime")));
                    if (props.getProperty("ultimate.step.0.shutdownTime") != null)
                        step.setShutdownTime(Integer.parseInt(props.getProperty("ultimate.step.0.shutdownTime")));
                    prefs.ultimate.getSteps().clear();
                    prefs.ultimate.getSteps().add(step);
                }
            }
        } catch (Exception ignored) { }
    }

    public void save() {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(PREF_FILE, this);
        } catch (IOException e) {
            System.err.println("Failed to save user preferences: " + e.getMessage());
        }
    }
} 