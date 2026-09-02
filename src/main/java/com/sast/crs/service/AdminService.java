package com.sast.crs.service;

import com.alibaba.fastjson2.JSONObject;
import com.sast.crs.entity.Competition;
import com.sast.crs.entity.User;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
}
