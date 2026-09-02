package com.sast.crs.util;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sast.crs.entity.Competition;
import com.sast.crs.entity.User;
import com.sast.crs.mapper.AdminMapper;
import com.sast.crs.mapper.UserMapper;
import com.sast.crs.mapper.WhiteListMapper;
import com.sast.crs.model.WhiteList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.*;

@Slf4j
@Component
public class ExcelForWhiteListUtil extends AnalysisEventListener<Map<Integer, String>> {

    @Resource
    WhiteListMapper whiteListMapper;

    @Resource
    UserMapper userMapper;

    @Resource
    AdminMapper adminMapper;

    private Long comId;
    private Boolean isWhiteList;

    private static final int BATCH_COUNT = 25;

    private List<String> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    public void setComId(Long comId) {
        this.comId = comId;
    }

    public void setIsWhiteList(Boolean isWhiteList) {
        this.isWhiteList = isWhiteList;
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
        // 如果不设置白名单，则删除已设置的白名单
        if (!isWhiteList) {
            QueryWrapper<WhiteList> whiteListQueryWrapper = new QueryWrapper<>();
            whiteListQueryWrapper.eq("com_id", comId);
            if (whiteListMapper.exists(whiteListQueryWrapper)) {
                whiteListMapper.delete(whiteListQueryWrapper);
            }
            return;
        }

        log.info("解析到一条数据:{}", JSON.toJSONString(data));
        // 判断用户是否存在
//            if (!userIsExist(b)) {
//                log.error("学生{}不存在", b);
//                throw new LocalRuntimeException(ErrorEnum.USER_NOT_EXIST);
//            }
        cachedDataList.add(data.get(0));
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        updateCom(isWhiteList);
        saveData();
    }

    /**
     * 保存数据到数据库
     */
    public void saveData() {
        if (cachedDataList.isEmpty()) return;
        log.info("{}条数据，开始存储数据库", cachedDataList.size());
        WhiteList whiteList = new WhiteList(null, comId, cachedDataList);
        whiteListMapper.insert(whiteList);
        log.info("存储成功！");
        cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    }

    /**
     * 更新数据库中的数据
     */
    public void updateData() {
        log.info("{}条数据，开始存储数据库", cachedDataList.size());
        WhiteList whiteList = new WhiteList(null, comId, cachedDataList);
        QueryWrapper<WhiteList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("com_id", comId);
        whiteListMapper.update(whiteList, queryWrapper);
        log.info("更新成功！");
        cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
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
     * 更新比赛中的isWhiteList字段
     *
     * @param isWhiteList 是否设置名单
     */
    public void updateCom(Boolean isWhiteList) {
        QueryWrapper<Competition> competitionQueryWrapper = new QueryWrapper<>();
        competitionQueryWrapper.eq("id", comId);
        Competition competition = adminMapper.selectOne(competitionQueryWrapper);
        competition.setIsWhiteList(isWhiteList);
        adminMapper.updateById(competition);
    }
}
