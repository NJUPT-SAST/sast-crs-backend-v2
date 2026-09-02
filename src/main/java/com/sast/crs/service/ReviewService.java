package com.sast.crs.service;

import com.sast.crs.model.ComListForReview;
import com.sast.crs.model.PageInfo;
import com.sast.crs.model.ProgramInfoForReview;
import com.sast.crs.model.ProgramListForReview;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ReviewService {
    PageInfo<ComListForReview> getCompetitionList(Integer page, String code, Integer depId);

    PageInfo<ProgramListForReview> getProgramList(String code, Integer comId, Integer pageNum);

    ProgramInfoForReview getProgramInfo(Integer proId);

    Boolean updateReview(String code, Integer id, Boolean accept, String opinion);

    Boolean redPoint(Integer depId);

    Integer getTotal(String code, Integer comId);

    List<Map<String, String>> importStudent(MultipartFile file, Integer depId, HttpServletResponse response) throws IOException;

}
