package com.qa.app.model.threadgroups;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.Serializable;

public class UltimateThreadGroupStep implements Serializable {
    private static final long serialVersionUID = 1L;

    private final IntegerProperty startTime = new SimpleIntegerProperty();
    private final IntegerProperty initialLoad = new SimpleIntegerProperty();
    private final IntegerProperty startupTime = new SimpleIntegerProperty();
    private final IntegerProperty holdTime = new SimpleIntegerProperty();
    private final IntegerProperty shutdownTime = new SimpleIntegerProperty();

    public UltimateThreadGroupStep(int startTime, int initialLoad, int startupTime, int holdTime, int shutdownTime) {
        setStartTime(startTime);
        setInitialLoad(initialLoad);
        setStartupTime(startupTime);
        setHoldTime(holdTime);
        setShutdownTime(shutdownTime);
    }

    // Default constructor for UI
    public UltimateThreadGroupStep() {
        this(0, 10, 30, 60, 10);
    }

    public int getStartTime() {
        return startTime.get();
    }

    public void setStartTime(int startTime) {
        this.startTime.set(startTime);
    }

    public IntegerProperty startTimeProperty() {
        return startTime;
    }

    public int getInitialLoad() {
        return initialLoad.get();
    }

    public void setInitialLoad(int initialLoad) {
        this.initialLoad.set(initialLoad);
    }

    public IntegerProperty initialLoadProperty() {
        return initialLoad;
    }

    public int getStartupTime() {
        return startupTime.get();
    }

    public void setStartupTime(int startupTime) {
        this.startupTime.set(startupTime);
    }

    public IntegerProperty startupTimeProperty() {
        return startupTime;
    }

    public int getHoldTime() {
        return holdTime.get();
    }

    public void setHoldTime(int holdTime) {
        this.holdTime.set(holdTime);
    }

    public IntegerProperty holdTimeProperty() {
        return holdTime;
    }

    public int getShutdownTime() {
        return shutdownTime.get();
    }

    public void setShutdownTime(int shutdownTime) {
        this.shutdownTime.set(shutdownTime);
    }

    public IntegerProperty shutdownTimeProperty() {
        return shutdownTime;
    }
} 