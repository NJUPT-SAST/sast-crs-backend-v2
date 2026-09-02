package com.sast.crs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("judge")
public class Judge {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 比赛id
     */
    @TableField("com_id")
    private Long comId;

    /**
     * 评委学工号
     */
    @TableField("judge_code")
    private String judgeCode;

    /**
     * 队长学号
     */
    @TableField("user_code")
    private String userCode;
}
