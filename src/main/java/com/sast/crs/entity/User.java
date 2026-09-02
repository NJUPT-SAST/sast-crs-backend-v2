package com.sast.crs.entity;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sast.crs.util.UserExtraConverter;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName(value = "user", autoResultMap = true)
@ExcelIgnoreUnannotated
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 6839081045762510729L;
    @ExcelProperty("学号")
    @TableId(value = "code", type = IdType.INPUT)
    private String code;
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("密码")
    private String password;
    @TableField("dep_id")
    private Integer depId;
    private Integer role;
    /***
     * 用户的额外信息
     */
    @TableField(value = "extra", typeHandler = JacksonTypeHandler.class)
    @ExcelProperty(value = "联系方式", converter = UserExtraConverter.class)
    private UserExtra extra;
}
