package net.dorokhov.pony.common;

import java.util.Comparator;
import java.util.Optional;

/**
 * Based on https://gist.github.com/mkrueske/aa0d4502c45cbc15a316.
 */
public final class OptionalComparators {

    private OptionalComparators() {
    }

    public static <T extends Comparable<T>> Comparator<Optional<T>> nullFirst() {
        return nullFirst(Comparator.<T>naturalOrder());
    }

    public static <T extends Comparable<T>> Comparator<Optional<T>> nullFirst(Comparator<T> comparator) {
        return Comparator.comparing(x -> x.orElse(null), Comparator.nullsFirst(comparator));
    }

    public static <T extends Comparable<T>> Comparator<Optional<T>> nullLast() {
        return nullLast(Comparator.<T>naturalOrder());
    }

    public static <T extends Comparable<T>> Comparator<Optional<T>> nullLast(Comparator<T> comparator) {
        return Comparator.comparing(x -> x.orElse(null), Comparator.nullsLast(comparator));
    }
}
