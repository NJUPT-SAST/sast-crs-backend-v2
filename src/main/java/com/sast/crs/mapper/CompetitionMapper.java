package com.sast.crs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sast.crs.entity.Competition;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionMapper extends BaseMapper<Competition> {
    String getTableString(Long comId);
}
