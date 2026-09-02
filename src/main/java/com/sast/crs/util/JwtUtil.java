package com.sast.crs.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sast.crs.constant.RedisKeyConst;
import com.sast.crs.entity.User;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private int expiration;

    private final RedisUtil redisUtil;

    public JwtUtil(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 启动时校验 JWT 密钥已配置，避免运行时才发现签名密钥为空
     */
    @jakarta.annotation.PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET 未配置：请设置环境变量 JWT_SECRET（生产环境必须为随机长串），配置项说明见 README");
        }
    }

    public String generateToken(User user) {
        Calendar time = Calendar.getInstance();
        time.add(Calendar.HOUR, expiration);
        JWTCreator.Builder builder = JWT.create();
        builder.withClaim("code", user.getCode());
        builder.withExpiresAt(time.getTime());
        return builder.sign(Algorithm.HMAC256(secret));
    }

    public Map<String, Claim> getClaims(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getClaims();
        } catch (JWTVerificationException e) {
            log.info("Token验证错误：{}", e.getMessage());
            throw new LocalRuntimeException(ErrorEnum.TOKEN_ERROR);
        }
    }

    public User getUser(String token) {
        Map<String, Claim> claims = this.getClaims(token);
        User user = new User();
        user.setCode(claims.get("code").asString());
        return user;
    }

    public Boolean isExpired(User user) {
        Long expire = redisUtil.getExpire(RedisKeyConst.getTokenKey(user));
        return expire <= 0;
    }

    public void reFreshToken(User user) {
        redisUtil.expire(RedisKeyConst.getTokenKey(user), 1, TimeUnit.HOURS);
    }
}
