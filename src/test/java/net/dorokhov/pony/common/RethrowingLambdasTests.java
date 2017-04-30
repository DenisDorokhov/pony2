package net.dorokhov.pony.common;

import org.junit.Test;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RethrowingLambdasTests {

    @Test
    public void rethrowConsumer() throws Exception {
        assertThatThrownBy(() -> rethrow((RethrowingLambdas.ThrowingConsumer<Object>) value -> { throw new Exception(); })
                .accept("value")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void rethrowBiConsumer() throws Exception {
        assertThatThrownBy(() -> rethrow((value1, value2) -> { throw new Exception(); })
                .accept("value1", "value2")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void rethrowFunction() throws Exception {
        assertThatThrownBy(() -> rethrow((RethrowingLambdas.ThrowingFunction<Object, Object>) value -> { throw new Exception(); })
                .apply("value")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void rethrowSupplier() throws Exception {
        assertThatThrownBy(() -> rethrow(() -> { throw new Exception(); })
                .get()).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void rethrowUnaryOperator() throws Exception {
        assertThatThrownBy(() -> rethrow((RethrowingLambdas.ThrowingUnaryOperator<Object>) value -> { throw new Exception(); })
                .apply("value")).isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void rethrowRunnable() throws Exception {
        assertThatThrownBy(() -> rethrow((RethrowingLambdas.ThrowingRunnable) () -> { throw new Exception(); })
                .run()).isInstanceOf(RuntimeException.class);
    }
}
