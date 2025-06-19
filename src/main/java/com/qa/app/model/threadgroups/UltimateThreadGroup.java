package com.qa.app.model.threadgroups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UltimateThreadGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<UltimateThreadGroupStep> steps = new ArrayList<>();

    public List<UltimateThreadGroupStep> getSteps() {
        return steps;
    }

    public void setSteps(List<UltimateThreadGroupStep> steps) {
        this.steps = steps;
    }
} 