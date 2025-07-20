package com.qa.app.service.runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for common Gatling runner operations.
 */
public class GatlingRunnerUtils {

    /**
     * Assembles the classpath for Gatling execution.
     * Prefers Maven-provided classpath file if available, otherwise falls back to system classpath.
     *
     * @return the assembled classpath string
     */
    public static String assembleClasspath() {
        // Prefer classpath provided by Maven build plugin (read from a file to avoid command line length limits)
        String classpathFile = System.getProperty("gatling.classpath.file");
        if (classpathFile != null) {
            try {
                String dependencyClasspath = new String(Files.readAllBytes(Paths.get(classpathFile)));
                String projectClassesPath = Paths.get(System.getProperty("user.dir"), "target", "classes").toString();
                // Prepend the project's own classes to the classpath
                return projectClassesPath + File.pathSeparator + dependencyClasspath;
            } catch (IOException e) {
                System.err.println("WARN: Failed to read classpath from " + classpathFile + ", falling back to default.");
            }
        }

        // Fallback to original method for other environments (e.g., IDE, fat JAR)
        String cp = System.getProperty("java.class.path");
        try {
            String selfPath = new File(GatlingRunnerUtils.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getPath();
            if (!cp.contains(selfPath)) {
                cp += File.pathSeparator + selfPath;
            }
        } catch (Exception ignored) { }
        return cp;
    }

    /**
     * Builds the command list to run Gatling with the specified simulation class and system properties.
     *
     * @param simulationClass the fully qualified name of the simulation class
     * @param sysProps        map of system properties to set (e.g., "gatling.tests.file" -> path)
     * @param resultsPath     the path for Gatling results
     * @return the list of command arguments
     * @throws Exception if logback configuration fails
     */
    public static List<String> buildGatlingCommand(String simulationClass, Map<String, String> sysProps, String resultsPath) throws Exception {
        String javaHome = System.getProperty("java.home");
        String javaBin = Paths.get(javaHome, "bin", "java").toString();
        String classpath = assembleClasspath();
        String gatlingMain = "io.gatling.app.Gatling";

        URL logbackUrl = GatlingRunnerUtils.class.getClassLoader().getResource("logback.xml");
        String logbackPath = null;
        if (logbackUrl != null) {
            if ("jar".equals(logbackUrl.getProtocol())) {
                // Extract logback.xml from JAR to a temporary file
                try (InputStream inputStream = logbackUrl.openStream()) {
                    File tempFile = File.createTempFile("logback-", ".xml");
                    tempFile.deleteOnExit();
                    Files.copy(inputStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    logbackPath = tempFile.getAbsolutePath();
                }
            } else {
                // Running from file system (e.g., in IDE)
                logbackPath = new File(logbackUrl.toURI()).getAbsolutePath();
            }
        }

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("--add-opens");
        command.add("java.base/java.lang=ALL-UNNAMED");
        command.add("-cp");
        command.add(classpath);
        if (logbackPath != null) {
            command.add("-Dlogback.configurationFile=" + logbackPath);
        }
        for (Map.Entry<String, String> entry : sysProps.entrySet()) {
            command.add("-D" + entry.getKey() + "=" + entry.getValue());
        }
        command.add(gatlingMain);
        command.add("-s");
        command.add(simulationClass);
        command.add("-rf");
        command.add(resultsPath);
        return command;
    }
} 