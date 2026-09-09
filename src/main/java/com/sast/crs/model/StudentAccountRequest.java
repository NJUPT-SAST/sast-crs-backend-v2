package com.sast.crs.model;

import lombok.Data;

/**
 * 学生账号增删改请求体
 */
@Data
public class StudentAccountRequest {
    /**
     * 学号
     */
    private String code;

    /**
     * 姓名
     */
    private String name;

    /**
     * 联系方式
     */
    private String contact;

    /**
     * 密码：新增时必填；编辑时留空表示不重置
     */
    private String password;
}
