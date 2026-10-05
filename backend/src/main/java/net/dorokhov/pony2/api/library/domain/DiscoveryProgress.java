package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryType.ALBUM;
import static net.dorokhov.pony2.api.library.domain.DiscoveryType.ARTIST;
import static net.dorokhov.pony2.api.library.domain.DiscoveryType.FULL;

public final class DiscoveryProgress {

    public enum Step {

        FULL_ARTIST_DISCOVERY(FULL, 0, 2),
        FULL_ALBUM_DISCOVERY(FULL, 1, 2),

        ARTIST_DISCOVERY(ARTIST, 0, 1),

        ALBUM_DISCOVERY(ALBUM, 0, 1),
        ;

        private final DiscoveryType discoveryType;
        private final int stepNumber;
        private final int totalSteps;

        Step(DiscoveryType discoveryType, int stepNumber, int totalSteps) {
            this.discoveryType = checkNotNull(discoveryType);
            this.stepNumber = stepNumber;
            this.totalSteps = totalSteps;
        }

        public DiscoveryType getDiscoveryType() {
            return discoveryType;
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
                    .add("discoveryType", discoveryType)
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
    private final Value value;

    public DiscoveryProgress(Step step, @Nullable Value value) {
        this.step = checkNotNull(step);
        this.value = value;
    }

    public Step getStep() {
        return step;
    }

    @Nullable
    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("step", step)
                .add("value", value)
                .toString();
    }
}
