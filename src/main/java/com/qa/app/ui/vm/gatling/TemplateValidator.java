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
import java.util.ArrayList;
import java.util.List;

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
        // 1. Pre-processing: Normalize smart quotes and placeholders
        source = source.replace('\u201C', '"').replace('\u201D', '"');
        
        Pattern p = Pattern.compile("@\\s*\\{\\s*([\\s\\S]+?)\\s*\\}");
        Matcher placeholderMatcher = p.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (placeholderMatcher.find()) {
            placeholderMatcher.appendReplacement(sb, "@{" + placeholderMatcher.group(1).trim() + "}");
        }
        placeholderMatcher.appendTail(sb);
        source = sb.toString();

        // 2. Tokenize
        Pattern tokenPattern = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|@\\{[^}]+\\}|\\[#--[\\s\\S]*?--\\]|\\[/?#([a-zA-Z]+)[^]]*\\]|[\\{\\}\\[\\],:]|[^\\s\"\\{\\}\\[\\],:]+");
        List<String> tokens = new ArrayList<>();
        Matcher m = tokenPattern.matcher(source);
        while (m.find()) {
            String t = m.group().trim();
            if (!t.isEmpty()) {
                tokens.add(t);
            }
        }

        // 3. Formatting Logic with a stateful machine
        StringBuilder result = new StringBuilder();
        int indent = 0;
        final String indentString = "  ";
        boolean onNewLine = true;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            // Pre-token logic: Handle indentation reduction
            if (token.equals("}") || token.equals("]") || token.startsWith("[/#") || token.startsWith("[#else")) {
                indent = Math.max(0, indent - 1);
                if (!onNewLine) {
                    result.append(System.lineSeparator());
                    onNewLine = true;
                }
            }
            
            // Handle special `value [#if], [/#if]` pattern
            boolean isValueToken = !"{}[],:".contains(token) && !token.startsWith("[#") && !token.startsWith("[/@");
            if (isValueToken && i + 3 < tokens.size() && tokens.get(i+1).startsWith("[#if") && tokens.get(i+2).equals(",") && tokens.get(i+3).startsWith("[/#if")) {
                if (onNewLine) {
                    result.append(indentString.repeat(indent));
                }
                result.append(token).append(" ").append(tokens.get(i+1)).append(tokens.get(i+2)).append(tokens.get(i+3));
                onNewLine = false; // We just printed content
                i += 3; // Consume the pattern
            } else {
                 // Regular token processing
                if (onNewLine) {
                    result.append(indentString.repeat(indent));
                }
                result.append(token);
                onNewLine = false;
            }

            // Post-token logic: Handle spacing and line breaks
            if (token.equals(":")) {
                result.append(" ");
            }

            if (token.equals("{") || token.equals("[") || token.equals(",") || token.startsWith("[#--") || token.startsWith("[#list") || token.startsWith("[#if") || token.startsWith("[/#") || token.startsWith("[#else")) {
                result.append(System.lineSeparator());
                onNewLine = true;
            }
            
            // Post-token logic: Handle indentation increase
            if (token.equals("{") || token.equals("[") || token.startsWith("[#list") || token.startsWith("[#if") || token.startsWith("[#else")) {
                indent++;
            }
        }

        return result.toString();
    }

    private static String prettyPrintXmlFreemarker(String source) {
        String indentString = "  ";
        Pattern pattern = Pattern.compile("<!--[\\s\\S]*?-->|\\[/?#?[\\s\\S]*?\\]|<[^>]+>|[^<]+");

        // Tokenize first so we can look ahead when necessary
        List<String> tokens = new ArrayList<>();
        Matcher m = pattern.matcher(source);
        while (m.find()) {
            String t = m.group();
            if (t != null && !t.trim().isEmpty()) {
                tokens.add(t.trim());
            }
        }

        StringBuilder result = new StringBuilder();
        int indent = 0;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

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

            // Decrease indent for closing elements before writing them
            if (isClosingTag || (isFtlDirective && contentPart.startsWith("[/#"))) {
                indent = Math.max(0, indent - 1);
            }

            // Attempt to inline: <tag>text</tag>
            if (!isClosingTag && !isSelfClosingTag && !isProcessingInstruction && !isDoctype && !isFtlDirective && !isXmlComment) {
                if (i + 2 < tokens.size()) {
                    String nextToken = tokens.get(i + 1).trim();
                    String nextNextToken = tokens.get(i + 2).trim();

                    boolean nextIsText = !nextToken.startsWith("<");
                    String elemName = getElementName(contentPart);
                    if (nextIsText && !elemName.isEmpty() && nextNextToken.equals("</" + elemName + ">")) {
                        // Inline representation
                        result.append(indentString.repeat(indent))
                              .append(contentPart)
                              .append(nextToken)
                              .append(nextNextToken);

                        if (!commentPart.isEmpty()) {
                            result.append(" ").append(commentPart);
                        }

                        result.append(System.lineSeparator());
                        // Skip the two tokens we just processed
                        i += 2;
                        continue;
                    }
                }
            }

            // Default handling
            result.append(indentString.repeat(indent)).append(contentPart);
            if (!commentPart.isEmpty()) {
                result.append(" ").append(commentPart);
            }
            result.append(System.lineSeparator());

            // Increase indent after opening tags when appropriate
            if (!isClosingTag && !isSelfClosingTag && !isProcessingInstruction && !isDoctype && !isFtlDirective && !isXmlComment && !isTextNode) {
                indent++;
            } else if (isFtlDirective && !contentPart.startsWith("[/#") && !contentPart.contains("]")) {
                int spaceIdx = contentPart.indexOf(" ");
                int endIdx = spaceIdx > -1 ? spaceIdx : contentPart.length();
                String directiveName = contentPart.substring(2, endIdx).trim();
                if (!directiveName.equals("assign") && !directiveName.equals("include") && !directiveName.equals("import")) {
                    indent++;
                }
            }
        }

        return result.toString().trim();
    }

    /**
     * Extracts the XML element name from an opening tag like <tag ...> or </tag>
     */
    private static String getElementName(String tagToken) {
        Matcher matcher = Pattern.compile("<\\/?\\s*([a-zA-Z0-9_:.-]+)").matcher(tagToken);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
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