package com.sast.crs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JudgeAssignColumnsTest {

    private Map<Integer, String> head(String... titles) {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (int i = 0; i < titles.length; i++) {
            map.put(i, titles[i]);
        }
        return map;
    }

    @Test
    void detectsJudgeColumnInExportedTemplate() {
        // 导出模板：作品id / 作品名称 / 项目类别 / 评委学号
        assertEquals(List.of(3), JudgeAssignColumns.detect(head("作品id", "作品名称", "项目类别", "评委学号")));
    }

    @Test
    void detectsEveryJudgeColumn() {
        assertEquals(
                List.of(3, 4, 5), JudgeAssignColumns.detect(head("作品id", "作品名称", "项目类别", "评委学号", "评委学号1", "评委学号2")));
    }

    @Test
    void ignoresBlankAndUnrelatedHeaders() {
        assertTrue(JudgeAssignColumns.detect(head("作品id", "作品名称", "项目类别")).isEmpty());
        assertTrue(JudgeAssignColumns.detect(head("作品id", "作品名称", "项目类别", null)).isEmpty());
        assertTrue(JudgeAssignColumns.detect(null).isEmpty());
    }

    @Test
    void fallsBackToColumnAfterWorkType() {
        // 存量文件：没有「评委学号」表头，评委学号从「项目类别」后面一列开始
        assertEquals(3, JudgeAssignColumns.fallbackStart(head("作品id", "作品名称", "项目类别")));
    }

    @Test
    void fallsBackToLegacyThirdColumnWithoutWorkType() {
        assertEquals(2, JudgeAssignColumns.fallbackStart(head("作品id", "作品名称")));
        assertEquals(2, JudgeAssignColumns.fallbackStart(null));
    }
}
