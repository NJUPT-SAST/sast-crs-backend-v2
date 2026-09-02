package com.sast.crs.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.sast.crs.constant.CommonConst;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class WorkOutput {
    @ExcelProperty("作品id")
    private Long id;

    @ExcelProperty("作品名称")
    private String name;

    @ExcelProperty(CommonConst.WORK_TYPE)
    private String type;
}