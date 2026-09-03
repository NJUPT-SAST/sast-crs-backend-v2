package com.sast.crs.model;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.sast.crs.enums.CompetitionTypeEnum;
import lombok.Data;

@Data
@TableName(autoResultMap = true)
public class TeamInfoWithCom {
    private String comName;
    private CompetitionTypeEnum comType;
    private String teamName;
    @TableField(typeHandler = FastjsonTypeHandler.class)
    private JSONArray teacherJson;
    @TableField(typeHandler = FastjsonTypeHandler.class)
    private JSONArray teamMembersJson;
}
