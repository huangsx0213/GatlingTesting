package com.qa.app.service.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting dynamic variables from strings, e.g., @{varName} from URLs.
 */
public class VariableUtil {

    private static final Pattern DYNAMIC_VAR_PATTERN = Pattern.compile("@\\{([^}]+)\\}");

    /**
     * Extracts the set of dynamic variable names from the given text.
     * For example, from "/user/@{id}/@{page}" returns Set["id", "page"].
     * @param text the input string (e.g., URL)
     * @return set of unique variable names
     */
    public static Set<String> extractDynamicVars(String text) {
        Set<String> vars = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return vars;
        }
        Matcher matcher = DYNAMIC_VAR_PATTERN.matcher(text);
        while (matcher.find()) {
            vars.add(matcher.group(1));
        }
        return vars;
    }
} 