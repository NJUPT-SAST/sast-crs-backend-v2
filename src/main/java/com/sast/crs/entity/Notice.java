package com.sast.crs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class Notice {

    @TableId(type = IdType.AUTO)
    Long id;

    @NotNull(message = "比赛ID不能为空")
    @JsonProperty("com_id")
    Long comId;

    @NotBlank(message = "标题不能为空")
    String title;

    @NotBlank(message = "内容不能为空")
    String content;

    @NotNull(message = "角色不能为空")
    Integer role;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    LocalDateTime time;
}
