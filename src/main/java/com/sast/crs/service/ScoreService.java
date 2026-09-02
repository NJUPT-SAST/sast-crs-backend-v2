package com.sast.crs.service;

import com.sast.crs.model.ComListForScore;
import com.sast.crs.model.PageInfo;
import com.sast.crs.model.ProgramInfoForScore;
import com.sast.crs.model.ProgramListForScore;

import jakarta.servlet.http.HttpServletResponse;

public interface ScoreService {
    PageInfo<ComListForScore> getCompetitionList(String code, Integer pageNum);

    PageInfo<ProgramListForScore> getProgramList(String code, Integer comId, Integer pageNum);

    ProgramInfoForScore getProgramInfo(Integer proId, String judgeCode);

    Boolean uploadScore(String teacherCode, Integer id, Integer score, String opinion);

    Boolean redPoint(String code);

    Boolean confirmPro(String code, Integer id);

    Integer getTotal(String code, Integer comId);

    void exportScore(HttpServletResponse response, Long comId);
}
