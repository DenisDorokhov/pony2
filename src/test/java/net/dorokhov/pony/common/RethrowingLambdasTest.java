package net.dorokhov.pony.common;

import net.dorokhov.pony.common.RethrowingLambdas.ThrowingConsumer;
import net.dorokhov.pony.common.RethrowingLambdas.ThrowingFunction;
import net.dorokhov.pony.common.RethrowingLambdas.ThrowingRunnable;
import net.dorokhov.pony.common.RethrowingLambdas.ThrowingUnaryOperator;
import org.junit.Test;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RethrowingLambdasTest {

    @Test
    public void shouldRethrowConsumer() throws Exception {
        assertThatThrownBy(() -> rethrow((ThrowingConsumer<Object>) value -> { throw new Exception(); })
                .accept("value")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowBiConsumer() throws Exception {
        assertThatThrownBy(() -> rethrow((value1, value2) -> { throw new Exception(); })
                .accept("value1", "value2")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowFunction() throws Exception {
        assertThatThrownBy(() -> rethrow((ThrowingFunction<Object, Object>) value -> { throw new Exception(); })
                .apply("value")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowSupplier() throws Exception {
        assertThatThrownBy(() -> rethrow(() -> { throw new Exception(); })
                .get()).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowUnaryOperator() throws Exception {
        assertThatThrownBy(() -> rethrow((ThrowingUnaryOperator<Object>) value -> { throw new Exception(); })
                .apply("value")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void shouldRethrowRunnable() throws Exception {
        assertThatThrownBy(() -> rethrow((ThrowingRunnable) () -> { throw new Exception(); })
                .run()).isInstanceOf(RuntimeException.class);
    }
}
