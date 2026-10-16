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

    /**
     * 评委学号：导出时留空，管理员在这一列（可往右复制成多列）填评委学号后原表导入即可。
     * 表头不能删，导入端按表头定位评委学号列。
     */
    @ExcelProperty("评委学号")
    private String judgeCode;
}
