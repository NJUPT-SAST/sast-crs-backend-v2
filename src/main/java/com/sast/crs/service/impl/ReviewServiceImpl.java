package com.sast.crs.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.entity.Competition;
import com.sast.crs.entity.UserInfo;
import com.sast.crs.enums.UserRoleEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.CompetitionMapper;
import com.sast.crs.mapper.ReviewMapper;
import com.sast.crs.model.*;
import com.sast.crs.service.ReviewService;
import com.sast.crs.util.AccountImportUtil;
import com.sast.crs.util.COSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private CompetitionMapper competitionMapper;
    @Autowired
    private AccountImportUtil accountImportUtil;


    @Override
    public PageInfo<ComListForReview> getCompetitionList(Integer pageNum, String code) {
        //使用MybatisPlus插件进行分页操作
        Page<ComListForReview> page = new Page<>(pageNum, 10);
        IPage<ComListForReview> pages = reviewMapper.getComInfo(page, code);
        // 总数/已审核数按与作品列表一致的口径统计：review_settings 中映射给该审核人的学院
        List<ComListForReview> list = pages.getRecords();
        for (ComListForReview record : list) {
            JSONObject settings = reviewMapper.confirm(record.getId()).getJSONObject("review_settings");
            if (settings == null) {
                record.setTotalNum(0);
                record.setCompletedNum(0);
                continue;
            }
            String setting = settings.getString("0");
            List<Integer> depIds = new ArrayList<>();
            if (setting != null && Objects.equals(setting, code)) {
                // 总审核人：统计未分配给其他审核人的学院
                for (String key : settings.keySet()) {
                    if (!settings.getString(key).equals(code) & !key.equals("0")) {
                        depIds.add(Integer.parseInt(key));
                    }
                }
                if (depIds.isEmpty()) {
                    // 没有排除任何学院，用哨兵值使 NOT IN 命中全部作品
                    depIds.add(-1);
                }
                record.setTotalNum(reviewMapper.getScopeTotalNotIn(depIds, record.getId()));
                record.setCompletedNum(reviewMapper.getScopeDoneNotIn(depIds, record.getId()));
            } else {
                // 普通审核人：统计映射给自己的学院
                for (String key : settings.keySet()) {
                    if (settings.getString(key).equals(code)) {
                        depIds.add(Integer.parseInt(key));
                    }
                }
                if (depIds.isEmpty()) {
                    // 没有映射到自己的学院，用哨兵值使 IN 恒不命中
                    depIds.add(-1);
                }
                record.setTotalNum(reviewMapper.getScopeTotal(depIds, record.getId()));
                record.setCompletedNum(reviewMapper.getScopeDone(depIds, record.getId()));
            }
        }
        //重新包装
        Integer total = Math.toIntExact(pages.getTotal());
        Integer current = Math.toIntExact(pages.getCurrent());
        Integer pageSize = Math.toIntExact(pages.getSize());
        Integer pageTotal = Math.toIntExact(pages.getPages());
        //返回结果
        return new PageInfo<>(total, list, current, pageSize, pageTotal);
    }

    //这段注释
    @Override
    public PageInfo<ProgramListForReview> getProgramList(String code, Integer comId, Integer pageNum) {
        JSONObject confirm = reviewMapper.confirm(comId);
        if (confirm == null) {
            return null;
        }
        JSONObject settings = confirm.getJSONObject("review_settings");
        String setting = settings.getString("0");
        Page<ProgramListForReview> page = new Page<>(pageNum, 10);
        page.setOptimizeCountSql(false);
        IPage<ProgramListForReview> pages;
        if (setting != null && Objects.equals(setting, code)) {
            List<Integer> depIds = new ArrayList<>();
            for (String key : settings.keySet()) {
                if (!settings.getString(key).equals(code) & !key.equals("0")) {
                    depIds.add(Integer.parseInt(key));
                }
            }
            pages = reviewMapper.getProgramInfoNotIn(page, comId, depIds);
        } else {
            List<Integer> depIds = new ArrayList<>();
            for (String key : settings.keySet()) {
                if (settings.getString(key).equals(code)) {
                    depIds.add(Integer.parseInt(key));
                }
            }
            pages = reviewMapper.getProgramInfo(page, comId, depIds);
        }
        //重新包装
        Integer total = Math.toIntExact(pages.getTotal());
        Integer current = Math.toIntExact(pages.getCurrent());
        Integer pageSize = Math.toIntExact(pages.getSize());
        Integer pageTotal = Math.toIntExact(pages.getPages());
        List<ProgramListForReview> recordList = pages.getRecords();
        //返回结果
        return new PageInfo<>(total, recordList, current, pageSize, pageTotal);
    }

    @Override
    public ProgramInfoForReview getProgramInfo(Integer proId) {
        Integer comId = reviewMapper.getComIdByProId(proId);
        String captainId = reviewMapper.getCaptainIdByProId(proId);
        if (comId == null || captainId == null) {
            return null;
        }
        //获取文字信息
        List<Text> texts = new ArrayList<>();
        String SContents = reviewMapper.getContents(comId, captainId);
        if (SContents != null) {
            List<Content> contents = JSON.parseArray(SContents).toJavaList(Content.class);
            for (Content content : contents) {
                if (!content.getIsFile()) {
                    texts.add(new Text(content.getInput(), content.getContent()));
                }
            }
        }
        //获取附件
        List<String> urls = reviewMapper.getAccessories(comId, captainId);
        List<Accessories> accessories = new ArrayList<>();
        if (urls != null) {
            for (Object url : urls) {
                String sUrl = url.toString();
                accessories.add(new Accessories(COSUtil.getOriginalFilename(sUrl), sUrl));
            }
        }
        String teamName = reviewMapper.getTeamName(comId, captainId);
        //成员信息
        String jMembers = reviewMapper.getJMember(comId, captainId);
        //获取成员数量
        JSONArray array = JSON.parseArray(jMembers);
        Integer memberNum = array.size();

        //包装返回
        return new ProgramInfoForReview(teamName, new UserInfo(captainId, reviewMapper.getCaptainName(captainId)), memberNum, array, accessories, texts);
    }

    @Override
    public Boolean updateReview(String code, Integer id, Boolean accept, String opinion) {
        // 校验是否处于评审时间窗内
        Integer comId = reviewMapper.getComIdByProId(id);
        Competition competition = competitionMapper.selectById(comId);
        if (competition == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(competition.getReviewBeginTime())) {
            throw new LocalRuntimeException("评审尚未开始");
        }
        if (now.isAfter(competition.getReviewEndTime())) {
            throw new LocalRuntimeException("评审已截止");
        }
        return reviewMapper.updateReview(code, id, accept, opinion) > 0;
    }

    @Override
    public Boolean redPoint(Integer depId) {
        Integer rCount = reviewMapper.getReviewCount();
        Integer cCount = reviewMapper.getComCount();
        return rCount > cCount;
    }

    @Override
    public Integer getTotal(String code, Integer comId) {
        JSONObject settings = reviewMapper.confirm(comId).getJSONObject("review_settings");
        String setting = settings.getString("0");
        List<Integer> list = new ArrayList<>();
        if (setting != null & Objects.equals(setting, code)) {
            for (String key : settings.keySet()) {
                if (!settings.getString(key).equals(code) & !key.equals("0")) {
                    list.add(Integer.parseInt(key));
                }
            }
            return reviewMapper.getTotalNotIn(list, comId);
        }
        for (String key : settings.keySet()) {
            if (settings.getString(key).equals(code)) {
                list.add(Integer.parseInt(key));
            }
        }
        return reviewMapper.getTotal(list, comId);
    }

    @Override
    public List<Map<String, String>> importStudent(MultipartFile file, Integer depId, HttpServletResponse response) throws IOException {
        return accountImportUtil.importAccounts(file, depId, UserRoleEnum.COMMON_STUDENT.getRole(), "学生");
    }
}
