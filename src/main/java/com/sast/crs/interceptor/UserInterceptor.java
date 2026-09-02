package com.sast.crs.interceptor;

import com.sast.crs.annotation.CheckRole;
import com.sast.crs.annotation.PassToken;
import com.sast.crs.entity.User;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.UserMapper;
import com.sast.crs.util.CommonUtil;
import com.sast.crs.util.JwtUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class UserInterceptor implements HandlerInterceptor {
    public static ThreadLocal<User> userHolder = new ThreadLocal<>();

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public UserInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        if (!(handler instanceof HandlerMethod))
            return true;
        Method method = ((HandlerMethod) handler).getMethod();
        boolean isAnonymous = allowNoToken(method);
        String token = request.getHeader("Token");
        if (!StringUtils.hasLength(token)) {
            // 判断接口是否允许没有Token
            if (isAnonymous) return true;
            throw new LocalRuntimeException(ErrorEnum.NO_TOKEN);
        }

        User user = jwtUtil.getUser(token);
        if (user == null)
            throw new LocalRuntimeException(ErrorEnum.TOKEN_ERROR);
        // 登录过期
        if (jwtUtil.isExpired(user))
            throw new LocalRuntimeException(ErrorEnum.EXPIRED_LOGIN);
        User userFromDB = userMapper.selectById(user.getCode());
        // 判断是否是已知用户
        if (userFromDB == null)
            throw new LocalRuntimeException(ErrorEnum.EXPIRED_LOGIN);
        // 检查权限
        if (!isAnonymous && !checkPermission(method, userFromDB))
            throw new LocalRuntimeException(ErrorEnum.NO_ROLE);
        userHolder.set(userFromDB);
        // 更新redis中的token持续时间
        jwtUtil.reFreshToken(userFromDB);
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) {
        userHolder.remove();
    }

    private boolean allowNoToken(Method method) {
        PassToken passToken = CommonUtil.getAnnotation(method, PassToken.class);
        return passToken != null && passToken.required();
    }

    private boolean checkPermission(Method method, User user) {
        CheckRole checkRole = CommonUtil.getAnnotation(method, CheckRole.class);
        if (checkRole == null) return true;
        return user.getRole() >= checkRole.role().getRole();
    }
}
