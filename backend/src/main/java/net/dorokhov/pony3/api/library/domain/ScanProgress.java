package net.dorokhov.pony3.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;
import static net.dorokhov.pony3.api.library.domain.ScanType.EDIT;
import static net.dorokhov.pony3.api.library.domain.ScanType.FULL;

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
            return MoreObjects.toStringHelper(this)
                    .add("name", name())
                    .add("scanType", scanType)
                    .add("stepNumber", stepNumber)
                    .add("totalSteps", totalSteps)
                    .toString();
        }
    }
    
    public static final class Value {

        private final long itemsComplete;
        private final long itemsTotal;

        public Value(long itemsComplete, long itemsTotal) {
            this.itemsComplete = itemsComplete;
            this.itemsTotal = itemsTotal;
        }

        public long getItemsComplete() {
            return itemsComplete;
        }

        public long getItemsTotal() {
            return itemsTotal;
        }

        @Override
        @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Value that = (Value) obj;
            return Objects.equal(itemsComplete, that.itemsComplete) &&
                    Objects.equal(itemsTotal, that.itemsTotal);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(itemsComplete, itemsTotal);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("itemsComplete", itemsComplete)
                    .add("itemsTotal", itemsTotal)
                    .toString();
        }

        public static Value of(long itemsComplete, long itemsTotal) {
            return new Value(itemsComplete, itemsTotal);
        }
    }

    private final Step step;
    private final List<File> files;
    private final Value value;

    public ScanProgress(Step step, List<File> files, @Nullable Value value) {
        this.step = checkNotNull(step);
        this.files = unmodifiableList(files);
        this.value = value;
    }

    public Step getStep() {
        return step;
    }

    public List<File> getFiles() {
        return files;
    }

    @Nullable
    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("step", step)
                .add("files", files)
                .add("value", value)
                .toString();
    }
}
