package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.ScanProgress;
import net.dorokhov.pony.api.library.domain.ScanType;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

public final class ScanProgressDto {
    
    public static final class Step {
        
        private final ScanProgress.Step code;
        private final ScanType scanType;
        private final int stepNumber;
        private final int totalSteps;

        Step(ScanProgress.Step code, ScanType scanType, int stepNumber, int totalSteps) {
            this.code = code;
            this.scanType = scanType;
            this.stepNumber = stepNumber;
            this.totalSteps = totalSteps;
        }

        public ScanProgress.Step getCode() {
            return code;
        }

        public ScanType getScanType() {
            return scanType;
        }

        public int getStepNumber() {
            return stepNumber;
        }

        public int getTotalSteps() {
            return totalSteps;
        }
        
        public static Step of(ScanProgress.Step step) {
            return new Step(step, step.getScanType(), step.getStepNumber(), step.getTotalSteps());
        }
    }
    
    public static final class Value {

        private final long itemsComplete;
        private final long itemsTotal;

        Value(long itemsComplete, long itemsTotal) {
            this.itemsComplete = itemsComplete;
            this.itemsTotal = itemsTotal;
        }

        public long getItemsComplete() {
            return itemsComplete;
        }

        public long getItemsTotal() {
            return itemsTotal;
        }
        
        public static Value of(ScanProgress.Value value) {
            return new Value(value.getItemsComplete(), value.getItemsTotal());
        }
    }

    private final Step step;
    private final List<String> files;
    private final Value value;

    private ScanProgressDto(Step step, List<String> files, @Nullable Value value) {
        this.step = checkNotNull(step);
        this.files = unmodifiableList(files);
        this.value = value;
    }

    public Step getStep() {
        return step;
    }

    public List<String> getFiles() {
        return files;
    }

    public Value getValue() {
        return value;
    }

    public static ScanProgressDto of(ScanProgress scanProgress) {
        return new ScanProgressDto(Step.of(scanProgress.getStep()), 
                scanProgress.getFiles().stream()
                        .map(File::getAbsolutePath)
                        .collect(Collectors.toList()),
                scanProgress.getValue() != null ? Value.of(scanProgress.getValue()) : null);
    }
}
