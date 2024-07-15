package net.dorokhov.pony3.common;

import net.dorokhov.pony3.common.RethrowingLambdas.ThrowingConsumer;
import net.dorokhov.pony3.common.RethrowingLambdas.ThrowingFunction;
import net.dorokhov.pony3.common.RethrowingLambdas.ThrowingRunnable;
import net.dorokhov.pony3.common.RethrowingLambdas.ThrowingUnaryOperator;
import org.junit.jupiter.api.Test;

import static net.dorokhov.pony3.common.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RethrowingLambdasTest {

    @Test
    public void shouldRethrowConsumer() {
        assertThatThrownBy(() -> rethrow((ThrowingConsumer<Object>) value -> { throw new Exception(); })
                .accept("value")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowBiConsumer() {
        assertThatThrownBy(() -> rethrow((value1, value2) -> { throw new Exception(); })
                .accept("value1", "value2")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowFunction() {
        assertThatThrownBy(() -> rethrow((ThrowingFunction<Object, Object>) value -> { throw new Exception(); })
                .apply("value")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowSupplier() {
        assertThatThrownBy(() -> rethrow(() -> { throw new Exception(); })
                .get()).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowUnaryOperator() {
        assertThatThrownBy(() -> rethrow((ThrowingUnaryOperator<Object>) value -> { throw new Exception(); })
                .apply("value")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowRunnable() {
        assertThatThrownBy(() -> rethrow((ThrowingRunnable) () -> { throw new Exception(); })
                .run()).isInstanceOf(RuntimeException.class);
    }
}
