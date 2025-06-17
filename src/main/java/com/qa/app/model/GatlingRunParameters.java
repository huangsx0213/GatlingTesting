package com.qa.app.model;

public class GatlingRunParameters {
    private int users;
    private int rampUp;
    private int repetitions;

    // A no-argument constructor is required for Jackson deserialization
    public GatlingRunParameters() {
    }

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