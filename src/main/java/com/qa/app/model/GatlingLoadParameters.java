package com.qa.app.model;

import com.qa.app.model.threadgroups.*;

import java.io.Serializable;

public class GatlingLoadParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    private ThreadGroupType type = ThreadGroupType.STANDARD;
    private StandardThreadGroup standardThreadGroup;
    private SteppingThreadGroup steppingThreadGroup;
    private UltimateThreadGroup ultimateThreadGroup;

    public ThreadGroupType getType() {
        return type;
    }

    public void setType(ThreadGroupType type) {
        this.type = type;
    }

    public StandardThreadGroup getStandardThreadGroup() {
        return standardThreadGroup;
    }

    public void setStandardThreadGroup(StandardThreadGroup standardThreadGroup) {
        this.standardThreadGroup = standardThreadGroup;
    }

    public SteppingThreadGroup getSteppingThreadGroup() {
        return steppingThreadGroup;
    }

    public void setSteppingThreadGroup(SteppingThreadGroup steppingThreadGroup) {
        this.steppingThreadGroup = steppingThreadGroup;
    }

    public UltimateThreadGroup getUltimateThreadGroup() {
        return ultimateThreadGroup;
    }

    public void setUltimateThreadGroup(UltimateThreadGroup ultimateThreadGroup) {
        this.ultimateThreadGroup = ultimateThreadGroup;
    }
} 