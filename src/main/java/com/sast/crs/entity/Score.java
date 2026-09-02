package com.sast.crs.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Score {

    @TableId(type = IdType.AUTO)
    @ExcelProperty("评审结果id")
    Long id;

    @ExcelProperty("评委学工号")
    String judgeCode;

    @ExcelProperty("队长学工号")
    String userCode;

    @ExcelProperty("比赛id")
    Long comId;

    @ExcelProperty("分数")
    Integer score;

    @ExcelProperty("评分意见")
    String opinion;
}
