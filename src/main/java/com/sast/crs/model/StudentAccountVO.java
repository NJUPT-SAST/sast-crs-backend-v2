package com.sast.crs.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 学生账号列表项
 */
@Data
@AllArgsConstructor
public class StudentAccountVO {
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
}
