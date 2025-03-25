package app.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect // Клас
@Component
public class LoggingAspect {

//    @After("execution(* app.web.IndexController.*(..))")
//    public void logIndexControllerMethods() {
//
//        System.out.println("Hey, another method in IndexController was executed!");
//    }

    // Advice - метод с доп. логика
    @After("bean(userController)")  // Pointcut - "regex" for methods
    public void logIndexControllerMethods(JoinPoint joinPoint) {

        System.out.println("Hey, another method in UserController was executed!");
    }

    @Around(value = "@annotation(app.aspect.VeryImportant)")
    public Object logVeryImportantMethodExecution(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        System.out.println("Before method execution!");
        Object methodResult = proceedingJoinPoint.proceed();
        System.out.println("After method execution!");

        return methodResult;
    }
}