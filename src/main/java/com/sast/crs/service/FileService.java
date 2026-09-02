package com.sast.crs.service;

import com.sast.crs.entity.User;
import com.sast.crs.model.WorkOutput;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface FileService {

    List<WorkOutput> exportFileData(Long comId);

    void exportComInfo(HttpServletResponse response, Long comId);

    String getDownloadCertificate(User user, String url);
}
