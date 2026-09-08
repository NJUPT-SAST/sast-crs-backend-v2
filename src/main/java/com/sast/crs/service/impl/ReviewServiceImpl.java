package com.sast.crs.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.entity.UserInfo;
import com.sast.crs.enums.UserRoleEnum;
import com.sast.crs.mapper.ReviewMapper;
import com.sast.crs.model.*;
import com.sast.crs.service.ReviewService;
import com.sast.crs.util.AccountImportUtil;
import com.sast.crs.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private AccountImportUtil accountImportUtil;


    @Override
    public PageInfo<ComListForReview> getCompetitionList(Integer pageNum, String code, Integer depId) {
        //使用MybatisPlus插件进行分页操作
        Page<ComListForReview> page = new Page<>(pageNum, 10);
        IPage<ComListForReview> pages = reviewMapper.getComInfo(page, code, depId);
        //重新包装
        Integer total = Math.toIntExact(pages.getTotal());
        Integer current = Math.toIntExact(pages.getCurrent());
        Integer pageSize = Math.toIntExact(pages.getSize());
        Integer pageTotal = Math.toIntExact(pages.getPages());
        List<ComListForReview> list = pages.getRecords();
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
                accessories.add(new Accessories(FileUtil.getOriginalFilename(sUrl), sUrl));
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
