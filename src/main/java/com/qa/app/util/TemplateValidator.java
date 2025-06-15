package com.qa.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A utility class to validate and format different template types.
 */
public class TemplateValidator {

    private static final Configuration freemarkerCfg;
    private static final ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    static {
        freemarkerCfg = new Configuration(new Version("2.3.32"));
        freemarkerCfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        freemarkerCfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        freemarkerCfg.setClassicCompatible(true);
    }

    // --- Public API for Different Formats ---

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
            new Template("ftl-validation", processed, freemarkerCfg);
            // We could add a dummy render check here as well for deeper validation if needed.
            return new ValidationResult(true, null);
        } catch (IOException e) {
            return new ValidationResult(false, "FTL syntax error: " + e.getMessage());
        }
    }

    private static String formatFtl(String ftlContent) throws Exception {
        // This is a best-effort, intelligent formatter.
        // For FTL/XML, it uses a robust, regex-based tokenizer to provide indentation
        // without performing strict validation, thus avoiding parser crashes on complex templates.
        // For JSON, it uses the standard pretty-printer.

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

        // Regex to split lines while respecting FTL comments
        String[] lines = source.split("\\R");

        for (String line : lines) {
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

            if (matcher.find(0) || !commentPart.isEmpty()) { // Check if the line had any tokens or a comment
                 if (!result.toString().endsWith(System.lineSeparator())) {
                    result.append(System.lineSeparator());
                }
            }
        }
        String raw = result.toString();
        // Remove all empty or whitespace-only lines
        return java.util.Arrays.stream(raw.split("\\R"))
                .filter(line -> !line.trim().isEmpty())
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("");
    }

    private static String prettyPrintXmlFreemarker(String source) {
        StringBuilder result = new StringBuilder();
        int indent = 0;
        String indentString = "  ";
        // Regex to tokenize into: tags, FTL directives (including comments), and text content.
        Pattern pattern = Pattern.compile("<!--[\\s\\S]*?-->|\\[/?#?[\\s\\S]*?\\]|<[^>]+>|[^<]+");
        Matcher matcher = pattern.matcher(source);

        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.isEmpty()) continue;

            String contentPart = token;
            String commentPart = "";
            int ftlCommentStart = token.indexOf("[#--");
            
            // Handle FTL comments attached to XML tags
            if (ftlCommentStart > 0 && token.startsWith("<")) {
                contentPart = token.substring(0, ftlCommentStart).trim();
                commentPart = token.substring(ftlCommentStart).trim();
            }

            boolean isClosingTag = contentPart.startsWith("</");
            boolean isOpeningTag = contentPart.startsWith("<") && !isClosingTag;
            boolean isSelfClosing = contentPart.endsWith("/>");

            if (isClosingTag) {
                indent = Math.max(0, indent - 1);
            }
            
            result.append(indentString.repeat(indent));
            result.append(contentPart);

            if (isOpeningTag && !isSelfClosing) {
                indent++;
            }
            
            if(!commentPart.isEmpty()){
                result.append(" ").append(commentPart);
            }
            
            result.append(System.lineSeparator());
        }
        String raw = result.toString();
        // Remove all empty or whitespace-only lines
        return java.util.Arrays.stream(raw.split("\\R"))
                .filter(line -> !line.trim().isEmpty())
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("");
    }

    private static String renderDummyFtl(String templateContent) throws IOException, TemplateException {
        String processed = preprocessForFreeMarker(templateContent);
        Map<String, Object> dummyModel = new HashMap<>();
        Pattern pattern = Pattern.compile("\\@\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(templateContent);
        while (matcher.find()) {
            dummyModel.put(matcher.group(1).trim(), "dummy_value");
        }
        
        Template template = new Template("ftl-dummy-render", processed, freemarkerCfg);
        StringWriter out = new StringWriter();
        template.process(dummyModel, out);
        return out.toString();
    }

    private static String preprocessForFreeMarker(String content) {
        String processed = content;
        processed = processed.replaceAll("\\@\\{([^}]+)\\}", "[=$1]");
        processed = processed.replace("\\\"", "\"");
        processed = processed.replaceAll("\\[#--[\\s\\S]*?--\\]", "");
        return processed;
    }

    // --- JSON Specific Logic ---

    private static ValidationResult validateJson(String json) {
        try {
            jsonMapper.readTree(json);
            return new ValidationResult(true, null);
        } catch (IOException e) {
            return new ValidationResult(false, "Invalid JSON structure: " + e.getMessage());
        }
    }

    private static String formatJson(String json) throws IOException {
        Object obj = jsonMapper.readValue(json, Object.class);
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
    
    // --- XML Specific Logic ---

    private static ValidationResult validateXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Secure processing
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setNamespaceAware(true);
            factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            return new ValidationResult(true, null);
        } catch (Exception e) {
            return new ValidationResult(false, "Invalid XML structure: " + e.getMessage());
        }
    }
    
    private static String formatXml(String xml) throws Exception {
        Source xmlInput = new StreamSource(new StringReader(xml));
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(xmlInput, xmlOutput);
        return stringWriter.toString();
    }

    // --- Result Wrapper Class ---

    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }
} 