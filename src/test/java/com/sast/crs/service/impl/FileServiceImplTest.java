package com.sast.crs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sast.crs.entity.Work;
import com.sast.crs.mapper.WorkMapper;
import com.sast.crs.model.WorkOutput;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileServiceImplTest {

    private static final Long COM_ID = 41L;

    @Test
    void exportsCollegeAndTypeFromTheSubmittedWorkForm() {
        Work work = work("""
                [{"input":"学院","content":"计算机学院"},{"input":"项目类别","content":"创新项目"}]
                """);
        List<WorkOutput> result = serviceWithRows(List.of(work)).exportFileData(COM_ID);
        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).getId());
        assertEquals("演示作品", result.get(0).getName());
        assertEquals("计算机学院", result.get(0).getCollege());
        assertEquals("创新项目", result.get(0).getType());
        assertNull(result.get(0).getJudgeCode());
    }

    @Test
    void leavesCollegeEmptyForHistoricalWorkWithoutTheField() {
        Work work = work("""
                [{"input":"项目类别","content":"创新项目"}]
                """);
        List<WorkOutput> result = serviceWithRows(List.of(work)).exportFileData(COM_ID);
        assertNull(result.get(0).getCollege());
        assertEquals("创新项目", result.get(0).getType());
    }

    @Test
    void leavesCollegeEmptyForNullSubmittedCollege() {
        Work work = work("""
                [{"input":"学院","content":null},{"input":"项目类别","content":"创新项目"}]
                """);
        assertNull(serviceWithRows(List.of(work)).exportFileData(COM_ID).get(0).getCollege());
    }

    @Test
    void allowsAnEmptyApprovedSetForAHeaderOnlyTemplate() {
        assertTrue(serviceWithRows(List.of()).exportFileData(COM_ID).isEmpty());
    }

    private Work work(String schemaContent) {
        Work work = new Work();
        work.setId(9L);
        work.setWorkName("演示作品");
        work.setSchemaContent(schemaContent);
        return work;
    }

    private FileServiceImpl serviceWithRows(List<Work> rows) {
        WorkMapper mapper = (WorkMapper) Proxy.newProxyInstance(
                WorkMapper.class.getClassLoader(), new Class<?>[]{WorkMapper.class}, (proxy, method, args) -> {
                    if (method.getName().equals("selectApprovedForJudgeTemplate")) {
                        assertEquals(COM_ID, args[0]);
                        return rows;
                    }
                    throw new AssertionError("Unexpected query: " + method.getName());
                });
        return new FileServiceImpl(null, null, mapper, null);
    }
}
