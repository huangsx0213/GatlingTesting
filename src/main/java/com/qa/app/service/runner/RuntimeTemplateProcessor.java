package com.qa.app.service.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.service.util.VariableGenerator;

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

public class RuntimeTemplateProcessor {

    private static final Configuration FREEMARKER_CFG;
    private static final Pattern AT_PLACEHOLDER = Pattern.compile("@\\{([^}]+)\\}");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    static {
        FREEMARKER_CFG = new Configuration(new Version("2.3.32"));
        FREEMARKER_CFG.setDefaultEncoding("UTF-8");
        FREEMARKER_CFG.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        FREEMARKER_CFG.setOutputFormat(JSONOutputFormat.INSTANCE);
        FREEMARKER_CFG.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        FREEMARKER_CFG.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        FREEMARKER_CFG.setClassicCompatible(true); // Treats missing variables as empty strings
    }

    public static Object convertToModelValue(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();

        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                JsonNode node = JSON_MAPPER.readTree(trimmed);
                return processJsonNode(node);
            } catch (Exception ignored) {
                // Not valid JSON, ignore and treat as plain string
            }
        }
        return raw;
    }

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
            return node.asText();
        }
    }

    private static String preprocessForFreeMarker(String content) {
        if (content == null) return null;
        return AT_PLACEHOLDER.matcher(content).replaceAll("[=$1]");
    }

    public static String render(String templateStr, Map<String, String> variableExpressionMap) {
        if (templateStr == null || templateStr.isBlank()) {
            return templateStr;
        }
        
        String processedTemplate = TestRunContext.processVariableReferences(templateStr);
        String fmTemplateStr = preprocessForFreeMarker(processedTemplate);

        Map<String, Object> dataModel = new HashMap<>();
        if (variableExpressionMap != null) {
            for (Map.Entry<String, String> entry : variableExpressionMap.entrySet()) {
                String processedValue = TestRunContext.processVariableReferences(entry.getValue());
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
            System.err.println("[ERROR] Template rendering failed: " + e.getMessage());
            return "TEMPLATE_RUNTIME_ERROR: " + e.getMessage();
        }
    }
} 