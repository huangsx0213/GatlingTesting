package com.qa.app.service.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.service.script.VariableGenerator;
import freemarker.core.JSONOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A utility class for rendering request and headers templates at runtime.
 * Similar to the UI layer's {@link com.qa.app.ui.vm.gatling.TemplateHandler},
 * but this class focuses on regenerating dynamic variables before each Gatling request,
 * ensuring variables are generated in real-time for multiple loop requests.
 */
public class RuntimeTemplateProcessor {

    private static final Configuration FREEMARKER_CFG;

    static {
        FREEMARKER_CFG = new Configuration(new Version("2.3.32"));
        FREEMARKER_CFG.setDefaultEncoding("UTF-8");
        FREEMARKER_CFG.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        FREEMARKER_CFG.setOutputFormat(JSONOutputFormat.INSTANCE);
        FREEMARKER_CFG.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        FREEMARKER_CFG.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        FREEMARKER_CFG.setClassicCompatible(true);
    }

    private static final Pattern AT_PLACEHOLDER = Pattern.compile("@\\{([^}]+)}");

    // Jackson mapper for JSON detection
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * If the given string looks like a JSON object/array and can be parsed, return the parsed Map/List;
     * otherwise return the original string.
     */
    public static Object convertToModelValue(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();

        // Quick heuristics to skip obviously-non-JSON values
        if (trimmed.length() < 2) return raw;
        char first = trimmed.charAt(0);
        char last = trimmed.charAt(trimmed.length() - 1);
        if ((first == '{' && last == '}') || (first == '[' && last == ']')) {
            try {
                JsonNode node = JSON_MAPPER.readTree(trimmed);
                return processJsonNode(node);
            } catch (Exception ignored) {
                // Not valid JSON, ignore and treat as plain string
            }
        }
        return raw;
    }

    /**
     * Recursively walk JsonNode tree and replace text nodes containing "@{" patterns
     * by the result of VariableGenerator.generate(). The resulting structure is
     * returned as Map/List compatible with FreeMarker.
     */
    private static Object processJsonNode(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.fieldNames().forEachRemaining(field -> {
                JsonNode child = node.get(field);
                map.put(field, processJsonNode(child));
            });
            return map;
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode child : node) {
                list.add(processJsonNode(child));
            }
            return list;
        } else if (node.isTextual()) {
            return VariableGenerator.getInstance().resolveVariables(node.asText());
        } else if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isNull()) {
            return null;
        } else {
            // Other node types (binary etc.) return as text
            return node.asText();
        }
    }

    /**
     * Pre-process the template, converting user-defined @{var} placeholders to FreeMarker-compatible [=var] form.
     */
    private static String preprocessForFreeMarker(String content) {
        if (content == null) return null;
        return AT_PLACEHOLDER.matcher(content).replaceAll("[=$1]");
    }

    /**
     * Render the content template.
     *
     * @param templateStr           The template string (may contain @{var} placeholders)
     * @param variableExpressionMap Map of variable expressions, where values may also contain @{...} syntax
     * @return The rendered result string
     */
    public static String render(String templateStr, Map<String, String> variableExpressionMap) {
        if (templateStr == null || templateStr.isBlank()) {
            return templateStr;
        }
        
        // Process test variable references ${TCID.variableName}
        String processedTemplate = TestRunContext.processVariableReferences(templateStr);
        
        String fmTemplateStr = preprocessForFreeMarker(processedTemplate);

        // Construct the data model
        Map<String, Object> dataModel = new HashMap<>();
        if (variableExpressionMap != null) {
            for (Map.Entry<String, String> entry : variableExpressionMap.entrySet()) {
                // Process test variable references in variable values
                String processedValue = TestRunContext.processVariableReferences(entry.getValue());
                // Call VariableGenerator.generate on variable expressions again to achieve true dynamic effects
                String value = VariableGenerator.getInstance().resolveVariables(processedValue);
                dataModel.put(entry.getKey(), convertToModelValue(value));
            }
        }

        try {
            Template template = new Template("rtTemplate", fmTemplateStr, FREEMARKER_CFG);
            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            return out.toString();
        } catch (Exception e) {
            // Return an error message if runtime rendering fails, to prevent the entire stress test from crashing
            return "TEMPLATE_RUNTIME_ERROR: " + e.getMessage();
        }
    }
} 