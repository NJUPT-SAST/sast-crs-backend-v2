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
    /**
     * 审核结论：true=通过，false=未通过，null=未审核（比赛不需要审核时也是 null）
     */
    private Boolean accept;
    /**
     * 审核意见，导出时作为「未通过原因」
     */
    private String opinion;
}
