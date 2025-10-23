package org.example.ai_api.Aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.example.ai_api.Utils.QpsCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 10353965
 */
@Aspect
@Component
public class QpsAspect {
    @Autowired
    private QpsCounter qpsCounter;

    /**
     * 拦截所有Controller方法
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    @Before("controllerMethods()")
    public void beforeController() {
        qpsCounter.increment();
    }
}
