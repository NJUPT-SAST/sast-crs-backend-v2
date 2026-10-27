package com.sast.crs.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class WorkMapperApprovalFilterTest {

    @Test
    void templateQueryRequiresAnApprovedReview() {
        String resource = "mapper/WorkMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            Configuration configuration = new Configuration();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();

            BoundSql boundSql = configuration.getMappedStatement("com.sast.crs.mapper.WorkMapper.selectApprovedForJudgeTemplate").getBoundSql(Collections.singletonMap("comId", 41L));
            String sql = boundSql.getSql().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

            assertTrue(sql.contains("exists ( select 1 from review r"));
            assertTrue(sql.contains("r.com_id = w.com_id"));
            assertTrue(sql.contains("r.user_code = w.user_code"));
            assertTrue(sql.contains("r.accept = 1"));
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }
}
