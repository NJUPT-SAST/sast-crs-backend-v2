package com.sast.crs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("department")
public class Department {
    /**
     * 学院编号
     */
    @TableId(type = IdType.INPUT)
    private Integer id;

    /**
     * 学院名称
     */
    private String name;
}
