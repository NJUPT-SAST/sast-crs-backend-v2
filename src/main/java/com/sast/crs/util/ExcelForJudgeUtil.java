package com.sast.crs.util;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sast.crs.entity.*;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.*;
import com.sast.crs.service.JudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class ExcelForJudgeUtil extends AnalysisEventListener<Map<Integer, String>> {
    @Resource
    JudgeService judgeService;
    @Resource
    JudgeMapper judgeMapper;
    @Resource
    WorkMapper workMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    ReviewMapper reviewMapper;
    @Resource
    AdminMapper adminMapper;

    private static final int BATCH_COUNT = 10;
    private List<Judge> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
        log.info("解析到一条数据:{}", JSON.toJSONString(data));
        // 作品id
        Long id = Long.valueOf(data.get(0));
        QueryWrapper<Work> workQueryWrapper = new QueryWrapper<>();
        workQueryWrapper.eq("id", id);
        // 判断作品id是否存在
        if (!workMapper.exists(workQueryWrapper)) {
            throw new LocalRuntimeException(ErrorEnum.WORK_NOT_EXIST);
        }
        String userCode = getUserCode(id);
        // 活动id
        Long comId = workMapper.selectById(id).getComId();
        QueryWrapper<Review> reviewQueryWrapper = new QueryWrapper<>();
        reviewQueryWrapper.eq("com_id", comId).eq("user_code", userCode);
        Review review = reviewMapper.selectOne(reviewQueryWrapper);
        QueryWrapper<Competition> competitionQueryWrapper = new QueryWrapper<>();
        competitionQueryWrapper.eq("id", comId).select("is_review");
        Competition competition = adminMapper.selectOne(competitionQueryWrapper);
        // 判断比赛是否需要审核和作品是否审核，如果不需要则跳过这条数据
        if (competition.getIsReview() && (review.getAccept() == null || !review.getAccept())) {
            log.warn("作品id:{}未通过审核，跳过分配评委", id);
            return;
        }
        QueryWrapper<Judge> judgeQueryWrapper = new QueryWrapper<>();
        judgeQueryWrapper.eq("user_code", userCode).eq("com_id", comId);
        // 判断这个比赛是否已经分配评委
        boolean isUpdate = judgeMapper.exists(judgeQueryWrapper);
        // 已经分配评委的话此时需要覆盖掉这些数据
        if (isUpdate) {
            judgeMapper.delete(judgeQueryWrapper);
        }

        Set<String> codes = new HashSet<>();
        // excel表从第三格开始为评委学号
        for (int i = 2; i < data.size(); i++) {
            String judgeCode = data.get(i);
            codes.add(judgeCode);
        }
        for (String judgeCode : codes) {
            // 判断评委学号是否存在
            if (!userIsExist(judgeCode)) {
                log.error("学号{}不存在", judgeCode);
                throw new LocalRuntimeException(ErrorEnum.USER_NOT_EXIST);
            }
            Judge judge = new Judge();
            judge.setJudgeCode(judgeCode);
            judge.setComId(comId);
            judge.setUserCode(userCode);
            cachedDataList.add(judge);
        }
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            addJudges();
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        addJudges();
        log.info("所有数据解析完成！");
    }

    private void addJudges() {
        judgeService.saveBatch(cachedDataList);
        log.info("存储数据库成功！");
        cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    }

    private String getUserCode(Long id) {
        QueryWrapper<Work> wrapper = new QueryWrapper<>();
        wrapper.select("user_code").eq("id", id);
        return workMapper.selectOne(wrapper).getUserCode();
    }

    /**
     * 判断是否存在这个用户
     *
     * @param userCode 用户学号
     * @return 判断结果
     */
    public boolean userIsExist(String userCode) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("code", userCode);
        return userMapper.exists(wrapper);
    }

    /**
     * 判断该评委是否已分配到该作品
     *
     * @param userCode  作品队长学号
     * @param judgeCode 评委学号
     * @param comId     比赛id
     * @return 判断结果
     */
    public boolean judgeIsAssign(String userCode, String judgeCode, Long comId) {
        QueryWrapper<Judge> wrapper = new QueryWrapper<>();
        wrapper.eq("judge_code", judgeCode).eq("user_code", userCode).eq("com_id", comId);
        return judgeMapper.exists(wrapper);
    }
}
