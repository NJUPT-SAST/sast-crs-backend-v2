package com.sast.crs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkForExcel {
    private Integer id;
    private String workName;
    private String userCode;
    private String comName;
    private String teamName;
    private String memberJson;
    private String captainName;
    private String schemaContent;
}
