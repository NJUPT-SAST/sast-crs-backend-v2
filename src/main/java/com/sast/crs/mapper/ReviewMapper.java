package com.sast.crs.mapper;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.entity.Review;
import com.sast.crs.model.ComListForReview;
import com.sast.crs.model.ProgramListForReview;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewMapper extends BaseMapper<Review> {
    IPage<ComListForReview> getComInfo(Page<ComListForReview> page, @Param("code") String code, @Param("dep_id") Integer depId);

    JSONObject confirm(@Param("comId") Integer comId);

    IPage<ProgramListForReview> getProgramInfo(Page<ProgramListForReview> page, @Param("comId") Integer comId, @Param("depIds") List<Integer> depIds);

    IPage<ProgramListForReview> getProgramInfoNotIn(Page<ProgramListForReview> page, @Param("comId") Integer comId, @Param("depIds") List<Integer> depIds);

    Integer getComIdByProId(Integer proId);

    List<String> getAccessories(@Param("comId") Integer comId, @Param("userCode") String userCode);

    String getJMember(@Param("comId") Integer comId, @Param("captainId") String captainId);

    String getCaptainIdByProId(Integer proId);

    Integer updateReview(@Param("code") String code, @Param("id") Integer id, @Param("accept") Boolean accept, @Param("opinion") String opinion);

    Integer getReviewCount();

    Integer getComCount();

    Long getReviewNum(@Param("comId") Long comId);

    Integer getTotal(@Param("settings") List<Integer> list, @Param("comId") Integer comId);

    String getContents(@Param("comId") Integer comId, @Param("userCode") String userCode);

    Integer getTotalNotIn(@Param("settings") List<Integer> list, @Param("comId") Integer comId);

    String getTeamName(@Param("comId") Integer comId, @Param("captainId") String captainId);

    String getCaptainName(@Param("code") String captainId);
}
