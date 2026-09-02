package com.sast.crs.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.MapUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sast.crs.annotation.CheckRole;
import com.sast.crs.annotation.OperateLog;
import com.sast.crs.entity.*;
import com.sast.crs.enums.UserRoleEnum;
import com.sast.crs.mapper.WhiteListMapper;
import com.sast.crs.model.WhiteList;
import com.sast.crs.util.ExcelForJudgeUtil;
import com.sast.crs.model.WorkOutput;
import com.sast.crs.service.*;
import com.sast.crs.util.ExcelForWhiteListUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CheckRole(UserRoleEnum.ADMIN)
public class AdminController {

    private final AdminService adminService;
    private final FileService fileService;
    private final ScoreService scoreService;
    private final NoticeService noticeService;
    private final ExcelForJudgeUtil excelForJudgeUtil;
    private final ExcelForWhiteListUtil whiteListUtil;
    private final WhiteListMapper whiteListMapper;

    public AdminController(AdminService adminService, FileService fileService, ScoreService scoreService, NoticeService noticeService, ExcelForJudgeUtil excelForJudgeUtil, ExcelForWhiteListUtil whiteListUtil, WhiteListMapper whiteListMapper) {
        this.adminService = adminService;
        this.fileService = fileService;
        this.scoreService = scoreService;
        this.noticeService = noticeService;
        this.excelForJudgeUtil = excelForJudgeUtil;
        this.whiteListUtil = whiteListUtil;
        this.whiteListMapper = whiteListMapper;
    }

    /**
     * 踩坑记录：formdata 可以单独给每个参数设置content-type
     * 创建活动
     *
     * @param competition 比赛（活动）
     * @param cover       封面
     * @return 比赛id
     */
    @OperateLog(value = "管理端设置活动信息")
    @PostMapping("/com/create")
    public Long createContest(@RequestParam String competition, MultipartFile cover) {
        Competition parseCom = JSON.parseObject(competition, Competition.class);
        adminService.createContest(parseCom, cover);
        return parseCom.getId();
    }

    /**
     * 设置参赛白名单
     *
     * @param comId 比赛id
     * @param file  白名单Excel文件
     * @return 执行结果
     */
    @OperateLog(value = "管理端设置/修改参赛白名单")
    @PostMapping("/com/whitelist")
    public String SetWhiteList(@RequestParam Long comId, @RequestParam Boolean isWhiteList, MultipartFile file) {
        whiteListUtil.setComId(comId);
        whiteListUtil.setIsWhiteList(isWhiteList);
        // 如果不设置白名单，则删除已设置的白名单
        if (!isWhiteList) {
            QueryWrapper<WhiteList> whiteListQueryWrapper = new QueryWrapper<>();
            whiteListQueryWrapper.eq("com_id", comId);
            if (whiteListMapper.exists(whiteListQueryWrapper)) {
                whiteListMapper.delete(whiteListQueryWrapper);
            }
            return "success";
        }
        try {
            EasyExcel.read(file.getInputStream(), whiteListUtil).sheet().headRowNumber(0).doRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "success";
    }

    /**
     * 修改活动信息
     *
     * @param competition 活动（比赛）
     * @return 活动id
     */
    @OperateLog(value = "管理端修改活动信息")
    @PostMapping("/com/edit")
    public Long editContest(@RequestParam String competition, MultipartFile cover) {
        Competition parseCom = JSON.parseObject(competition, Competition.class);
        adminService.editContest(parseCom, cover);
        return parseCom.getId();
    }

    /**
     * 获取比赛表单
     *
     * @param comId 比赛id
     * @return 比赛表单
     */
    @OperateLog("获取比赛表单")
    @GetMapping("/com/getSchema")
    public JSONObject getComSchema(@RequestParam Long comId) {
        return adminService.getSchema(comId);
    }

    /**
     * 删除活动
     *
     * @param comId 活动id
     * @return 执行结果
     */
    @OperateLog(value = "管理端删除活动")
    @PostMapping("/com/delete")
    public String deleteContest(@RequestParam Long comId) {
        adminService.deleteContest(comId);
        return "success";
    }

    /**
     * 获取活动列表
     *
     * @param pageNum  当前页数
     * @param pageSize 每页大小
     * @return 活动列表+页数
     */
    @OperateLog(value = "管理端获取活动列表")
    @GetMapping("/com/competitionList")
    public Map<String, Object> getContestList(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return adminService.getContestList(pageNum, pageSize);
    }

    /**
     * 获取活动信息
     *
     * @param comId 活动id
     * @return 活动信息
     */
    @OperateLog(value = "管理端获取活动详情")
    @GetMapping("/com/competitionInfo")
    public Competition getContestInfo(@RequestParam Long comId) {
        return adminService.getContestInfo(comId);
    }

    /**
     * 管理活动
     *
     * @param comId    活动id
     * @param pageNum  当前页数
     * @param pageSize 每页大小
     * @return 活动管理界面的信息
     */
    @OperateLog(value = "管理端管理活动")
    @GetMapping("/com/manager")
    public Map<String, Object> comManager(@RequestParam Long comId, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return adminService.getComMangerInfo(pageNum, pageSize, comId);
    }

    /**
     * 获取用户详细信息
     *
     * @param code 学号
     * @return 用户信息
     */
    @OperateLog(value = "管理端获取用户详细信息")
    @GetMapping("/userInfo")
    public User getUserInfo(@RequestParam String code) {
        return adminService.getUserInfo(code);
    }

    /**
     * 导出作品id和作品名称用以分配评委
     *
     * @param response 响应
     * @param comId    活动id
     */
    @OperateLog(value = "导出作品数据")
    @GetMapping("/exportWorkData")
    public void exportFileData(HttpServletResponse response, @RequestParam Long comId) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");

            String fileName = URLEncoder.encode("作品id及名称" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<WorkOutput> dataList = fileService.exportFileData(comId);
            // 这里需要设置不关闭流
            EasyExcel.write(response.getOutputStream(), WorkOutput.class).autoCloseStream(Boolean.FALSE).sheet(
                    "sheet1").doWrite(dataList);
        } catch (Exception e) {
            // 重置response
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            Map<String, String> map = MapUtils.newHashMap();
            map.put("status", "failure");
            map.put("message", "下载文件失败" + e.getMessage());
            try {
                response.getWriter().println(JSON.toJSONString(map));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 通过导入excel表分配评委
     *
     * @param file 导入excel表
     * @return 执行结果
     */
    @OperateLog(value = "分配评委")
    @PostMapping("/judge/assign")
    public String distributeJudges(MultipartFile file) {
        try {
            EasyExcel.read(file.getInputStream(), excelForJudgeUtil).sheet().doRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "success";
    }

    /**
     * 导出评审结果
     *
     * @param response 响应
     * @param comId    活动id
     */
    @OperateLog(value = "导出评审结果")
    @GetMapping("/data/result")
    public void exportResult(HttpServletResponse response, @RequestParam("comId") Long comId) {
        scoreService.exportScore(response, comId);
    }

    /**
     * 发布公告
     *
     * @param notice 公告
     * @return 执行结果
     */
    @OperateLog("发布公告")
    @PostMapping("/notice/release")
    public String announce(@Valid @RequestBody Notice notice) {
        noticeService.setNotice(notice);
        return "success";
    }

    /**
     * 修改公告
     *
     * @param notice 公告
     * @return 执行结果
     */
    @OperateLog("修改公告")
    @PostMapping("/notice/edit")
    public String editNotice(@RequestBody Notice notice) {
        noticeService.editNotice(notice);
        return "success";
    }

    /**
     * 删除公告
     *
     * @param id 公告id
     * @return 执行结果
     */
    @OperateLog("删除公告")
    @PostMapping("/notice/del")
    public String delNotice(@RequestParam Long id) {
        noticeService.delNotice(id);
        return "success";
    }

    /**
     * 导出作品信息
     *
     * @param response 响应
     * @param comId    比赛id
     */
    @OperateLog("导出参赛信息")
    @GetMapping("/data/exportComInfo")
    public void exportFile(HttpServletResponse response, @RequestParam Long comId) {
        fileService.exportComInfo(response, comId);
    }

    /**
     * 导出作品文件
     *
     * @param comId    比赛id
     * @param userCode 队长学号
     * @param response 响应
     */
    @OperateLog("导出作品文件")
    @GetMapping("/data/exportWork")
    public void exportWork(@RequestParam Long comId, @RequestParam String userCode, HttpServletResponse response) {
        try {
            adminService.download(response, comId, userCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
