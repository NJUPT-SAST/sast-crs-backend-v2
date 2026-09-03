package com.sast.crs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.entity.Competition;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionMapper extends BaseMapper<Competition> {
    String getTableString(Long comId);

    IPage<Competition> selectSignedCompetitions(Page<Competition> page, @Param("userCode") String userCode);
}
