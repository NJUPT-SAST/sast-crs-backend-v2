package com.sast.crs.service;

import com.alibaba.fastjson2.JSONObject;
import com.sast.crs.entity.Competition;
import com.sast.crs.entity.User;
import com.sast.crs.model.JudgeAccountRequest;
import com.sast.crs.model.StudentAccountRequest;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface AdminService {

    void createContest(Competition competition, MultipartFile cover);

    void editContest(Competition competition, MultipartFile cover);

    void deleteContest(Long id);

    Map<String, Object> getContestList(Integer pageNum, Integer pageSize);

    Competition getContestInfo(Long id);

    User getUserInfo(String code);

    void download(HttpServletResponse response, Long comId, String userCode) throws IOException;

    Map<String, Object> getComMangerInfo(Integer pageNum, Integer pageSize, Long comId);

    JSONObject getSchema(Long comId);

    Map<String, Object> getJudgeAccountList(Integer pageNum, Integer pageSize);

    void createJudgeAccount(JudgeAccountRequest request);

    void editJudgeAccount(JudgeAccountRequest request);

    void deleteJudgeAccount(String code);

    Map<String, Object> getStudentAccountList(Integer pageNum, Integer pageSize);

    void createStudentAccount(StudentAccountRequest request);

    void editStudentAccount(StudentAccountRequest request);

    void deleteStudentAccount(String code);

    List<Map<String, String>> importJudgeAccount(MultipartFile file, Integer depId);

    void importJudgeAssign(MultipartFile file);

    void setWhiteList(Long comId, Boolean isWhiteList, MultipartFile file);
}
