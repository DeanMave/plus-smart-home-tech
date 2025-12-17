package ru.yandex.practicum.commerce.shopping.store.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("@annotation(Loggable)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.debug("Вызов метода: {}", methodName);
        log.debug("Параметры: {}", args);

        Object result = joinPoint.proceed();

        log.debug("Завершение метода: {} - Результат: {}", methodName, result);
        return result;
    }
}