package com.sast.crs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sast.crs.entity.Work;
import com.sast.crs.model.WorkForExcel;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkMapper extends BaseMapper<Work> {
    List<Work> getWorks(@Param("pageNum") Integer pageNum, @Param("pageSize") Integer pageSize, @Param("comId") Long comId);
    List<WorkForExcel> selectListForExcel(@Param("comId") Long comId);
}
