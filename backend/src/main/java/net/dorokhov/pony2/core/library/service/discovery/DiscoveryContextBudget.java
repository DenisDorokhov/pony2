package net.dorokhov.pony2.core.library.service.discovery;

public class DiscoveryContextBudget {

    private final double limitRatio;
    private final double triggerRatio;
    private final double targetRatio;

    private int contextSize;
    private int compactionFragmentBudget;
    private boolean compactionTargetCheckPending;

    public DiscoveryContextBudget(double limitRatio, double triggerRatio, double targetRatio) {
        this.limitRatio = limitRatio;
        this.triggerRatio = triggerRatio;
        this.targetRatio = targetRatio;
    }

    public int getContextSize() {
        return contextSize;
    }

    public void setContextSize(int contextSize) {
        this.contextSize = contextSize;
    }

    public int getLimitTokens() {
        return (int) (contextSize * limitRatio);
    }

    public int getTriggerTokens() {
        return (int) (contextSize * triggerRatio);
    }

    public int getTargetTokens() {
        return (int) (contextSize * targetRatio);
    }

    public int getCompactionFragmentBudget() {
        return compactionFragmentBudget;
    }

    public void initializeCompactionFragmentBudget(int fragmentBudgetDivisor) {
        if (compactionFragmentBudget == 0) {
            compactionFragmentBudget = Math.max(1, getTargetTokens() / fragmentBudgetDivisor);
        }
    }

    public boolean reduceCompactionFragmentBudget() {
        if (compactionFragmentBudget <= 1) {
            return false;
        }
        compactionFragmentBudget /= 2;
        return true;
    }

    public boolean isCompactionTargetCheckPending() {
        return compactionTargetCheckPending;
    }

    public void markCompactionTargetChecked() {
        compactionTargetCheckPending = false;
    }

    public void markCompactionApplied() {
        compactionTargetCheckPending = true;
    }
}
