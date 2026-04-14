package com.sast.crs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.entity.Score;
import com.sast.crs.model.ComListForScore;
import com.sast.crs.model.ProgramListForScore;
import com.sast.crs.model.ScoreExportRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Jun
 * @date 2022/07/23
 */
@Mapper
public interface ScoreMapper extends BaseMapper<Score> {
    IPage<ComListForScore> getComInfo(Page<ComListForScore> page, @Param("code") String code);
    IPage<ProgramListForScore> getProList(Page<ProgramListForScore> page,
                                          @Param("code") String code,
                                          @Param("comId") Integer comId);
    Integer upload(@Param("jCode") String JudgeCode,
                   @Param("uCode") String userCode, @Param("comId") Integer comId,
                   @Param("score") Integer score, @Param("opinion") String opinion);
    String getUserCode(Integer proId);
    Integer getIndexId(@Param("id") Integer id, @Param("code") String teacherCode, @Param("user") String userCode);
    Integer getJudgeCount(String code);
    Integer getComCount(String code);
    Integer getJCount(@Param("code") String code, @Param("id") Integer id);
    Integer getTotal(@Param("code") String code, @Param("id") Integer comId);
    Integer getScoreInfo(@Param("comId") Integer comId, @Param("user") String userCode, @Param("judge") String judgeCode);
    String getOpinionInfo(@Param("comId") Integer comId, @Param("user") String userCode, @Param("judge") String judgeCode);
    Integer getComIdByProId(@Param("proId") Integer proId);
    Integer getTotalDone(@Param("code") String code, @Param("id") Integer comId);
    Integer getScore(@Param("code") String code, @Param("id") Integer id);
    String getOpinion(@Param("code") String code, @Param("id") Integer id);
    boolean isExistence(@Param("comId") Integer comId, @Param("userCode") String userCode,@Param("teacher") String teacher);
    Integer updateScore(@Param("comId") Integer comId,@Param("teacher") String teacher,@Param("student") String student,
                               @Param("score") Integer score, @Param("opinion") String opinion) ;

    List<ScoreExportRow> selectExportRows(@Param("comId") Long comId);
}
