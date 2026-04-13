package com.sast.crs.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sast.crs.entity.User;
import com.sast.crs.pojo.UserResponse;

import java.util.Map;

public interface UserService {
    Map<String, Object> getAllComList(Integer cur, Integer limit);
    Map<String, Object> getSignedComList(String userCode, Integer cur, Integer limit);
    Map<String, Object> getComInfo(Long comId);
    Map<String, Object> getComSignUpInfo(Long comId);
    Map<String, Object> searchComName(String key, Integer cur, Integer limit);
    Map<String, Object> getTeamInfo(User user, Long comId);
    Map<String, String> getUploadCertificate(User user, Long comId, String input, String filename);
    UserResponse getUserProfile(User user);
    JSONObject getComSchemaTemplate(Long comId);
    JSONArray getComSchema(User user, Long comId);
    void uploadComSchema(User user, Long comId, String jsonData);
    void signUpCom(User user, String jsonData);
}
