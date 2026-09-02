package com.sast.crs.controller;

import com.sast.crs.annotation.CheckRole;
import com.sast.crs.annotation.OperateLog;
import com.sast.crs.entity.User;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.enums.UserRoleEnum;
import com.sast.crs.interceptor.UserInterceptor;
import com.sast.crs.model.ComListForReview;
import com.sast.crs.model.PageInfo;
import com.sast.crs.model.ProgramInfoForReview;
import com.sast.crs.model.ProgramListForReview;
import com.sast.crs.response.GlobalResponse;
import com.sast.crs.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/review")
@CheckRole(UserRoleEnum.REVIEW)
public class ReviewController {

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    private final ReviewService reviewService;

    @OperateLog("获取审核比赛列表")
    @GetMapping("/competition-list")
    public Object GetCompetitionList(@RequestParam(defaultValue = "1") Integer page) {
        User user = UserInterceptor.userHolder.get();
        //交由service处理
        PageInfo<ComListForReview> result = reviewService.getCompetitionList(page, user.getCode(), user.getDepId());
        if (result.getTotal().equals(0)) {
            return GlobalResponse.failure(ErrorEnum.NO_RESULT);
        }
        return GlobalResponse.success(result);
    }

    @OperateLog("获取审核作品列表")
    @GetMapping("/program-list")
    public Object GetProgramList(@NotNull Integer comId, @NotNull Integer page) {
        User user = UserInterceptor.userHolder.get();
        //判定权限并交Service处理
        //从service获取分页信息
        PageInfo<ProgramListForReview> result = reviewService.getProgramList(user.getCode(), comId, page);
        if (result == null) {
            return GlobalResponse.failure(ErrorEnum.NO_RESULT);
        }
        return GlobalResponse.success(result);
    }

    @OperateLog("获取审核作品")
    @GetMapping("/program-info")
    public Object GetProgramInfo(@NotNull Integer id) {
        ProgramInfoForReview programInfo = reviewService.getProgramInfo(id);
        if (programInfo == null) {
            return GlobalResponse.failure("没有该比赛");
        }
        return GlobalResponse.success(programInfo);
    }

    @OperateLog("提交审核信息")
    @PostMapping("/upload")
    public Object uploadReview(@NotNull Integer id, @NotNull Boolean accept, String opinion) {
        User user = UserInterceptor.userHolder.get();
        if (reviewService.updateReview(user.getCode(), id, accept, opinion)) {
            return GlobalResponse.success();
        }
        return GlobalResponse.failure(ErrorEnum.COMMON_ERROR);
    }

    @OperateLog("红点信息")
    @GetMapping("/red-point")
    public Object redPoint() {
        User user = UserInterceptor.userHolder.get();
        return GlobalResponse.success(reviewService.redPoint(user.getDepId()));
    }

    @OperateLog("获取待审核总数")
    @GetMapping("total")
    public Object total(Integer comId) {
        User user = UserInterceptor.userHolder.get();
        return GlobalResponse.success(reviewService.getTotal(user.getCode(), comId));
    }

    @OperateLog("导入学生账号并导出excel")
    @PostMapping("/import")
    @Transactional
    public List<Map<String, String>> importStudent(@RequestBody MultipartFile file, HttpServletResponse response) throws IOException {
        User user = UserInterceptor.userHolder.get();
        return reviewService.importStudent(file, user.getDepId(), response);
    }


}
