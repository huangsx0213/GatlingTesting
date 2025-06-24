package com.qa.app.model;

public class ResponseCheck {
    private CheckType type;
    private String expression;
    private Operator operator;
    private String expect;
    private String saveAs;
    // If true, this check becomes optional (non-fatal) in Gatling
    private boolean optional;
    private String actual;

    public ResponseCheck() {
        this.optional = false;
        this.actual = null;
    }

    public ResponseCheck(CheckType type, String expression, Operator operator, String expect, String saveAs) {
        this(type, expression, operator, expect, saveAs, false);
    }

    public ResponseCheck(CheckType type, String expression, Operator operator, String expect, String saveAs, boolean optional) {
        this.type = type;
        this.expression = expression;
        this.operator = operator;
        this.expect = expect;
        this.saveAs = saveAs;
        this.optional = optional;
        this.actual = null;
    }

    public CheckType getType() {
        return type;
    }

    public void setType(CheckType type) {
        this.type = type;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getExpect() {
        return expect;
    }

    public void setExpect(String expect) {
        this.expect = expect;
    }

    public String getSaveAs() {
        return saveAs;
    }

    public void setSaveAs(String saveAs) {
        this.saveAs = saveAs;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }
} 