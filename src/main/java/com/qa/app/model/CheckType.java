package com.qa.app.model;

public enum CheckType {
    STATUS, // HTTP status code
    JSON_PATH,
    XPATH,
    REGEX,
    DIFF    // Difference check: compare value changes before and after an operation
} 