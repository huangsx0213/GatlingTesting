package com.qa.app.util;


import com.qa.app.service.api.IVariableService;
import com.qa.app.service.impl.VariableServiceImpl;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VariableGenerator {

    public enum Rule {
        TIMESTAMP("@{timestamp}", "Current timestamp in milliseconds"),
        ISO_DATETIME("@{iso_datetime}", "Current ISO 8601 datetime"),
        RANDOM_UUID("@{randomUUID}", "A random UUID"),
        RANDOM_INT("@{randomInt(min,max)}", "A random integer in a range (inclusive)"),
        RANDOM_STRING("@{randomString(length)}", "A random alphanumeric string of a given length");

        private final String format;
        private final String description;

        Rule(String format, String description) {
            this.format = format;
            this.description = description;
        }

        public String getFormat() {
            return format;
        }

        public String getDescription() {
            return description;
        }
    }

    private static List<com.qa.app.util.GroovyVariable> CUSTOM_VARIABLES;
    private static final IVariableService variableService = new VariableServiceImpl();

    static {
        loadCustomVariables();
    }

    public static void loadCustomVariables() {
        try {
            CUSTOM_VARIABLES = variableService.loadVariables();
        } catch (Exception e) {
            System.err.println("Error loading custom variables: " + e.getMessage());
            CUSTOM_VARIABLES = new ArrayList<>();
        }
    }

    public static void reloadCustomVariables() {
        loadCustomVariables();
    }

    private static final Pattern RANDOM_INT_PATTERN = Pattern.compile("@\\{randomInt\\((\\d+),(\\d+)\\)\\}");
    private static final Pattern RANDOM_STRING_PATTERN = Pattern.compile("@\\{randomString\\((\\d+)\\)\\}");
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static List<String> getRuleFormats() {
        List<String> formats = Stream.of(Rule.values()).map(Rule::getFormat).collect(Collectors.toList());
        if (CUSTOM_VARIABLES != null) {
            CUSTOM_VARIABLES.forEach(v -> formats.add(v.getFormat()));
        }
        return formats;
    }

    /**
     * Returns a list of all variable definitions (built-in and custom) for UI consumption.
     * Each map contains "format" and "description".
     * @return List of variable definitions.
     */
    public static List<java.util.Map<String, String>> getAllVariableDefinitions() {
        List<java.util.Map<String, String>> definitions = new ArrayList<>();
        // Add built-in rules
        for (Rule rule : Rule.values()) {
            definitions.add(java.util.Map.of("format", rule.getFormat(), "description", rule.getDescription()));
        }
        // Add custom rules
        if (CUSTOM_VARIABLES != null) {
            for (com.qa.app.util.GroovyVariable variable : CUSTOM_VARIABLES) {
                definitions.add(java.util.Map.of("format", variable.getFormat(), "description", variable.getDescription()));
            }
        }
        return definitions;
    }

    public static String generate(String value) {
        if (value == null || !value.contains("@{")) {
            return value;
        }

        // 1. Check for custom Groovy variables first
        if (CUSTOM_VARIABLES != null) {
            for (com.qa.app.util.GroovyVariable variable : CUSTOM_VARIABLES) {
                Matcher matcher = variable.getMatcher(value);
                if (matcher.matches()) {
                    int groupCount = matcher.groupCount();
                    String[] args = new String[groupCount];
                    for (int i = 0; i < groupCount; i++) {
                        args[i] = matcher.group(i + 1).trim();
                    }
                    return variable.generate((Object[]) args);
                }
            }
        }

        // 2. Fallback to built-in rules
        if (Rule.TIMESTAMP.getFormat().equals(value)) {
            return String.valueOf(Instant.now().toEpochMilli());
        }

        if (Rule.ISO_DATETIME.getFormat().equals(value)) {
            return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }

        if (Rule.RANDOM_UUID.getFormat().equals(value)) {
            return UUID.randomUUID().toString();
        }

        Matcher intMatcher = RANDOM_INT_PATTERN.matcher(value);
        if (intMatcher.matches()) {
            int min = Integer.parseInt(intMatcher.group(1));
            int max = Integer.parseInt(intMatcher.group(2));
            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }
            return String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
        }

        Matcher stringMatcher = RANDOM_STRING_PATTERN.matcher(value);
        if (stringMatcher.matches()) {
            int length = Integer.parseInt(stringMatcher.group(1));
            if (length <= 0) return "";
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(ALPHANUMERIC.charAt(ThreadLocalRandom.current().nextInt(ALPHANUMERIC.length())));
            }
            return sb.toString();
        }

        return value; // Return original value if no rule matches
    }
}
