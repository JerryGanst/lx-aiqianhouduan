package org.example.ai_api.Utils;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 程序运行过程中的错误计数器
 * @author 10353965
 */
@Component
public class ErrorCounter {
    private final AtomicInteger errorCounter = new AtomicInteger(0);

    public void increment() {
        errorCounter.incrementAndGet();
    }

    public int getAndReset() {
        return errorCounter.getAndSet(0);
    }
}
