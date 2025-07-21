package com.qa.app.service.util;

import com.qa.app.service.ServiceException;
import com.qa.app.service.api.IVariableTransformMethodService;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility responsible for parsing and applying conversion pipeline expressions
 * of the form "${var | trim | substring(0,5)}".
 *
 * This class does NOT perform variable resolution – caller must supply the
 * original value. It only executes the conversion chain.
 */
public final class VariableTransformUtil {

    private static final Pattern STEP_PATTERN = Pattern.compile("\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:\\(([^)]*)\\))?\\s*");

    private VariableTransformUtil() {}

    /**
     * Apply a pipeline expression to the provided value.
     *
     * @param value         original value (may be null)
     * @param pipelineExpr  expression e.g. "trim | substring(0,3)"
     * @param service       transform service for resolving converters
     * @return converted value
     */
    public static Object applyPipeline(Object value, String pipelineExpr, IVariableTransformMethodService service) throws ServiceException {
        if (pipelineExpr == null || pipelineExpr.isEmpty()) {
            return value;
        }
        String[] steps = pipelineExpr.split("\\|"); // split by pipe
        Object current = value;
        for (String stepRaw : steps) {
            String step = stepRaw.trim();
            if (step.isEmpty()) continue;
            Matcher m = STEP_PATTERN.matcher(step);
            if (!m.matches()) {
                throw new ServiceException("Invalid transform step syntax: " + step);
            }
            String methodName = m.group(1);
            List<String> params = new ArrayList<>();
            String paramStr = m.group(2);
            if (paramStr != null && !paramStr.trim().isEmpty()) {
                for (String p : paramStr.split(",")) {
                    params.add(p.trim());
                }
            }
            current = service.apply(methodName, current, params);
        }
        return current;
    }
} 