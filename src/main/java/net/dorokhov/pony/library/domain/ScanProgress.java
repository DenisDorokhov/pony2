package net.dorokhov.pony.library.domain;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.dorokhov.pony.library.domain.ScanType.EDIT;
import static net.dorokhov.pony.library.domain.ScanType.FULL;

public final class ScanProgress {

    public enum Step {

        FULL_PREPARING(FULL, 0, 6),
        FULL_SEARCHING_MEDIA(FULL, 1, 6),
        FULL_CLEANING_SONGS(FULL, 2, 6),
        FULL_CLEANING_ARTWORKS(FULL, 3, 6),
        FULL_IMPORTING(FULL, 4, 6),
        FULL_SEARCHING_ARTWORKS(FULL, 5, 6),

        EDIT_PREPARING(EDIT, 0, 3),
        EDIT_WRITING(EDIT, 1, 3),
        EDIT_SEARCHING_ARTWORKS(EDIT, 2, 3);

        private final ScanType scanType;
        private final int stepNumber;
        private final int totalSteps;

        Step(ScanType scanType, int stepNumber, int totalSteps) {
            this.scanType = checkNotNull(scanType);
            this.stepNumber = stepNumber;
            this.totalSteps = totalSteps;
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

        @Override
        public String toString() {
            return "Step{" +
                    "name=" + name() +
                    ", scanType=" + scanType +
                    ", stepNumber=" + stepNumber +
                    ", totalSteps=" + totalSteps +
                    '}';
        }
    }

    private final Step step;
    private final List<File> files;
    private final double value;

    public ScanProgress(Step step, List<File> files, double value) {
        this.step = checkNotNull(step);
        this.files = ImmutableList.copyOf(files);
        this.value = value;
    }

    public Step getStep() {
        return step;
    }

    public List<File> getFiles() {
        return files;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ScanProgress{" +
                "step=" + step +
                ", files=" + files +
                ", value=" + value +
                '}';
    }
}
