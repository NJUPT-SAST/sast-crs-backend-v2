package com.sast.crs.util;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.DigestUtils;

public class SecureUtil {
    /**
     * MD5 加密
     * @param str 需要加密的内容
     * @return 加密之后的内容
     */
    @NotNull
    public static String encryptMD5(@NotNull String str) {
        return DigestUtils.md5DigestAsHex(str.getBytes());
    }
}
