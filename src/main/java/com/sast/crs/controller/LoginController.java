package com.sast.crs.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sast.crs.constant.RedisKeyConst;
import com.sast.crs.entity.User;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.UserMapper;
import com.sast.crs.util.JwtUtil;
import com.sast.crs.util.RedisUtil;
import com.sast.crs.util.SecureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
public class LoginController {
    public static final String LOGIN_VALIDATE_CODE = "VAL_CODE:";

    private JwtUtil jwtUtil;
    private RedisUtil redisUtil;
    private UserMapper userMapper;

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Autowired
    public void setRedisUtil(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 返回验证码图片，并将验证码存入Redis
     */
    @GetMapping("/getValidateCode")
    public void getImgValidateCode(HttpServletResponse response) {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");

        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.addHeader("CAPTCHA", uuid);
        response.setContentType("image/png");

        // 200x50、4位数字验证码，150条干扰线
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 50, 4, 150);
        // getCode() 会懒触发验证码生成（验证码文本 + 图片字节）
        String capText = captcha.getCode();

        try {
            // hutool 生成的图片为 PNG 格式，直接写原始字节流
            captcha.write(response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }

        redisUtil.set(LOGIN_VALIDATE_CODE + uuid, capText, 60 * 5);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestHeader("User-Agent") String agent, @RequestParam(defaultValue = "") String code, @RequestParam(defaultValue = "") String password, @RequestParam(defaultValue = "") String validateCode, @RequestHeader("CAPTCHA") String uuid) {
        //验证验证码
        String currentCode = (String) redisUtil.get(LOGIN_VALIDATE_CODE + uuid);
        if (currentCode == null) {
            throw new LocalRuntimeException("验证码失效");
        } else if (!currentCode.equals(validateCode)) {
            throw new LocalRuntimeException("验证码错误");
        }
        redisUtil.del(LOGIN_VALIDATE_CODE + uuid);

        if ("".equals(code) || "".equals(password)) {
            throw new LocalRuntimeException("学号或密码不能为空");
        }

        User userFromDB = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getCode, code));
        if (userFromDB == null) {
            throw new LocalRuntimeException("账号不存在");
        } else if (!SecureUtil.encryptMD5(password).equals(userFromDB.getPassword())) {
            throw new LocalRuntimeException("密码错误");
        }

        String token = jwtUtil.generateToken(userFromDB);
        Map<String, Object> map = new HashMap<>();
        map.put("name", userFromDB.getName());
        map.put("depId", userFromDB.getDepId());
        map.put("role", userFromDB.getRole());
        map.put("token", token);

        log.info("===============================================");
        log.info("用户登录：{}，role：{}", userFromDB.getName(), userFromDB.getRole());
        log.info("登录Agent:{}", agent);
        log.info("===============================================");

        //用redis中的过期时间代替JWT的过期时间，每次经过拦截器时更新过期时间
        redisUtil.set(RedisKeyConst.getTokenKey(userFromDB), token, 1, TimeUnit.HOURS);
        return map;
    }
}
