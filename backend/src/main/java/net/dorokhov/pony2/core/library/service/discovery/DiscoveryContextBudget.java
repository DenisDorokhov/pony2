package net.dorokhov.pony2.core.library.service.discovery;

public class DiscoveryContextBudget {

    private final double limitRatio;
    private final double triggerRatio;
    private int contextSize;

    public DiscoveryContextBudget(double limitRatio, double triggerRatio) {
        this.limitRatio = limitRatio;
        this.triggerRatio = triggerRatio;
    }

    public int getContextSize() {
        return contextSize;
    }

    public void setContextSize(int contextSize) {
        this.contextSize = contextSize;
    }

    public double getLimitRatio() {
        return limitRatio;
    }

    public double getTriggerRatio() {
        return triggerRatio;
    }
}
