package com.qa.app.service.util;

import java.util.List;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of built-in variable converters that are available out of the box.
 * All converter names are case-insensitive. They should be referenced in
 * expressions exactly as defined here (case doesn't matter).
 */
public enum BuiltInVariableConverter implements VariableConverter {

    /** Trim whitespace at both ends. */
    TRIM("Trim whitespace", "value", "TRIM('  abc ')") {
        @Override
        public Object convert(Object value, List<String> params) {
            return value == null ? null : value.toString().trim();
        }
    },

    /** Convert to upper case. */
    UPPER("Convert to upper case", "value", "UPPER('abc')") {
        @Override
        public Object convert(Object value, List<String> params) {
            return value == null ? null : value.toString().toUpperCase();
        }
    },

    /** Substring with start and end index arguments. 
     * 
     * Usage: SUBSTRING(string, start, end)
    */
    SUBSTRING("Substring by indexes", "value,start,end", "SUBSTRING('hello',1,4)") {
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
    },

    /** Format and convert date/time with timezone and presets. Presets: " + getPresetKeys() */
    DATETIME("Converts date/time between timezones. Default input zone is UTC. Presets: " + getPresetKeys(),
            "value,outputPattern[,outputZone][,inputPattern][,inputZone]",
            "DATETIME('2023-10-10T09:00:00', 'ISO_ZONED_DATETIME', 'Asia/Hong_Kong', 'SQL_TIMESTAMP', 'Asia/Tokyo')") {
        @Override
        public Object convert(Object value, List<String> params) throws Exception {
            if (value == null) {
                return null;
            }
            if (params.isEmpty()) {
                throw new IllegalArgumentException("DATETIME requires at least `outputPattern` parameter.");
            }

            // New signature: value, outputPattern, [outputZone], [inputPattern], [inputZone]
            String outputPatternArg = params.get(0).trim();
            String outputZoneStr = params.size() >= 2 ? params.get(1).trim() : null;
            String inputPatternArg = params.size() >= 3 ? params.get(2).trim() : null;
            String inputZoneStr = params.size() >= 4 ? params.get(3).trim() : null;

            // Resolve patterns from presets or use as literal
            String outputPattern = PatternPresetHolder.getPattern(outputPatternArg);
            String inputPattern = (inputPatternArg != null) ? PatternPresetHolder.getPattern(inputPatternArg) : null;

            ZoneId outputZone = (outputZoneStr != null && !outputZoneStr.isEmpty()) ? ZoneId.of(outputZoneStr) : ZoneId.systemDefault();
            ZoneId inputZone = (inputZoneStr != null && !inputZoneStr.isEmpty()) ? ZoneId.of(inputZoneStr) : ZoneId.of("UTC");

            ZonedDateTime zdt;

            if (value instanceof ZonedDateTime) {
                zdt = (ZonedDateTime) value; // Already has zone, inputZone is ignored.
            } else if (value instanceof LocalDateTime) {
                zdt = ((LocalDateTime) value).atZone(inputZone); // Apply specified input zone
            } else if (value instanceof Number) {
                long epochMillis = ((Number) value).longValue();
                zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), inputZone);
            } else {
                String strVal = value.toString().trim();
                if (strVal.matches("\\d+")) {
                    long epochMillis = Long.parseLong(strVal);
                    zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), inputZone);
                } else {
                    DateTimeFormatter inFmt = (inputPattern != null && !inputPattern.isEmpty())
                            ? DateTimeFormatter.ofPattern(inputPattern)
                            : DateTimeFormatter.ISO_DATE_TIME;
                    try {
                        // Priority 1: Try parsing with full zone info from string first.
                        zdt = ZonedDateTime.parse(strVal, inFmt);
                    } catch (DateTimeParseException e1) {
                        try {
                            // Priority 2: Parse as local time and apply the specified inputZone.
                            LocalDateTime ldt = LocalDateTime.parse(strVal, inFmt);
                            zdt = ldt.atZone(inputZone);
                        } catch (DateTimeParseException e2) {
                            // Priority 3: Fallback to local date.
                            LocalDate ld = LocalDate.parse(strVal, inFmt);
                            zdt = ld.atStartOfDay(inputZone);
                        }
                    }
                }
            }
            DateTimeFormatter outFmt = DateTimeFormatter.ofPattern(outputPattern);
            return zdt.withZoneSameInstant(outputZone).format(outFmt);
        }
    };

    // Lazy-loaded holder class for pattern presets to avoid initialization order issues
    private static class PatternPresetHolder {
        private static final Map<String, String> PATTERN_PRESETS = new HashMap<>();
        static {
            PATTERN_PRESETS.put("ISO_DATE", "yyyy-MM-dd");
            PATTERN_PRESETS.put("ISO_DATETIME", "yyyy-MM-dd'T'HH:mm:ss");
            PATTERN_PRESETS.put("ISO_ZONED_DATETIME", "yyyy-MM-dd'T'HH:mm:ssXXX");
            PATTERN_PRESETS.put("SQL_TIMESTAMP", "yyyy-MM-dd HH:mm:ss");
            PATTERN_PRESETS.put("US_DATETIME", "MM/dd/yyyy HH:mm:ss");
            PATTERN_PRESETS.put("RFC_1123", "EEE, dd MMM yyyy HH:mm:ss zzz");
        }

        static String getPattern(String key) {
            return PATTERN_PRESETS.getOrDefault(key, key);
        }

        static String getPresetKeys() {
            return String.join(", ", PATTERN_PRESETS.keySet());
        }
    }

    private static String getPresetKeys() {
        return PatternPresetHolder.getPresetKeys();
    }

    private final String description;
    private final String paramSpec;
    private final String sampleUsage;

    BuiltInVariableConverter(String description, String paramSpec, String sampleUsage) {
        this.description = description;
        this.paramSpec = paramSpec;
        this.sampleUsage = sampleUsage;
    }

    public String getDescription() {
        return description;
    }

    public String getParamSpec() {
        return paramSpec;
    }

    public String getSampleUsage() {
        return sampleUsage;
    }

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