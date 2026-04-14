package com.sast.crs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sast.crs.entity.Team;
import com.sast.crs.model.TeamInfoWithCom;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMapper extends BaseMapper<Team> {
    TeamInfoWithCom selectTeamInfoWithCom(@Param("comId") Long comId,
                                      @Param("captainCode") String captainCode);
}
