package net.dorokhov.pony.common;

import org.junit.Test;

import java.util.Comparator;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionalComparatorsTests {

    @Test
    public void respectNullFirst() throws Exception {
        int result = OptionalComparators.<String>nullFirst().compare(Optional.of("foo"), Optional.empty());
        assertThat(result).isEqualTo(1);
    }
    
    @Test
    public void respectNullLast() throws Exception {
        int result = OptionalComparators.<String>nullLast().compare(Optional.of("foo"), Optional.empty());
        assertThat(result).isEqualTo(-1);
    }
    
    @Test
    public void compareNotNulls() throws Exception {
        int result;
        
        result = OptionalComparators.<String>nullFirst().compare(Optional.of("2"), Optional.of("1"));
        assertThat(result).isEqualTo(1);
        result = OptionalComparators.nullFirst(Comparator.<String>reverseOrder()).compare(Optional.of("2"), Optional.of("1"));
        assertThat(result).isEqualTo(-1);
        
        result = OptionalComparators.<String>nullLast().compare(Optional.of("2"), Optional.of("1"));
        assertThat(result).isEqualTo(1);
        result = OptionalComparators.nullLast(Comparator.<String>reverseOrder()).compare(Optional.of("2"), Optional.of("1"));
        assertThat(result).isEqualTo(-1);
    }
    
    @Test
    public void compareNulls() throws Exception {
        int result;
        result = OptionalComparators.<String>nullFirst().compare(Optional.empty(), Optional.empty());
        assertThat(result).isEqualTo(0);
        result = OptionalComparators.<String>nullLast().compare(Optional.empty(), Optional.empty());
        assertThat(result).isEqualTo(0);
    }
}
