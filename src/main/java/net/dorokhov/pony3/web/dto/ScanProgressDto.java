package net.dorokhov.pony3.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony3.api.library.domain.ScanProgress;
import net.dorokhov.pony3.api.library.domain.ScanType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ScanProgressDto {
    
    public static final class Step {
        
        private ScanProgress.Step code;
        private ScanType scanType;
        private int stepNumber;
        private int totalSteps;

        public ScanProgress.Step getCode() {
            return code;
        }

        public Step setCode(ScanProgress.Step code) {
            this.code = code;
            return this;
        }

        public ScanType getScanType() {
            return scanType;
        }

        public Step setScanType(ScanType scanType) {
            this.scanType = scanType;
            return this;
        }

        public int getStepNumber() {
            return stepNumber;
        }

        public Step setStepNumber(int stepNumber) {
            this.stepNumber = stepNumber;
            return this;
        }

        public int getTotalSteps() {
            return totalSteps;
        }

        public Step setTotalSteps(int totalSteps) {
            this.totalSteps = totalSteps;
            return this;
        }

        public static Step of(ScanProgress.Step step) {
            return new Step()
                    .setCode(step)
                    .setScanType(step.getScanType())
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

        public static Value of(ScanProgress.Value value) {
            return new Value()
                    .setItemsComplete(value.getItemsComplete())
                    .setItemsTotal(value.getItemsTotal());
        }
    }

    private Step step;
    private List<String> files = new ArrayList<>();
    private Value value;

    public Step getStep() {
        return step;
    }

    public ScanProgressDto setStep(Step step) {
        this.step = step;
        return this;
    }

    public List<String> getFiles() {
        if (files == null) {
            files = new ArrayList<>();
        }
        return files;
    }

    public ScanProgressDto setFiles(List<String> files) {
        this.files = files;
        return this;
    }

    @Nullable
    public Value getValue() {
        return value;
    }

    public ScanProgressDto setValue(@Nullable Value value) {
        this.value = value;
        return this;
    }

    public static ScanProgressDto of(ScanProgress scanProgress) {
        return new ScanProgressDto()
                .setStep(Step.of(scanProgress.getStep()))
                .setFiles(scanProgress.getFiles().stream()
                        .map(File::getAbsolutePath)
                        .collect(Collectors.toList()))
                .setValue(scanProgress.getValue() != null ? Value.of(scanProgress.getValue()) : null);
    }
}
