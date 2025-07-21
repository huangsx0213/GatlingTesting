package com.qa.app.service.util;

import java.util.List;

/**
 * Functional interface for converting a value using optional parameters.
 */
@FunctionalInterface
public interface VariableConverter {
    Object convert(Object value, List<String> params) throws Exception;
} 