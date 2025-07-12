package com.qa.app.util;

import com.qa.app.model.Operator;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

public class OperatorUtil {

    public static boolean compare(String actual, Operator operator, String expected) {
        if (operator == Operator.IS_NULL) {
            return actual == null;
        }
        if (operator == Operator.NOT_NULL) {
            return actual != null;
        }

        // For all other operators, actual value cannot be null
        if (actual == null) {
            return false;
        }

        switch (operator) {
            case IS:
                return Objects.equals(actual, expected);
            case IS_NOT:
                return !Objects.equals(actual, expected);
            case CONTAINS:
                return expected != null && actual.contains(expected);
            case NOT_CONTAINS:
                return expected == null || !actual.contains(expected);
            case GREATER_THAN:
            case LESS_THAN:
                return compareNumerically(actual, operator, expected);
            case MATCHES:
                return expected != null && Pattern.matches(expected, actual);
            case NOT_MATCHES:
                return expected == null || !Pattern.matches(expected, actual);
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + operator);
        }
    }

    private static boolean compareNumerically(String actualStr, Operator operator, String expectedStr) {
        try {
            BigDecimal actualNum = new BigDecimal(actualStr);
            BigDecimal expectedNum = new BigDecimal(expectedStr);
            int comparisonResult = actualNum.compareTo(expectedNum);

            if (operator == Operator.GREATER_THAN) {
                return comparisonResult > 0;
            } else if (operator == Operator.LESS_THAN) {
                return comparisonResult < 0;
            }
            return false;
        } catch (NumberFormatException e) {
            // If values are not numeric, comparison is false
            return false;
        }
    }
} 