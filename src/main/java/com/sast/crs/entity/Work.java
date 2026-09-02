package com.sast.crs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName(value = "work", autoResultMap = true)
public class Work {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 比赛ID
     */
    @TableField("com_id")
    private Long comId;
    /**
     * 队长学号
     */
    @TableField("user_code")
    private String userCode;
    /**
     * 作品名称
     */
    @TableField("work_name")
    private String workName;
    /**
     * 由XRender渲染的表单数据
     */
    @TableField("schema_content")
    private String schemaContent;
}
