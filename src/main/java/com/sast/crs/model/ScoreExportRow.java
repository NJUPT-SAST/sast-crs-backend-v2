package com.sast.crs.model;

import lombok.Data;

@Data
public class ScoreExportRow {
    private Integer id;
    private String leaderCode;
    private String leaderName;
    private String depName;
    private String schemaContent;
    private String judgeCode;
    private Integer score;
    private String opinion;
}
