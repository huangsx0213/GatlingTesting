package com.qa.app.model;

public enum CheckType {
    STATUS, // HTTP status code
    JSON_PATH,
    XPATH,
    REGEX,
    DIFF,    // Difference check: compare value changes before and after an operation
    PRE_CHECK, // Pre-check: validate reference API value before main request
    PST_CHECK,  // Post-check: validate reference API value after main request
    DB
} 