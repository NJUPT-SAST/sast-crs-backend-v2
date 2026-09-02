package com.sast.crs.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("file")
public class File implements Serializable {
    @Serial
    private static final long serialVersionUID = -8010947317726351477L;

    @TableId(type = IdType.AUTO)
    @ExcelProperty("作品id")
    private Long id;

    /**
     * 比赛ID
     */
    @TableField("com_id")
    @ExcelProperty("活动id")
    private Long comId;

    /**
     * 队长学号
     */
    @TableField("user_code")
    @ExcelProperty("团队赛队长学号/个人赛个人学号")
    private String userCode;

    /**
     * 输入框名
     */
    @Nullable
    @ExcelProperty("输入框名")
    private String input;

    /**
     * 作品地址
     */
    @ExcelIgnore
    private String url;
}
