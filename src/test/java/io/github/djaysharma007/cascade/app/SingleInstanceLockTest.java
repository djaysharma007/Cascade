package io.github.djaysharma007.cascade.app;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SingleInstanceLockTest {

    @Test
    void testSingleInstanceLockAcquisitionAndSignal() throws InterruptedException {
        AtomicBoolean signaled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        SingleInstanceLock primary = new SingleInstanceLock(() -> {
            signaled.set(true);
            latch.countDown();
        });

        boolean acquired = primary.tryAcquire();
        assertThat(acquired).isTrue();

        try {
            // Secondary instance should fail to acquire and signal primary
            SingleInstanceLock secondary = new SingleInstanceLock(() -> {});
            boolean secondaryAcquired = secondary.tryAcquire();
            assertThat(secondaryAcquired).isFalse();

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(signaled.get()).isTrue();
        } finally {
            primary.release();
        }
    }
}
