package com.sast.crs.interceptor;

import com.sast.crs.pojo.TraceLog;
import com.sast.crs.util.CommonUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

@Component
public class RequestInterceptor implements HandlerInterceptor {
    public static ThreadLocal<TraceLog> requestHolder = new ThreadLocal<>();

    /**
     * 每次收到请求时，记录该次请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        // MDC机制 https://www.jianshu.com/p/1dea7479eb07
        MDC.put("TRACE_ID", UUID.randomUUID().toString());
        TraceLog preTraceLog = new TraceLog().setEnv("PC").setSpendTime(System.currentTimeMillis() + "").setUrl(request.getRequestURI()).setUserAgent(CommonUtil.getUserAgent(request));
        requestHolder.set(preTraceLog);
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, @Nullable Exception ex) {
        requestHolder.remove();
        MDC.clear();
    }
}
