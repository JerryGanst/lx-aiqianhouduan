package org.example.ai_api.Service.Apis.Commons;

import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流的订阅管理与sse事件的构造
 * @author 10353965
 */
@Component
public class StreamHub {

    private static final Logger logger = LoggerFactory.getLogger(StreamHub.class);
    //流预占位符
    private static final Subscription PLACEHOLDER_SUBSCRIPTION = new Subscription() {
        @Override
        public void request(long n) {}
        @Override
        public void cancel() {}
    };
    //流取消占位符
    private static final Subscription CANCEL_PLACEHOLDER = new Subscription() {
        @Override
        public void request(long n) {}
        @Override
        public void cancel() {}
    };
    //流订阅管理表
    private final Map<String, Subscription> subscriptionMap ;

    public StreamHub() {
        this.subscriptionMap = new ConcurrentHashMap<>();
    }

    //为流生成唯一标识
    public String keyOf(String userId, String sessionId) {
        logger.info("生成流唯一标识  userId: {} sessionId: {}", userId, sessionId);
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (sessionId == null) {
            sessionId = "";
        }
        return userId + ":" + sessionId;
    }

    //流的预占位
    public void placeHolder(String key) {
        logger.info("预占位流  key: {}", key);
        subscriptionMap.putIfAbsent(key, PLACEHOLDER_SUBSCRIPTION);
    }

    //流的真实绑定
    public void bind(String key, Subscription subscription) {
        logger.info("绑定流  key: {}", key);
        Objects.requireNonNull(subscription);
        subscriptionMap.compute(key,(k , old)->{
            if(old == CANCEL_PLACEHOLDER){
                //当前流已被取消，取消新流并不入表
                subscription.cancel();
                return old;
            }
            if(old != null && old != PLACEHOLDER_SUBSCRIPTION&& old != subscription) {
                old.cancel();
            }
            return subscription;
        });
    }

    //流的取消
    public boolean cancel(String key) {
        logger.info("取消流  key: {}", key);
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        subscriptionMap.compute(key, (k, old) -> {
            if (old == null){
                return CANCEL_PLACEHOLDER;
            }
            if(old != PLACEHOLDER_SUBSCRIPTION&& old != CANCEL_PLACEHOLDER){
                old.cancel();
                cancelled.set(true);
            }
            return CANCEL_PLACEHOLDER;
        });
        return cancelled.get();
    }

    //流的清理，确保流的订阅被移除
    public boolean release(String key) {
        logger.info("清理流  key: {}", key);
        Subscription removed = subscriptionMap.remove(key);
        return removed != null;
    }

    //判断流是否被订阅
    public  boolean isSubscribed(String key) {
        Subscription subscription = subscriptionMap.get(key);
        return subscription != null && subscription != PLACEHOLDER_SUBSCRIPTION  && subscription != CANCEL_PLACEHOLDER;
    }
}
