package com.sast.crs.controller;

import com.sast.crs.annotation.OperateLog;
import com.sast.crs.annotation.PassToken;
import com.sast.crs.entity.Department;
import com.sast.crs.entity.User;
import com.sast.crs.interceptor.UserInterceptor;
import com.sast.crs.service.DepartmentService;
import com.sast.crs.service.FileService;
import com.sast.crs.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PassToken
@RestController
@RequestMapping("/com")
public class CommonController {
    private FileService fileService;
    private NoticeService noticeService;
    private DepartmentService departmentService;

    @Autowired
    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    @Autowired
    public void setNoticeService(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Autowired
    public void setDepartmentService(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * 获取比赛公告列表
     * 已登录用户获取相应角色的公告
     * 未登录用户获取普通角色的公告
     *
     * @param id 比赛id
     * @return 公告列表
     */
    @OperateLog("获取比赛公告列表")
    @GetMapping("/notice/list")
    public List<Map<String, Object>> getComNotice(@RequestParam Long id) {
        return noticeService.getNotice(id);
    }

    /**
     * 获取下载凭证
     *
     * @param url URL编码后的文件地址
     */
    @OperateLog("获取下载凭证")
    @GetMapping("/file/downloadCertificate")
    public Map<String, String> downloadCertificate(@RequestParam String url) {
        User user = UserInterceptor.userHolder.get();
        return new HashMap<>() {
            {
                put("url", fileService.getDownloadCertificate(user, url));
            }
        };
    }

    /**
     * 获取学院列表
     * 学院编号是「审核学院代号」（review_settings 的 key）的唯一事实来源，
     * 前端不得再用本地清单的下标推导，否则学院增删后会静默错位。
     *
     * @return 学院列表（按学院编号升序）
     */
    @OperateLog("获取学院列表")
    @GetMapping("/department/list")
    public List<Department> getDepartmentList() {
        return departmentService.listAll();
    }
}
