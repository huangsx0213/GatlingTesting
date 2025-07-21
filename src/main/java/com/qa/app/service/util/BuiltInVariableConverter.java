package com.qa.app.service.util;

import java.util.List;

/**
 * Enumeration of built-in variable converters that are available out of the box.
 * All converter names are case-insensitive. They should be referenced in
 * expressions exactly as defined here (case doesn't matter).
 */
public enum BuiltInVariableConverter implements VariableConverter {

    /** Trim whitespace at both ends. */
    TRIM {
        @Override
        public Object convert(Object value, List<String> params) {
            return value == null ? null : value.toString().trim();
        }
    },

    /** Convert to upper case. */
    UPPER {
        @Override
        public Object convert(Object value, List<String> params) {
            return value == null ? null : value.toString().toUpperCase();
        }
    },

    /** Substring with start and end index arguments. */
    SUBSTRING {
        @Override
        public Object convert(Object value, List<String> params) throws Exception {
            String str = value == null ? "" : value.toString();
            if (params.size() < 2) {
                throw new IllegalArgumentException("SUBSTRING requires 2 integer parameters: start, end");
            }
            int start = Integer.parseInt(params.get(0).trim());
            int end = Integer.parseInt(params.get(1).trim());
            return str.substring(start, end);
        }
    };

    /**
     * Utility for resolving case-insensitive enum constant.
     */
    public static BuiltInVariableConverter from(String name) {
        for (BuiltInVariableConverter c : values()) {
            if (c.name().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }
} 