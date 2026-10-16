package com.sast.crs.constant;

import com.sast.crs.entity.User;

public class RedisKeyConst {

    /** 作品文件上传缓存的键前缀（也用于按前缀批量扫描） */
    public static final String WORK_FILE_PREFIX = "WORK_FILE:";

    public static String getTokenKey(User user) {
        return "TOKEN:" + user.getCode();
    }

    /**
     * 获取文件缓存数据的Key
     * 不包含前缀
     *
     * @param userCode 学号
     * @param comId    比赛 id：不带上的话，A 比赛留下的缓存会把 B 比赛同名字段的上传一起卡住
     * @param input    输入框名
     * @return key
     */
    public static String getWorkFileCacheKey(String userCode, Long comId, String input) {
        return WORK_FILE_PREFIX + userCode + ":" + comId + ":" + input;
    }
}
