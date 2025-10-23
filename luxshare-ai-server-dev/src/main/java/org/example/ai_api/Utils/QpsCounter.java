package org.example.ai_api.Utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统访问量计数器
 */
@Component
public class QpsCounter {
    private final AtomicInteger counter = new AtomicInteger(0);

    public void increment() {
        counter.incrementAndGet();
    }

    public int getAndReset() {
        return counter.getAndSet(0);
    }
}
