package com.sast.crs.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkForExport {
    @ExcelProperty("作品id")
    private Long id;

    @ExcelProperty("比赛名称")
    private String comName;

    @ExcelProperty("队长学号")
    private String userCode;

    @ExcelProperty("作品名称")
    private String workName;
}
