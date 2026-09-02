package com.sast.crs.entity;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sast.crs.enums.CompetitionTypeEnum;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "competition", autoResultMap = true)
public class Competition {

    /**
     * 比赛ID
     */
    @TableId(type = IdType.AUTO)
    @JsonProperty("id")
    private Long id;

    /**
     * 比赛名
     */
    @NotNull(message = "比赛名称不能为 null")
    @JsonProperty("name")
    private String name;

    /**
     * 比赛介绍
     */
    @NotNull(message = "比赛介绍不能为 null")
    @JsonProperty("introduce")
    private String introduce;

    /**
     * 报名开始时间
     */
    @JsonProperty("reg_begin_time")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "开始时间不能为 null")
    private LocalDateTime regBeginTime;

    /**
     * 报名结束时间
     */
    @JsonProperty("reg_end_time")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "结束时间不能为 null")
    private LocalDateTime regEndTime;

    /**
     * 提交开始时间
     */
    @JsonProperty("submit_begin_time")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "开始时间不能为 null")
    private LocalDateTime submitBeginTime;

    /**
     * 提交截止时间
     */
    @JsonProperty("submit_end_time")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "结束时间不能为 null")
    private LocalDateTime submitEndTime;

    /**
     * 评审开始时间
     */
    @JsonProperty("review_begin_time")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "开始时间不能为 null")
    private LocalDateTime reviewBeginTime;

    /**
     * 评审结束时间
     */
    @JsonProperty("review_end_time")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "结束时间不能为 null")
    private LocalDateTime reviewEndTime;

    /**
     * 表单 schema
     */
    @JsonProperty("table")
    @TableField(value = "`table`", typeHandler = JacksonTypeHandler.class)
    private JSONObject table;

    /**
     * 团队最小人数限制
     */
    @JsonProperty("min_team_members")
    private Integer minTeamMembers;

    /**
     * 团队最大人数限制
     */
    @JsonProperty("max_team_members")
    private Integer maxTeamMembers;

    /**
     * 活动负责人学工号
     */
    @JsonProperty("user_code")
    @NotNull(message = "负责人学工号不能为 null")
    private String userCode;

    /**
     * 是否需要经过审批（0 不需要，1 需要）
     */
    @JsonProperty("is_review")
    @NotNull(message = "是否需要审批不能为空")
    private Boolean isReview;

    /**
     * 审批关系
     */
    @TableField(typeHandler = FastjsonTypeHandler.class)
    @JsonProperty("review_settings")
    private Map<String, String> reviewSettings;

    /**
     * 比赛类型
     * 0 个人赛
     * 1 团队赛
     */
    @JsonProperty("type")
    private CompetitionTypeEnum type;

    /**
     * 封面URL
     */
    private String cover;

    /**
     * 是否设置白名单
     */
    @JsonProperty("is_white_list")
    private Boolean isWhiteList;

    /**
     * 是否为团队赛
     * 当type是1，但max/minTeamMembers都是1的时候也算是单人赛
     *
     * @return 团队赛 true；个人赛 false
     */
    public boolean isTeamCom() {
        if (CompetitionTypeEnum.SINGLE_COMPETITION.getType().equals(this.getType().getType()))
            return false;
        else return !Integer.valueOf(1).equals(minTeamMembers) || !Integer.valueOf(1).equals(maxTeamMembers);
    }
}
