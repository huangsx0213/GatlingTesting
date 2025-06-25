package com.qa.app.util;

import freemarker.core.JSONOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

import java.io.StringWriter;
import java.util.HashMap;
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
        String fmTemplateStr = preprocessForFreeMarker(templateStr);

        // Construct the data model
        Map<String, Object> dataModel = new HashMap<>();
        if (variableExpressionMap != null) {
            for (Map.Entry<String, String> entry : variableExpressionMap.entrySet()) {
                // Call VariableGenerator.generate on variable expressions again to achieve true dynamic effects
                String value = VariableGenerator.generate(entry.getValue());
                dataModel.put(entry.getKey(), value);
            }
        }

        try {
            Template template = new Template("rtTemplate", fmTemplateStr, FREEMARKER_CFG);
            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            return out.toString();
        } catch (TemplateException | java.io.IOException e) {
            // Return an error message if runtime rendering fails, to prevent the entire stress test from crashing
            return "TEMPLATE_RUNTIME_ERROR: " + e.getMessage();
        }
    }
} 