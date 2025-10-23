package org.example.ai_api.Interceptor;

import org.example.ai_api.Utils.ErrorCounter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 错误计数拦截器，用于统计接口调用错误次数
 */
@Component
public class ErrorCountInterceptor implements HandlerInterceptor {
    @Autowired
    private ErrorCounter errorCounter;

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, HttpServletResponse response, @NotNull Object handler, Exception ex) {
        int status = response.getStatus();
        if (status != 200) {
            errorCounter.increment();
        }
    }
}