package com.sast.crs.constant;

import com.sast.crs.entity.User;

public class RedisKeyConst {
    public static String getTokenKey(User user) {
        return "TOKEN:" + user.getCode();
    }

    /**
     * 获取文件缓存数据的Key
     * 不包含前缀
     * @param userCode 学号
     * @param input 输入框名
     * @return key
     */
    public static String getWorkFileCacheKey(String userCode, String input) {
        return "WORK_FILE:" + userCode + ":" + input;
    }
}
