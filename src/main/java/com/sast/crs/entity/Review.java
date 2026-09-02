package com.sast.crs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reviewCode;
    private Long comId;
    private String userCode;
    private Boolean accept;
    private String opinion;
}
