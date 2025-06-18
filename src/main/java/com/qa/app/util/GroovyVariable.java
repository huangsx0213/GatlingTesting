package com.qa.app.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GroovyVariable {

    private final String name;
    private final String format;
    private final String description;
    private final String groovyScript;
    private final Pattern pattern;
    private final int expectedArgCount;

    @JsonCreator
    public GroovyVariable(
            @JsonProperty("name") String name,
            @JsonProperty("format") String format,
            @JsonProperty("description") String description,
            @JsonProperty("groovyScript") String groovyScript) {
        this.name = name;
        this.format = format;
        this.description = description;
        this.groovyScript = groovyScript;

        long count = 0;
        if (format.contains("(") && format.contains(")")) {
            String argsPart = format.substring(format.indexOf('(') + 1, format.lastIndexOf(')'));
            if (!argsPart.trim().isEmpty()) {
                count = Arrays.stream(argsPart.split(",")).filter(s -> !s.trim().isEmpty()).count();
            }
        }
        this.expectedArgCount = (int) count;
        this.pattern = buildPatternFromFormat(format, this.expectedArgCount);
    }

    private Pattern buildPatternFromFormat(String formatStr, int argCount) {
        // Extract the variable name, e.g., "userGreeting" from "@{userGreeting(name)}"
        String varName;
        int openBrace = formatStr.indexOf('{');
        int openParen = formatStr.indexOf('(');
        int closeBrace = formatStr.lastIndexOf('}');

        if (openParen != -1 && openParen < closeBrace) {
            varName = formatStr.substring(openBrace + 1, openParen);
        } else {
            varName = formatStr.substring(openBrace + 1, closeBrace);
        }
        
        // Start building the regex: @{varName ... }
        String regex = "@\\{" + Pattern.quote(varName);

        if (argCount > 0) {
            // Add regex for arguments: (.*?), (.*?,.*?) etc.
            String paramsRegex = IntStream.range(0, argCount)
                                          .mapToObj(i -> "(.*?)")
                                          .collect(Collectors.joining("\\s*,\\s*"));
            regex += "\\s*\\(\\s*" + paramsRegex + "\\s*\\)";
        }

        // Close with the final brace
        regex += "\\}";

        return Pattern.compile(regex);
    }

    public String generate(Object... args) {
        if (args.length != this.expectedArgCount) {
            String errorMessage = String.format(
                "Error for variable '%s': Expected %d argument(s) but received %d. Usage: %s",
                this.name, this.expectedArgCount, args.length, this.description
            );
            System.err.println(errorMessage);
            return this.format;
        }

        try {
            Binding binding = new Binding();
            binding.setVariable("args", args);
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(groovyScript);
            return String.valueOf(result);
        } catch (Exception e) {
            System.err.println("Error executing Groovy script for variable '" + name + "': " + e.getMessage());
            e.printStackTrace();
            return format;
        }
    }

    public Matcher getMatcher(String input) {
        return this.pattern.matcher(input);
    }
    
    public Pattern getPattern() {
        return pattern;
    }

    public String getFormat() {
        return format;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }
    
    public String getGroovyScript() {
        return groovyScript;
    }
} 