package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryProgress;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;

public final class DiscoveryProgressDto {

    public static final class StepDescriptor {

        private DiscoveryProgress.Step step;
        private DiscoveryType discoveryType;
        private int stepNumber;
        private int totalSteps;

        public DiscoveryProgress.Step getStep() {
            return step;
        }

        public StepDescriptor setStep(DiscoveryProgress.Step step) {
            this.step = step;
            return this;
        }

        public DiscoveryType getDiscoveryType() {
            return discoveryType;
        }

        public StepDescriptor setDiscoveryType(DiscoveryType discoveryType) {
            this.discoveryType = discoveryType;
            return this;
        }

        public int getStepNumber() {
            return stepNumber;
        }

        public StepDescriptor setStepNumber(int stepNumber) {
            this.stepNumber = stepNumber;
            return this;
        }

        public int getTotalSteps() {
            return totalSteps;
        }

        public StepDescriptor setTotalSteps(int totalSteps) {
            this.totalSteps = totalSteps;
            return this;
        }

        public static StepDescriptor of(DiscoveryProgress.Step step) {
            return new StepDescriptor()
                    .setStep(step)
                    .setDiscoveryType(step.getDiscoveryType())
                    .setStepNumber(step.getStepNumber())
                    .setTotalSteps(step.getTotalSteps());
        }
    }

    public static final class Value {

        private long itemsComplete;
        private long itemsTotal;

        public long getItemsComplete() {
            return itemsComplete;
        }

        public Value setItemsComplete(long itemsComplete) {
            this.itemsComplete = itemsComplete;
            return this;
        }

        public long getItemsTotal() {
            return itemsTotal;
        }

        public Value setItemsTotal(long itemsTotal) {
            this.itemsTotal = itemsTotal;
            return this;
        }

        public static Value of(DiscoveryProgress.Value value) {
            return new Value()
                    .setItemsComplete(value.getItemsComplete())
                    .setItemsTotal(value.getItemsTotal());
        }
    }

    private StepDescriptor stepDescriptor;
    private Value value;

    public StepDescriptor getStepDescriptor() {
        return stepDescriptor;
    }

    public DiscoveryProgressDto setStepDescriptor(StepDescriptor stepDescriptor) {
        this.stepDescriptor = stepDescriptor;
        return this;
    }

    @Nullable
    public Value getValue() {
        return value;
    }

    public DiscoveryProgressDto setValue(@Nullable Value value) {
        this.value = value;
        return this;
    }

    public static DiscoveryProgressDto of(DiscoveryProgress discoveryProgress) {
        return new DiscoveryProgressDto()
                .setStepDescriptor(StepDescriptor.of(discoveryProgress.getStep()))
                .setValue(discoveryProgress.getValue() != null ? Value.of(discoveryProgress.getValue()) : null);
    }
}
