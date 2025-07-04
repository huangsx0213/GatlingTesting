package com.qa.app.service.script;

import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IVariableService;
import com.qa.app.service.impl.VariableServiceImpl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableGenerator {

    private static final VariableGenerator INSTANCE = new VariableGenerator();

    private List<GroovyScriptEngine> customVariables;
    private final IVariableService variableService;

    private VariableGenerator() {
        this.variableService = new VariableServiceImpl();
        reloadCustomVariables();
    }

    public static VariableGenerator getInstance() {
        return INSTANCE;
    }

    public void reloadCustomVariables() {
        try {
            customVariables = variableService.loadVariables();
        } catch (ServiceException e) {
            System.err.println("Failed to load custom variables: " + e.getMessage());
            customVariables = new ArrayList<>();
        }
    }

    public List<Map<String, String>> getVariableDefinitions() {
        List<Map<String, String>> definitions = new ArrayList<>();
        // Add built-in rules
        definitions.add(Map.of("format", "__UUID", "description", "Generates a random UUID"));
        definitions.add(Map.of("format", "__TIMESTAMP", "description", "Generates the current Unix timestamp (seconds)"));
        definitions.add(Map.of("format", "__DATETIME(format)", "description", "Generates date/time, e.g., __DATETIME(yyyy-MM-dd HH:mm:ss)"));
        definitions.add(Map.of("format", "__RANDOM(min,max)", "description", "Generates a random integer, e.g., __RANDOM(1,100)"));

        // Add custom rules
        if (customVariables != null) {
            for (GroovyScriptEngine variable : customVariables) {
                definitions.add(Map.of("format", variable.getFormat(), "description", variable.getDescription()));
            }
        }
        return definitions;
    }

    public String resolveVariables(String value) {
        if (value == null) {
            return null;
        }
        String resolvedValue = value;

        // 优先处理自定义变量
        if (customVariables != null) {
            for (GroovyScriptEngine variable : customVariables) {
                Matcher matcher = variable.getMatcher(resolvedValue);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    int groupCount = matcher.groupCount();
                    String[] args = new String[groupCount];
                    for (int i = 0; i < groupCount; i++) {
                        args[i] = matcher.group(i + 1);
                    }
                    String replacement = variable.generate((Object[]) args);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
                matcher.appendTail(sb);
                resolvedValue = sb.toString();
            }
        }

        // 处理内置变量
        resolvedValue = resolveBuiltInVariables(resolvedValue);

        return resolvedValue;
    }

    private String resolveBuiltInVariables(String value) {
        String translatedValue = translateLegacySyntax(value);
        String resolvedValue = translatedValue;

        if (resolvedValue.contains("__UUID")) {
            resolvedValue = resolvedValue.replace("__UUID", UUID.randomUUID().toString());
        }
        if (resolvedValue.contains("__TIMESTAMP")) {
            resolvedValue = resolvedValue.replace("__TIMESTAMP", String.valueOf(Instant.now().getEpochSecond()));
        }

        Pattern datetimePattern = Pattern.compile("__DATETIME\\((.*?)\\)");
        Matcher datetimeMatcher = datetimePattern.matcher(resolvedValue);
        StringBuffer sbDatetime = new StringBuffer();
        while (datetimeMatcher.find()) {
            String format = datetimeMatcher.group(1);
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
                String formattedDate = LocalDateTime.now().format(dtf);
                datetimeMatcher.appendReplacement(sbDatetime, Matcher.quoteReplacement(formattedDate));
            } catch (Exception e) {
                System.err.println("Error formatting date: " + e.getMessage());
            }
        }
        datetimeMatcher.appendTail(sbDatetime);
        resolvedValue = sbDatetime.toString();

        Pattern randomPattern = Pattern.compile("__RANDOM\\((\\d+),(\\d+)\\)");
        Matcher randomMatcher = randomPattern.matcher(resolvedValue);
        StringBuffer sbRandom = new StringBuffer();
        while (randomMatcher.find()) {
            int min = Integer.parseInt(randomMatcher.group(1));
            int max = Integer.parseInt(randomMatcher.group(2));
            int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
            randomMatcher.appendReplacement(sbRandom, String.valueOf(randomNum));
        }
        randomMatcher.appendTail(sbRandom);
        resolvedValue = sbRandom.toString();

        return resolvedValue;
    }

    private String translateLegacySyntax(String value) {
        if (value == null) {
            return null;
        }
        
        Pattern pattern = Pattern.compile("@\\{([a-zA-Z]+)(?:\\((.*?)\\))?\\}");
        
        return pattern.matcher(value).replaceAll(matchResult -> {
            String functionName = matchResult.group(1).toLowerCase();
            String args = matchResult.groupCount() > 1 ? matchResult.group(2) : null;
            
            return switch (functionName) {
                case "randomuuid" -> "__UUID";
                case "timestamp" -> "__TIMESTAMP";
                case "randomint" -> "__RANDOM(" + (args != null ? args : "") + ")";
                case "datetime" -> "__DATETIME(" + (args != null ? args : "") + ")";
                default -> matchResult.group(0); // No change if function is not recognized
            };
        });
    }

    public void setCustomVariables(List<GroovyScriptEngine> customVariables) {
        this.customVariables = customVariables;
    }
}
