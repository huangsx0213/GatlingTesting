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

    /**
     * Gets all variable definitions, including built-in and custom variables
     * 
     * Built-in variables usage examples:
     * 
     * 1. UUID: __UUID
     *    Example: "id": "__UUID" → "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
     * 
     * 2. Timestamp: __TIMESTAMP
     *    Example: "time": "__TIMESTAMP" → "time": "1684932156"
     * 
     * 3. Date/Time: __DATETIME(format)
     *    Example: "date": "__DATETIME(yyyy-MM-dd HH:mm:ss)" → "date": "2023-05-24 14:32:56"
     *    Common formats: yyyy-MM-dd, HH:mm:ss, yyyyMMddHHmmss
     * 
     * 4. Prefix + Timestamp: __PREFIX_TIMESTAMP(prefix)
     *    Example: "orderId": "__PREFIX_TIMESTAMP(ORDER_)" → "orderId": "ORDER_1684932156"
     * 
     * 5. Prefix + DateTime: __PREFIX_DATETIME(prefix,format)
     *    Example: "batchId": "__PREFIX_DATETIME(BATCH_,yyyyMMdd)" → "batchId": "BATCH_20230524"
     * 
     * 6. Random String: __RANDOM_STRING(length,mode)
     *    Modes available:
     *    - a: Alphanumeric (default) - uppercase + lowercase + numbers
     *      Example: "key": "__RANDOM_STRING(8,a)" → "key": "A3bX71pQ"
     *    - u: Uppercase only - only uppercase letters
     *      Example: "code": "__RANDOM_STRING(5,u)" → "code": "AXFGH"
     *    - l: Lowercase only - only lowercase letters
     *      Example: "username": "__RANDOM_STRING(6,l)" → "username": "abcdef"
     *    - m: Mixed case - uppercase + lowercase (no numbers)
     *      Example: "name": "__RANDOM_STRING(7,m)" → "name": "AbCdEfG"
     *    - n: Numeric only - only digits 0-9
     *      Example: "pin": "__RANDOM_STRING(4,n)" → "pin": "1234"
     */
    public List<Map<String, String>> getVariableDefinitions() {
        List<Map<String, String>> definitions = new ArrayList<>();
        // Add built-in rules
        definitions.add(Map.of("format", "__UUID", "description", "Generates a random UUID"));
        definitions.add(Map.of("format", "__TIMESTAMP", "description", "Generates the current Unix timestamp (seconds)"));
        definitions.add(Map.of("format", "__DATETIME(format)", "description", "Generates date/time, e.g., __DATETIME(yyyy-MM-dd HH:mm:ss)"));
        definitions.add(Map.of("format", "__PREFIX_TIMESTAMP(prefix)", "description", "Generates a string with prefix and timestamp, e.g., __PREFIX_TIMESTAMP(test_)"));
        definitions.add(Map.of("format", "__PREFIX_DATETIME(prefix,format)", "description", "Generates a string with prefix and formatted date, e.g., __PREFIX_DATETIME(order_,yyyyMMdd)"));
        definitions.add(Map.of("format", "__RANDOM_STRING(length[,mode])", "description", "Generates a random string, e.g., __RANDOM_STRING(10,a) - modes: a=alphanumeric, u=uppercase, l=lowercase, m=mixed case, n=numeric only"));

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

        Pattern prefixTimestampPattern = Pattern.compile("__PREFIX_TIMESTAMP\\((.*?)\\)");
        Matcher prefixTimestampMatcher = prefixTimestampPattern.matcher(resolvedValue);
        StringBuffer sbPrefixTs = new StringBuffer();
        while (prefixTimestampMatcher.find()) {
            String prefix = prefixTimestampMatcher.group(1);
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            prefixTimestampMatcher.appendReplacement(sbPrefixTs, Matcher.quoteReplacement(prefix + timestamp));
        }
        prefixTimestampMatcher.appendTail(sbPrefixTs);
        resolvedValue = sbPrefixTs.toString();

        Pattern prefixDatetimePattern = Pattern.compile("__PREFIX_DATETIME\\((.*?),(.*?)\\)");
        Matcher prefixDatetimeMatcher = prefixDatetimePattern.matcher(resolvedValue);
        StringBuffer sbPrefixDt = new StringBuffer();
        while (prefixDatetimeMatcher.find()) {
            String prefix = prefixDatetimeMatcher.group(1);
            String format = prefixDatetimeMatcher.group(2);
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
                String formattedDate = LocalDateTime.now().format(dtf);
                prefixDatetimeMatcher.appendReplacement(sbPrefixDt, Matcher.quoteReplacement(prefix + formattedDate));
            } catch (Exception e) {
                System.err.println("Error formatting date with prefix: " + e.getMessage());
            }
        }
        prefixDatetimeMatcher.appendTail(sbPrefixDt);
        resolvedValue = sbPrefixDt.toString();

        Pattern randomStringPattern = Pattern.compile("__RANDOM_STRING\\((\\d+)(?:,([aulnm]))?\\)");
        Matcher randomStringMatcher = randomStringPattern.matcher(resolvedValue);
        StringBuffer sbRandomString = new StringBuffer();
        while (randomStringMatcher.find()) {
            int length = Integer.parseInt(randomStringMatcher.group(1));
            String mode = randomStringMatcher.groupCount() > 1 && randomStringMatcher.group(2) != null ? 
                         randomStringMatcher.group(2) : "a";
            String randomString = generateRandomString(length, mode);
            randomStringMatcher.appendReplacement(sbRandomString, Matcher.quoteReplacement(randomString));
        }
        randomStringMatcher.appendTail(sbRandomString);
        resolvedValue = sbRandomString.toString();

        return resolvedValue;
    }


    private String generateRandomString(int length, String mode) {
        String chars;
        switch (mode) {
            case "u": // uppercase only
                chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                break;
            case "l": // lowercase only
                chars = "abcdefghijklmnopqrstuvwxyz";
                break;
            case "m": // mixed case (no numbers)
                chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                break;
            case "n": // numbers only
                chars = "0123456789";
                break;
            case "a": // alphanumeric (default)
            default:
                chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                break;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = ThreadLocalRandom.current().nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
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
                case "datetime" -> "__DATETIME(" + (args != null ? args : "") + ")";
                case "randomstring" -> {
                    if (args != null) {
                        String[] parts = args.split(",");
                        if (parts.length > 1) {
                            yield "__RANDOM_STRING(" + parts[0] + "," + parts[1] + ")";
                        } else {
                            yield "__RANDOM_STRING(" + args + ")";
                        }
                    } else {
                        yield "__RANDOM_STRING(10)";
                    }
                }
                default -> matchResult.group(0); // No change if function is not recognized
            };
        });
    }

    public void setCustomVariables(List<GroovyScriptEngine> customVariables) {
        this.customVariables = customVariables;
    }
}
