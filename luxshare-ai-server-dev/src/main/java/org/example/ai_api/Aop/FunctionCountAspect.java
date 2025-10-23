package org.example.ai_api.Aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.ai_api.Annotation.FunctionCount;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 10353965
 */
@Aspect
@Component
public class FunctionCountAspect {

    /**
     * 线程安全的功能计数器
     */
    private final ConcurrentHashMap<String, AtomicInteger> functionCounter = new ConcurrentHashMap<>();

    /**
     * 切点：拦截所有带FunctionCount注解的方法
     */
    @Pointcut("@annotation(functionCount)")
    public void functionCountPointcut(FunctionCount functionCount) {}

    /**
     * 统计功能被调用的次数
     */
    @After(value = "functionCountPointcut(functionCount)", argNames = "joinPoint,functionCount")
    public void afterFunctionCall(JoinPoint joinPoint, FunctionCount functionCount) {
        StringBuilder functionName = new StringBuilder(functionCount.value());
        // 针对知识库问答，按type区分计数
        if ("知识库问答".contentEquals(functionName)) {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg != null && arg.getClass().getSimpleName().equals("Query")) {
                    try {
                        // 反射获取type字段
                        java.lang.reflect.Method getTypeMethod = arg.getClass().getMethod("getType");
                        Object typeValue = getTypeMethod.invoke(arg);
                        if (typeValue != null) {
                            functionName.append("-").append(typeValue);
                        }
                    } catch (Exception e) {
                        // 忽略异常，保持原功能名
                    }
                    break;
                }
            }
        }
        functionCounter.computeIfAbsent(functionName.toString(), k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 获取并重置所有功能的计数（用于定时任务）
     */
    public ConcurrentHashMap<String, Integer> getAndResetAllCounts() {
        ConcurrentHashMap<String, Integer> snapshot = new ConcurrentHashMap<>();
        functionCounter.forEach((key, value) -> snapshot.put(key, value.getAndSet(0)));
        return snapshot;
    }
}
