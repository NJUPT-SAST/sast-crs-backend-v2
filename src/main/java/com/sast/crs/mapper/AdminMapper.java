package com.sast.crs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.entity.Competition;
import com.sast.crs.model.ComMangerVo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author xun
 * @create 2022/7/21 16:37
 */
@Repository
public interface AdminMapper extends BaseMapper<Competition> {
    IPage<ComMangerVo> getComMangerInfo(Page<ComMangerVo> page, @Param("comId") Long comId);
}
