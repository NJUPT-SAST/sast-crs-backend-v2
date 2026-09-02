package com.sast.crs.service;

import com.sast.crs.entity.Notice;

import java.util.List;
import java.util.Map;

public interface NoticeService {
    List<Map<String, Object>> getNotice(Long id);
    void setNotice(Notice notice);
    void editNotice(Notice notice);
    void delNotice(Long id);
}
