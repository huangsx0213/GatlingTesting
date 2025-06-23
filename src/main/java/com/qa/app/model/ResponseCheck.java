package com.qa.app.model;

public class ResponseCheck {
    private CheckType type;
    private String expression;
    private Operator operator;
    private String expect;
    private String saveAs;

    public ResponseCheck() {}

    public ResponseCheck(CheckType type, String expression, Operator operator, String expect, String saveAs) {
        this.type = type;
        this.expression = expression;
        this.operator = operator;
        this.expect = expect;
        this.saveAs = saveAs;
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
} 