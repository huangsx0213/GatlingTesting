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
 * 在运行时渲染请求/Headers 模板的工具类。
 * 与 UI 层的 {@link com.qa.app.ui.vm.gatling.TemplateHandler} 类似，
 * 但该类侧重于在 Gatling 的每一次请求之前重新生成动态变量，
 * 从而保证多次循环请求时变量都是实时生成的。
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
     * 对模板预处理，将用户使用的 @{var} 占位符转换为 FreeMarker 可识别的 [=var] 形式。
     */
    private static String preprocessForFreeMarker(String content) {
        if (content == null) return null;
        return AT_PLACEHOLDER.matcher(content).replaceAll("[=$1]");
    }

    /**
     * 渲染内容模板。
     *
     * @param templateStr           模板字符串（可能包含 @{var} 占位符）
     * @param variableExpressionMap 变量表达式映射，value 中可能也包含 @{...} 语法
     * @return 渲染后的结果字符串
     */
    public static String render(String templateStr, Map<String, String> variableExpressionMap) {
        if (templateStr == null || templateStr.isBlank()) {
            return templateStr;
        }
        String fmTemplateStr = preprocessForFreeMarker(templateStr);

        // 构造数据模型
        Map<String, Object> dataModel = new HashMap<>();
        if (variableExpressionMap != null) {
            for (Map.Entry<String, String> entry : variableExpressionMap.entrySet()) {
                // 对变量表达式再次调用 VariableGenerator.generate，以实现真正的***动态***效果
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
            // 运行时渲染失败时返回错误信息，避免整个压测崩溃
            return "TEMPLATE_RUNTIME_ERROR: " + e.getMessage();
        }
    }
} 