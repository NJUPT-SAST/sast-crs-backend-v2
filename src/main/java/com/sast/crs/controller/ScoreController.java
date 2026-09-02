package com.sast.crs.controller;

import com.sast.crs.annotation.CheckRole;
import com.sast.crs.annotation.OperateLog;
import com.sast.crs.entity.User;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.enums.UserRoleEnum;
import com.sast.crs.interceptor.UserInterceptor;
import com.sast.crs.model.ComListForScore;
import com.sast.crs.model.PageInfo;
import com.sast.crs.model.ProgramInfoForScore;
import com.sast.crs.model.ProgramListForScore;
import com.sast.crs.response.GlobalResponse;
import com.sast.crs.service.ScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/score")
@CheckRole(UserRoleEnum.JUDGE)
public class ScoreController {

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    private final ScoreService scoreService;


    @OperateLog("获取评分比赛列表")
    @GetMapping("/competition-list")
    public Object getCompetitionList(@NotNull Integer page) {
        User user = UserInterceptor.userHolder.get();
        //交由service处理
        PageInfo<ComListForScore> comList = scoreService.getCompetitionList(user.getCode(), page);
        if (comList.getTotal().equals(0)) {
            return GlobalResponse.failure(ErrorEnum.NO_RESULT);
        }
        return GlobalResponse.success(comList);
    }

    @OperateLog("获取评分项目列表")
    @GetMapping("/program-list")
    public Object getProgramList(@NotNull Integer comId, @NotNull Integer page) {
        User user = UserInterceptor.userHolder.get();
        //交由Service处理
        PageInfo<ProgramListForScore> proList = scoreService.getProgramList(user.getCode(), comId, page);
        if (proList.getTotal().equals(0)) {
            return GlobalResponse.failure(ErrorEnum.NO_RESULT);
        }
        return GlobalResponse.success(proList);
    }

    @OperateLog("获取评分项目信息")
    @GetMapping("/program-info")
    public Object getProgramInfo(@NotNull Integer id) {
        User user = UserInterceptor.userHolder.get();
        //判定权限并交由service处理
        if (scoreService.confirmPro(user.getCode(), id)) {
            ProgramInfoForScore ob = scoreService.getProgramInfo(id, user.getCode());
            if (ob == null) {
                return GlobalResponse.failure(ErrorEnum.NO_RESULT);
            }
            return GlobalResponse.success(ob);
        }
        return GlobalResponse.failure(ErrorEnum.NO_ROLE);
    }

    @OperateLog("提交评分信息")
    @PostMapping("/upload")
    public Object uploadScore(@NotNull Integer id, @NotNull Integer score, String opinion) {
        User user = UserInterceptor.userHolder.get();
        if (scoreService.uploadScore(user.getCode(), id, score, opinion)) {
            return GlobalResponse.success();
        }
        return GlobalResponse.failure(ErrorEnum.COMMON_ERROR);
    }

    @OperateLog("红点信息")
    @GetMapping("/red-point")
    public Object redPoint() {
        User user = UserInterceptor.userHolder.get();
        return GlobalResponse.success(scoreService.redPoint(user.getCode()));
    }

    @OperateLog("获取待评分作品总数")
    @ResponseBody
    @GetMapping("total")
    public Object total(Integer comId) {
        //从JWT中获取学工号
        User user = UserInterceptor.userHolder.get();
        String code = user.getCode();
        //交由service处理
        return GlobalResponse.success(scoreService.getTotal(code, comId));
    }
}
