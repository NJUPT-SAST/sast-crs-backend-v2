package com.sast.crs.model;

import com.alibaba.fastjson.JSONArray;
import com.sast.crs.entity.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgramInfoForScore {
    private String teamName;
    private UserInfo captain;
    private Integer memberNum;
    private JSONArray memberList;
    private List<Accessories> accessories;
    private List<Text> texts;
    private Integer score;
    private String opinion;
}
