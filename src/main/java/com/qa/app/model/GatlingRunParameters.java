package com.qa.app.model;

public class GatlingRunParameters {
    private final int users;
    private final int rampUp;
    private final int repetitions;

    public GatlingRunParameters(int users, int rampUp, int repetitions) {
        this.users = users;
        this.rampUp = rampUp;
        this.repetitions = repetitions;
    }

    public int getUsers() {
        return users;
    }

    public int getRampUp() {
        return rampUp;
    }

    public int getRepetitions() {
        return repetitions;
    }
} 