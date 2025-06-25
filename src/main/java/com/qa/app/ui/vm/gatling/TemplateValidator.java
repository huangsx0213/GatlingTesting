package com.qa.app.ui.vm.gatling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateValidator {

    private static final ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Configuration staticFreemarkerCfg;

    static {
        staticFreemarkerCfg = new Configuration(new Version("2.3.32"));
        staticFreemarkerCfg.setDefaultEncoding("UTF-8");
        staticFreemarkerCfg.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        staticFreemarkerCfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        staticFreemarkerCfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        staticFreemarkerCfg.setClassicCompatible(true);
    }

    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }

    public static ValidationResult validate(String content, String format) {
        if (content == null || content.isBlank()) {
            return new ValidationResult(true, null); // Empty is always valid
        }
        switch (format) {
            case "FTL":
                return validateFtl(content);
            case "JSON":
                return validateJson(content);
            case "XML":
                return validateXml(content);
            case "TEXT":
            default:
                return new ValidationResult(true, null);
        }
    }

    public static String format(String content, String format) throws Exception {
        if (content == null || content.isBlank()) {
            return "";
        }
        switch (format) {
            case "JSON":
                return formatJson(content);
            case "XML":
                return formatXml(content);
            case "FTL":
                return formatFtl(content);
            default:
                return content; // No change for TEXT
        }
    }

    // --- FTL Specific Logic ---

    private static ValidationResult validateFtl(String templateContent) {
        String processed = preprocessForFreeMarker(templateContent);
        try {
            new Template("ftl-validation", new StringReader(processed), staticFreemarkerCfg);
            return new ValidationResult(true, null);
        } catch (IOException e) {
            return new ValidationResult(false, "FTL syntax error: " + e.getMessage());
        }
    }

    private static String formatFtl(String ftlContent) throws Exception {
        String trimmedContent = ftlContent.trim();
        boolean isXml = !trimmedContent.isEmpty() && trimmedContent.startsWith("<");

        if (isXml) {
            return prettyPrintXmlFreemarker(ftlContent);
        } else {
            return prettyPrintJsonFreemarker(ftlContent);
        }
    }

    private static String prettyPrintJsonFreemarker(String source) {
        StringBuilder result = new StringBuilder();
        int indent = 0;
        String indentString = "  ";
        String[] lines = source.split("\\R");

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("[#--") && trimmedLine.endsWith("--]")) {
                if (!result.toString().isEmpty() && !result.toString().endsWith(System.lineSeparator())) {
                    result.append(System.lineSeparator());
                }
                result.append(indentString.repeat(indent));
                result.append(trimmedLine);
                result.append(System.lineSeparator());
                continue;
            }

            String contentPart = line;
            String commentPart = "";
            int commentStart = line.indexOf("[#--");
            if (commentStart != -1) {
                contentPart = line.substring(0, commentStart);
                commentPart = line.substring(commentStart);
            }

            Pattern pattern = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|\\[/?#?[\\s\\S]*?\\]|[\\{\\}\\[\\],:]|[^\\s\"\\{\\}\\[\\],:]+");
            Matcher matcher = pattern.matcher(contentPart);

            boolean needsIndent = true;
            String lastToken = result.toString().trim().endsWith("{") || result.toString().trim().endsWith("[") ? "" : "dummy";

            while (matcher.find()) {
                String token = matcher.group().trim();
                if (token.isEmpty()) continue;

                if (token.equals("}") || token.equals("]")) {
                    indent = Math.max(0, indent - 1);
                    if (!lastToken.isEmpty() && !result.toString().trim().endsWith("{") && !result.toString().trim().endsWith("[")) {
                        result.append(System.lineSeparator());
                    }
                    result.append(indentString.repeat(indent));
                } else if (needsIndent) {
                    if (!result.toString().trim().isEmpty() && !result.toString().trim().endsWith("\n")) {
                        result.append(System.lineSeparator());
                    }
                    result.append(indentString.repeat(indent));
                }

                result.append(token);

                if (token.equals("{") || token.equals("[")) {
                    indent++;
                    needsIndent = true;
                } else if (token.equals(",")) {
                    needsIndent = true;
                } else if (token.equals(":")) {
                    result.append(" ");
                    needsIndent = false;
                } else {
                    needsIndent = false;
                }
                lastToken = token;
            }

            if (!commentPart.isEmpty()) {
                result.append(" ").append(commentPart.trim());
            }

            if (matcher.find(0) || !commentPart.isEmpty()) {
                if (!result.toString().endsWith(System.lineSeparator())) {
                    result.append(System.lineSeparator());
                }
            }
        }
        String raw = result.toString();
        return java.util.Arrays.stream(raw.split("\\R"))
                .filter(line -> !line.trim().isEmpty())
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("");
    }

    private static String prettyPrintXmlFreemarker(String source) {
        StringBuilder result = new StringBuilder();
        int indent = 0;
        String indentString = "  ";
        Pattern pattern = Pattern.compile("<!--[\\s\\S]*?-->|\\[/?#?[\\s\\S]*?\\]|<[^>]+>|[^<]+");
        Matcher matcher = pattern.matcher(source);

        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.isEmpty()) continue;

            String contentPart = token;
            String commentPart = "";
            int ftlCommentStart = token.indexOf("[#--");

            if (ftlCommentStart > 0 && token.startsWith("<")) {
                contentPart = token.substring(0, ftlCommentStart).trim();
                commentPart = token.substring(ftlCommentStart).trim();
            }

            boolean isClosingTag = contentPart.startsWith("</");
            boolean isSelfClosingTag = contentPart.endsWith("/>");
            boolean isProcessingInstruction = contentPart.startsWith("<?");
            boolean isDoctype = contentPart.startsWith("<!DOCTYPE");
            boolean isFtlDirective = contentPart.startsWith("[#") || contentPart.startsWith("[/#") || contentPart.startsWith("[@");
            boolean isXmlComment = contentPart.startsWith("<!--");
            boolean isTextNode = !contentPart.startsWith("<");

            if (isClosingTag || (isFtlDirective && contentPart.startsWith("[/#"))) {
                indent = Math.max(0, indent - 1);
            }

            result.append(indentString.repeat(indent));
            result.append(contentPart);

            if (!commentPart.isEmpty()) {
                result.append(" ").append(commentPart);
            }

            result.append(System.lineSeparator());

            if (!isClosingTag && !isSelfClosingTag && !isProcessingInstruction && !isDoctype && !isFtlDirective && !isXmlComment && !isTextNode) {
                indent++;
            } else if (isFtlDirective && !contentPart.startsWith("[/#") && !contentPart.contains("]")) {
                String directiveName = contentPart.substring(2, contentPart.indexOf(" ")).trim();
                if (!directiveName.equals("assign") && !directiveName.equals("include") && !directiveName.equals("import")) {
                    indent++;
                }
            }
        }
        return result.toString().trim();
    }

    private static String preprocessForFreeMarker(String content) {
        return content.replaceAll("@\\{([^}]+)\\}", "[=$1]");
    }

    // --- JSON Specific Logic ---

    private static ValidationResult validateJson(String json) {
        try {
            jsonMapper.readTree(json);
            return new ValidationResult(true, null);
        } catch (IOException e) {
            return new ValidationResult(false, "Invalid JSON: " + e.getMessage());
        }
    }

    private static String formatJson(String json) throws IOException {
        Object jsonObj = jsonMapper.readValue(json, Object.class);
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
    }

    // --- XML Specific Logic ---

    private static ValidationResult validateXml(String xml) {
        try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            return new ValidationResult(true, null);
        } catch (Exception e) {
            return new ValidationResult(false, "Invalid XML: " + e.getMessage());
        }
    }

    private static String formatXml(String xml) throws Exception {
        Source xmlInput = new StreamSource(new StringReader(xml));
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 4);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(xmlInput, xmlOutput);
        return xmlOutput.getWriter().toString();
    }
} 