package com.qa.app.service.script;

import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IVariableService;
import com.qa.app.service.impl.VariableServiceImpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    public static String getBuiltInVariablesDocumentation() {
        return "Built-in variables usage examples:\n\n" +
                "1. UUID: __UUID\n" +
                "   Example: \"id\": \"__UUID\" → \"id\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\"\n\n" +
                "2. Timestamp (ms): __TIMESTAMP\n" +
                "   Example: \"time\": \"__TIMESTAMP\" → \"time\": \"1684932156123\"\n\n" +
                "3. Date/Time: __DATETIME(format[, timezone])\n" +
                "   Examples:\n" +
                "   - `__DATETIME(yyyy-MM-dd HH:mm:ss)` → `2025-07-05 12:30:45` (带分隔符的完整时间)\n" +
                "   - `__DATETIME(yyyyMMddHHmmss)` → `20250705123045` (年月日时分秒)\n" +
                "   - `__DATETIME(yyyy-MM-dd'T'HH:mm:ssXXX, Asia/Shanghai)` → `2025-07-05T12:30:45+08:00` (ISO 8601 格式)\n\n" +
                "4. Prefix + Timestamp (ms): __PREFIX_TIMESTAMP(prefix)\n" +
                "   Example: \"orderId\": \"__PREFIX_TIMESTAMP(ORDER_)\" → \"orderId\": \"ORDER_1684932156123\"\n\n" +
                "5. Prefix + DateTime: __PREFIX_DATETIME(prefix,format[, timezone])\n" +
                "   Example: \"batchId\": \"__PREFIX_DATETIME(BATCH_,yyyyMMdd,America/New_York)\" → \"batchId\": \"BATCH_20230524\"\n\n" +
                "6. Random String: __RANDOM_STRING(length,mode)\n" +
                "   Modes available:\n" +
                "   - a: Alphanumeric (default) - uppercase + lowercase + numbers\n" +
                "     Example: \"key\": \"__RANDOM_STRING(8,a)\" → \"key\": \"A3bX71pQ\"\n" +
                "   - u: Uppercase only - only uppercase letters\n" +
                "     Example: \"code\": \"__RANDOM_STRING(5,u)\" → \"code\": \"AXFGH\"\n" +
                "   - l: Lowercase only - only lowercase letters\n" +
                "     Example: \"username\": \"__RANDOM_STRING(6,l)\" → \"username\": \"abcdef\"\n" +
                "   - m: Mixed case - uppercase + lowercase (no numbers)\n" +
                "     Example: \"name\": \"__RANDOM_STRING(7,m)\" → \"name\": \"AbCdEfG\"\n" +
                "   - n: Numeric only - only digits 0-9\n" +
                "     Example: \"pin\": \"__RANDOM_STRING(4,n)\" → \"pin\": \"1234\"";
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
     * 2. Timestamp (ms): __TIMESTAMP
     *    Example: "time": "__TIMESTAMP" → "time": "1684932156123"
     * 
     * 3. Date/Time: __DATETIME(format[, timezone])
     *    Examples:
     *    - `__DATETIME(yyyy-MM-dd HH:mm:ss)` → `2025-07-05 12:30:45` (带分隔符的完整时间)
     *    - `__DATETIME(yyyyMMddHHmmss)` → `20250705123045` (年月日时分秒)
     *    - `__DATETIME(yyyy-MM-dd'T'HH:mm:ssXXX, Asia/Shanghai)` → `2025-07-05T12:30:45+08:00` (ISO 8601 格式)
     * 
     * 4. Prefix + Timestamp (ms): __PREFIX_TIMESTAMP(prefix)
     *    Example: "orderId": "__PREFIX_TIMESTAMP(ORDER_)" → "orderId": "ORDER_1684932156123"
     * 
     * 5. Prefix + DateTime: __PREFIX_DATETIME(prefix,format[, timezone])
     *    Example: "batchId": "__PREFIX_DATETIME(BATCH_,yyyyMMdd,America/New_York)" → "batchId": "BATCH_20230524"
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
        definitions.add(Map.of("format", "__TIMESTAMP", "description", "Generates the current Unix timestamp (milliseconds)"));
        definitions.add(Map.of("format", "__DATETIME(format[, timezone])", "description", "Generates date/time with optional timezone, e.g., __DATETIME(yyyy-MM-dd, UTC)"));
        definitions.add(Map.of("format", "__PREFIX_TIMESTAMP(prefix)", "description", "Generates a string with prefix and millisecond timestamp, e.g., __PREFIX_TIMESTAMP(test_)"));
        definitions.add(Map.of("format", "__PREFIX_DATETIME(prefix,format[, timezone])", "description", "Generates string with prefix and formatted date with optional timezone, e.g., __PREFIX_DATETIME(order_,yyyyMMdd,UTC)"));
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
            resolvedValue = resolvedValue.replace("__TIMESTAMP", String.valueOf(System.currentTimeMillis()));
        }

        Pattern datetimePattern = Pattern.compile("__DATETIME\\(([^,)]+)(?:,\\s*([^)]+))?\\)");
        Matcher datetimeMatcher = datetimePattern.matcher(resolvedValue);
        StringBuffer sbDatetime = new StringBuffer();
        while (datetimeMatcher.find()) {
            String format = datetimeMatcher.group(1).trim();
            String timezone = (datetimeMatcher.groupCount() > 1 && datetimeMatcher.group(2) != null) ? datetimeMatcher.group(2).trim() : null;
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
                String formattedDate;
                if (timezone != null && !timezone.isBlank()) {
                    formattedDate = ZonedDateTime.now(ZoneId.of(timezone)).format(dtf);
                } else {
                    formattedDate = LocalDateTime.now().format(dtf);
                }
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
            String timestamp = String.valueOf(System.currentTimeMillis());
            prefixTimestampMatcher.appendReplacement(sbPrefixTs, Matcher.quoteReplacement(prefix + timestamp));
        }
        prefixTimestampMatcher.appendTail(sbPrefixTs);
        resolvedValue = sbPrefixTs.toString();

        Pattern prefixDatetimePattern = Pattern.compile("__PREFIX_DATETIME\\(([^,)]+),([^,)]+)(?:,\\s*([^)]+))?\\)");
        Matcher prefixDatetimeMatcher = prefixDatetimePattern.matcher(resolvedValue);
        StringBuffer sbPrefixDt = new StringBuffer();
        while (prefixDatetimeMatcher.find()) {
            String prefix = prefixDatetimeMatcher.group(1).trim();
            String format = prefixDatetimeMatcher.group(2).trim();
            String timezone = (prefixDatetimeMatcher.groupCount() > 2 && prefixDatetimeMatcher.group(3) != null) ? prefixDatetimeMatcher.group(3).trim() : null;
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
                String formattedDate;
                if (timezone != null && !timezone.isBlank()) {
                    formattedDate = ZonedDateTime.now(ZoneId.of(timezone)).format(dtf);
                } else {
                    formattedDate = LocalDateTime.now().format(dtf);
                }
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
