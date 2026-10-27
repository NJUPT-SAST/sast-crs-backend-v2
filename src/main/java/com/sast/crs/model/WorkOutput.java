package com.sast.crs.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.sast.crs.constant.CommonConst;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class WorkOutput {
    @ExcelProperty(value = "作品id", index = 0)
    private Long id;

    @ExcelProperty(value = "作品名称", index = 1)
    private String name;

    @ExcelProperty(value = "学院", index = 2)
    private String college;

    @ExcelProperty(value = CommonConst.WORK_TYPE, index = 3)
    private String type;

    /**
     * 评委学号：导出时留空，管理员在这一列（可往右复制成多列）填评委学号后原表导入即可。
     * 表头不能删，导入端按表头定位评委学号列。
     */
    @ExcelProperty(value = "评委学号", index = 4)
    private String judgeCode;
}
