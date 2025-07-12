package com.qa.app.service.runner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test run context for managing variables during test execution.
 * Variables are stored in the format "TCID.variableName" and can be referenced using ${TCID.variableName} in subsequent tests.
 */
public class TestRunContext {
    // Map to store all test variables, key is "TCID.variableName", value is the variable value
    private static final Map<String, String> testVariables = new ConcurrentHashMap<>();
    
    // Regular expression to match ${TCID.variableName} format
    private static final Pattern VARIABLE_REFERENCE_PATTERN = Pattern.compile("\\$\\{([\\w.-]+)\\.([\\w.-]+)\\}");
    
    // Stores reference values for DIFF checks
    private static final Map<String, Double> diffBeforeValues = new ConcurrentHashMap<>();
    private static final Map<String, Double> diffAfterValues  = new ConcurrentHashMap<>();
    
    // Stores reference values for PRE_CHECK and PST_CHECK
    private static final Map<String, String> preCheckValues = new ConcurrentHashMap<>();
    private static final Map<String, String> pstCheckValues = new ConcurrentHashMap<>();
    
    /**
     * Clear all test variables
     */
    public static void clear() {
        testVariables.clear();
        diffBeforeValues.clear();
        diffAfterValues.clear();
        preCheckValues.clear();
        pstCheckValues.clear();
    }
    
    /**
     * Save test variable
     * @param tcid Test case ID
     * @param variableName Variable name
     * @param value Variable value
     */
    public static void saveVariable(String tcid, String variableName, String value) {
        if (tcid == null || tcid.isBlank() || variableName == null || variableName.isBlank()) {
            return;
        }
        String key = tcid + "." + variableName;
        testVariables.put(key, value);
        System.out.println("VARIABLE_SAVED|" + key + "|" + value);
    }
    
    /**
     * Get test variable
     * @param key Full variable key in the format "TCID.variableName"
     * @return Variable value, or null if not found
     */
    public static String getVariable(String key) {
        return testVariables.get(key);
    }
    
    /**
     * Process variable references in a string, replacing ${TCID.variableName} with the actual variable value
     * @param input Input string
     * @return String with variable references replaced
     */
    public static String processVariableReferences(String input) {
        if (input == null || input.isEmpty() || !input.contains("${")) {
            return input;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_REFERENCE_PATTERN.matcher(input);
        
        while (matcher.find()) {
            String fullKey = matcher.group(1) + "." + matcher.group(2);
            String value = testVariables.getOrDefault(fullKey, "${" + fullKey + " - NOT FOUND}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Get all test variables
     * @return Copy of the variable Map
     */
    public static Map<String, String> getAllVariables() {
        return new ConcurrentHashMap<>(testVariables);
    }
    
    /**
     * Save the "before" value for a DIFF check.
     * @param refKey logical key identifying the reference value (typically refTCID.path)
     * @param value  numeric value captured before operation
     */
    public static void saveBefore(String refKey, String value) {
        if (refKey == null || refKey.isBlank()) return;
        try {
            diffBeforeValues.put(refKey, Double.parseDouble(value));
        } catch (NumberFormatException ignored) {
            // Non-numeric value – skip storing to avoid NumberFormatException later
        }
    }
    
    /**
     * Save the "after" value for a DIFF check.
     * @param refKey logical key identifying the reference value (typically refTCID.path)
     * @param value  numeric value captured after operation
     */
    public static void saveAfter(String refKey, String value) {
        if (refKey == null || refKey.isBlank()) return;
        try {
            diffAfterValues.put(refKey, Double.parseDouble(value));
        } catch (NumberFormatException ignored) {
            // Non-numeric value – skip storing
        }
    }
    
    /**
     * Calculate difference (after - before) for a given reference key.
     * @param refKey reference key
     * @return calculated difference, or null if missing either value
     */
    public static Double calcDiff(String refKey) {
        if (!diffBeforeValues.containsKey(refKey) || !diffAfterValues.containsKey(refKey)) {
            return null;
        }
        return diffAfterValues.get(refKey) - diffBeforeValues.get(refKey);
    }
    
    /**
     * Save a PRE_CHECK value from a reference API.
     * @param refKey logical key identifying the reference value (typically refTCID.path)
     * @param value value from reference API (can be any string value)
     */
    public static void savePreCheck(String refKey, String value) {
        if (refKey == null || refKey.isBlank() || value == null) return;
        preCheckValues.put(refKey, value);
    }
    
    /**
     * Save a PST_CHECK value from a reference API.
     * @param refKey logical key identifying the reference value (typically refTCID.path)
     * @param value value from reference API (can be any string value)
     */
    public static void savePstCheck(String refKey, String value) {
        if (refKey == null || refKey.isBlank() || value == null) return;
        pstCheckValues.put(refKey, value);
    }
    
    /**
     * Get a PRE_CHECK value for evaluation.
     * @param refKey reference key
     * @return stored value, or null if not found
     */
    public static String getPreCheckValue(String refKey) {
        return preCheckValues.get(refKey);
    }
    
    /**
     * Get a PST_CHECK value for evaluation.
     * @param refKey reference key
     * @return stored value, or null if not found
     */
    public static String getPstCheckValue(String refKey) {
        return pstCheckValues.get(refKey);
    }
} 