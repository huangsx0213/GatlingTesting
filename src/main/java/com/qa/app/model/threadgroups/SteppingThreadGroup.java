package com.qa.app.model.threadgroups;

import java.io.Serializable;

public class SteppingThreadGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private int numThreads = 1;
    private int initialDelay = 0;
    private int startUsers = 0;
    private int incrementUsers = 1;
    private int incrementTime = 30;
    private int holdLoad = 60;
    private int threadLifetime = 60;

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public int getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(int initialDelay) {
        this.initialDelay = initialDelay;
    }

    public int getStartUsers() {
        return startUsers;
    }

    public void setStartUsers(int startUsers) {
        this.startUsers = startUsers;
    }

    public int getIncrementUsers() {
        return incrementUsers;
    }

    public void setIncrementUsers(int incrementUsers) {
        this.incrementUsers = incrementUsers;
    }

    public int getIncrementTime() {
        return incrementTime;
    }

    public void setIncrementTime(int incrementTime) {
        this.incrementTime = incrementTime;
    }

    public int getHoldLoad() {
        return holdLoad;
    }

    public void setHoldLoad(int holdLoad) {
        this.holdLoad = holdLoad;
    }

    public int getThreadLifetime() {
        return threadLifetime;
    }

    public void setThreadLifetime(int threadLifetime) {
        this.threadLifetime = threadLifetime;
    }
} 