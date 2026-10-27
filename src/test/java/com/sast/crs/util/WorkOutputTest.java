package com.sast.crs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.alibaba.excel.EasyExcel;
import com.sast.crs.model.WorkOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class WorkOutputTest {

    @Test
    void writesCollegeAndImportableJudgeColumnsInTheWorkbook() throws Exception {
        WorkOutput work = new WorkOutput();
        work.setId(9L);
        work.setName("演示作品");
        work.setCollege("计算机学院");
        work.setType("创新项目");
        try (Workbook workbook = export(List.of(work))) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<Integer, String> headers = headers(sheet);
            assertEquals(List.of("作品id", "作品名称", "学院", "项目类别", "评委学号"), List.copyOf(headers.values()));
            assertEquals("计算机学院", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("创新项目", sheet.getRow(1).getCell(3).getStringCellValue());
            assertEquals(List.of(4), JudgeAssignColumns.detect(headers));
            assertEquals(4, JudgeAssignColumns.fallbackStart(headers));
            headers.put(5, "评委学号1");
            headers.put(6, "评委学号2");
            assertEquals(List.of(4, 5, 6), JudgeAssignColumns.detect(headers));
        }
    }

    @Test
    void writesHeadersWhenNoWorkPassedReview() throws Exception {
        try (Workbook workbook = export(List.of())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(List.of("作品id", "作品名称", "学院", "项目类别", "评委学号"), List.copyOf(headers(sheet).values()));
            assertNull(sheet.getRow(1));
        }
    }

    private Workbook export(List<WorkOutput> rows) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, WorkOutput.class).sheet("sheet1").doWrite(rows);
        return WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()));
    }

    private Map<Integer, String> headers(Sheet sheet) {
        Map<Integer, String> headers = new LinkedHashMap<>();
        for (int index = 0; index < sheet.getRow(0).getLastCellNum(); index++) {
            headers.put(index, sheet.getRow(0).getCell(index).getStringCellValue());
        }
        return headers;
    }
}
