package com.sast.crs.aop;

import com.sast.crs.annotation.OperateLog;
import com.sast.crs.annotation.PassToken;
import com.sast.crs.entity.User;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.interceptor.RequestInterceptor;
import com.sast.crs.interceptor.UserInterceptor;
import com.sast.crs.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Aspect
@Component
public class OperateAspect {
    private final static Set<String> EXCLUDE_SET;

    static {
        EXCLUDE_SET = new HashSet<>();
        EXCLUDE_SET.add("password");
    }

    @Pointcut("@annotation(com.sast.crs.annotation.OperateLog)")
    public void operateLog() {
    }

    @Around("operateLog()&&@annotation(logAnno)")
    public Object aroundMethod(ProceedingJoinPoint proceedingJoinPoint, OperateLog logAnno) throws Throwable {
        Map<String, Object> paramMap = CommonUtil.getRequestParamMap(proceedingJoinPoint, EXCLUDE_SET);
        Object returnValue = proceedingJoinPoint.proceed();
        try {
            User user = needToken(proceedingJoinPoint)
                    ? Optional.ofNullable(UserInterceptor.userHolder.get()).orElseThrow(() -> new LocalRuntimeException(ErrorEnum.NO_LOGIN))
                    : UserInterceptor.userHolder.get();

            Optional.ofNullable(RequestInterceptor.requestHolder.get()).ifPresent((preTrack) -> {
                preTrack.setSpendTime(System.currentTimeMillis() - Long.parseLong(preTrack.getSpendTime()) + "ms")
                        .setDescription(logAnno.value())
                        .setParams(paramMap)
                        .setResult(returnValue)
                        .setUser(user);
                log.info(preTrack.toLogFormat(true));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }

    @AfterThrowing(pointcut = "operateLog()&&@annotation(logAnno)", throwing = "exception")
    public void throwHandler(JoinPoint joinPoint, OperateLog logAnno, Throwable exception) {
        Map<String, Object> paramMap = CommonUtil.getRequestParamMap(joinPoint, EXCLUDE_SET);
        try {
            User user = needToken(joinPoint)
                    ? Optional.ofNullable(UserInterceptor.userHolder.get()).orElseThrow(() -> new LocalRuntimeException(ErrorEnum.NO_LOGIN))
                    : UserInterceptor.userHolder.get();

            Optional.ofNullable(RequestInterceptor.requestHolder.get()).ifPresent(preTrack -> {
                preTrack.setSpendTime(System.currentTimeMillis() - Long.parseLong(preTrack.getSpendTime()) + "ms")
                        .setDescription(logAnno.value())
                        .setParams(paramMap)
                        .setResult(exception)
                        .setUser(user);
                log.info(preTrack.toLogFormat(false));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean needToken(@NotNull JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        PassToken passToken = CommonUtil.getAnnotation(signature.getMethod(), PassToken.class);
        return passToken == null || !passToken.required();
    }
}
