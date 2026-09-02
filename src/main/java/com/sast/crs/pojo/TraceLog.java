package com.sast.crs.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TraceLog {
    /**
     * 操作描述
     */
    private String description;
    /**
     * 请求平台
     */
    private String env;
    /**
     * UA
     */
    private String userAgent;
    /**
     * 操作用户
     */
    private Object user;

    /**
     * 消耗时间
     */
    private String spendTime;

    /**
     * URL
     */
    private String url;

    /**
     * 请求参数
     */
    private Object params;

    /**
     * 请求返回的结果
     */
    private Object result;

    private final static String commonFormat = """
            
            ===========捕获响应===========
            操作描述：%s
            请求平台：%s
            请求地址：%s
            请求参数：%s
            请求返回：%s
            请求用户：%s
            请求耗时：%s
            请求UA：%s
            ===========释放响应===========""";

    private final static String errorFormat = """
            
            ===========捕获异常===========
            操作描述：%s
            请求平台：%s
            请求地址：%s
            请求参数：%s
            请求异常：%s
            请求用户：%s
            请求耗时：%s
            请求UA：%s
            ===========释放异常===========""";

    public String toLogFormat(Boolean requestStatus) {
        String strResult = getResult(200);
        String strParam = String.valueOf(params);
        String format = requestStatus ? commonFormat : errorFormat;
        String strUser = String.valueOf(user);
        return String.format(format, description, env, url, strParam, strResult, strUser, spendTime, userAgent);
    }

    /**
     * 截取指定长度的返回值
     */
    public String getResult(int factor) {
        String result = String.valueOf(this.result);
        if (factor == 0 || result.length() < factor) {
            return result;
        }
        return result.substring(0, factor - 1) + "...}";
    }
}
