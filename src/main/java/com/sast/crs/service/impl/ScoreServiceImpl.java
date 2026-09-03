package com.sast.crs.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.util.MapUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.constant.CommonConst;
import com.sast.crs.entity.*;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.*;
import com.sast.crs.model.*;
import com.sast.crs.service.ScoreService;
import com.sast.crs.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ScoreServiceImpl implements ScoreService {

    @Autowired
    private ScoreMapper scoreMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Override
    public PageInfo<ComListForScore> getCompetitionList(String code, Integer pageNum) {
        //使用MybatisPlus插件进行分页操作
        Page<ComListForScore> page = new Page<>(pageNum, 10);
        IPage<ComListForScore> pages = scoreMapper.getComInfo(page, code);
        //重新包装
        Integer total = Math.toIntExact(pages.getTotal());
        Integer current = Math.toIntExact(pages.getCurrent());
        Integer pageSize = Math.toIntExact(pages.getSize());
        Integer pageTotal = Math.toIntExact(pages.getPages());
        List<ComListForScore> list = pages.getRecords();
        //返回结果
        return new PageInfo<>(total, list, current, pageSize, pageTotal);
    }

    @Override
    public PageInfo<ProgramListForScore> getProgramList(String code, Integer comId, Integer pageNum) {
        //使用MybatisPlus插件进行分页操作
        Page<ProgramListForScore> page = new Page<>(pageNum, 10);
        //mbp无法优化sql语句，于是关闭此自动优化
        page.setOptimizeCountSql(false);
        IPage<ProgramListForScore> pages = scoreMapper.getProList(page, code, comId);
        //重新包装
        Integer total = Math.toIntExact(pages.getTotal());
        Integer current = Math.toIntExact(pages.getCurrent());
        Integer pageSize = Math.toIntExact(pages.getSize());
        Integer pageTotal = Math.toIntExact(pages.getPages());
        //返回结果
        return new PageInfo<>(total, pages.getRecords(), current, pageSize, pageTotal);
    }

    @Override
    public ProgramInfoForScore getProgramInfo(Integer proId, String judgeCode) {
        Integer comId = scoreMapper.getComIdByProId(proId);
        String captainId = scoreMapper.getUserCode(proId);
        if (comId == null || captainId == null) {
            return null;
        }
        //成员信息
        String jMembers = reviewMapper.getJMember(comId, captainId);
        //获取成员数量
        JSONArray array = JSON.parseArray(jMembers);
        Integer memberNum = array.size();
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
        //获取评分
        Integer score = scoreMapper.getScoreInfo(comId, captainId, judgeCode);
        String opinion = scoreMapper.getOpinionInfo(comId, captainId, judgeCode);
        //获取附件
        List<String> urls = reviewMapper.getAccessories(comId, captainId);
        List<Accessories> accessories = new ArrayList<>();
        for (Object url : urls) {
            String sUrl = url.toString();
            accessories.add(new Accessories(FileUtil.getOriginalFilename(sUrl), sUrl));
        }
        //获取队伍名
        String teamName = reviewMapper.getTeamName(comId, captainId);
        //包装返回
        return new ProgramInfoForScore(teamName, new UserInfo(captainId, reviewMapper.getCaptainName(captainId)), memberNum, array, accessories, texts, score, opinion);
    }

    @Override
    public Boolean uploadScore(String teacherCode, Integer proId, Integer score, String opinion) {
        //获取比赛相关信息
        Integer comId = scoreMapper.getComIdByProId(proId);
        String userCode = scoreMapper.getUserCode(proId);
        //处理提交相关字段
        if (userCode != null && comId != null) {
            if (scoreMapper.isExistence(comId, userCode, teacherCode)) {
                return scoreMapper.updateScore(comId, teacherCode, userCode, score, opinion) > 0;
            }
            return scoreMapper.upload(teacherCode, userCode, comId, score, opinion) > 0;
        }
        return false;
    }

    @Override
    public Boolean redPoint(String code) {
        Integer jCount = scoreMapper.getJudgeCount(code);
        Integer cCount = scoreMapper.getComCount(code);
        return jCount > cCount;
    }

    @Override
    public Boolean confirmPro(String code, Integer id) {
        return scoreMapper.getJCount(code, id) > 0;
    }


    @Override
    public Integer getTotal(String code, Integer comId) {
        return scoreMapper.getTotal(code, comId) - scoreMapper.getTotalDone(code, comId);
    }

    @Override
    public void exportScore(HttpServletResponse response, Long comId) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("评审结果" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll(
                    "\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            Map<String, Object> dataMap = dataList(comId);
            Integer max = (Integer) dataMap.get("max");
            List<List<Object>> dataList = (List<List<Object>>) dataMap.get("dataList");
            // 这里需要设置不关闭流
            EasyExcel.write(response.getOutputStream()).head(head(max)).autoCloseStream(Boolean.FALSE).sheet("sheet1").doWrite(dataList);
        } catch (Exception e) {
            e.printStackTrace();
            // 重置response
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            Map<String, String> map = MapUtils.newHashMap();
            map.put("status", "failure");
            map.put("message", "下载文件失败" + e.getMessage());
            try {
                response.getWriter().println(com.alibaba.fastjson2.JSON.toJSONString(map));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 创建excel表标题
     * 标题评委数量为这个活动作品所分配的最大评委数
     *
     * @param max 当前活动作品分配的最大评委数
     * @return 标题
     */
    private List<List<String>> head(Integer max) {
        List<List<String>> list = ListUtils.newArrayList();
        List<String> head0 = ListUtils.newArrayList();
        head0.add("作品ID");
        List<String> head1 = ListUtils.newArrayList();
        head1.add("队长学工号");
        List<String> head2 = ListUtils.newArrayList();
        head2.add("队长姓名");
        List<String> head3 = ListUtils.newArrayList();
        head3.add("队长所在部门");
        List<String> head4 = ListUtils.newArrayList();
        head4.add("项目类别");
        list.add(head0);
        list.add(head1);
        list.add(head2);
        list.add(head3);
        list.add(head4);
        // 评委不定，所以这里需要遍历设置
        for (int i = 1; i <= max; ++i) {
            List<String> judgeCode = ListUtils.newArrayList();
            judgeCode.add("评委" + i + "学工号");
            list.add(judgeCode);
            List<String> judgeScore = ListUtils.newArrayList();
            judgeScore.add("评委" + i + "打分");
            list.add(judgeScore);
            List<String> judgeOpinion = ListUtils.newArrayList();
            judgeOpinion.add("评委" + i + "评语");
            list.add(judgeOpinion);
        }
        return list;
    }

    /**
     * 获取excel标题数据
     *
     * @param comId 活动id
     * @return 评审结果excel所需要的数据：作品ID、队长学工号、队长姓名、队长所在部门、项目类别、评委学工号、评委打分、评委评语
     */
    private Map<String, Object> dataList(Long comId) {
        List<ScoreExportRow> rows = scoreMapper.selectExportRows(comId);
        if (rows.isEmpty()) {
            throw new LocalRuntimeException(ErrorEnum.SCORE_NOT_EXIST);
        }
        Map<String, Object> res = new HashMap<>();
        List<List<Object>> dataList = ListUtils.newArrayList();
        Map<String, List<ScoreExportRow>> groupedRows = new LinkedHashMap<>();
        for (ScoreExportRow row : rows) {
            groupedRows.computeIfAbsent(row.getLeaderCode(), key -> new ArrayList<>()).add(row);
        }
        int max = groupedRows.values().stream().mapToInt(List::size).max().orElse(1);
        for (List<ScoreExportRow> group : groupedRows.values()) {
            ScoreExportRow first = group.get(0);
            List<Object> tempData = ListUtils.newArrayList();
            tempData.add(first.getId());
            tempData.add(first.getLeaderCode());
            tempData.add(first.getLeaderName());
            tempData.add(first.getDepName());
            tempData.add(getWorkType(first.getSchemaContent()));
            for (ScoreExportRow judge : group) {
                tempData.add(judge.getJudgeCode());
                tempData.add(judge.getScore());
                tempData.add(judge.getOpinion());
            }
            dataList.add(tempData);
        }
        res.put("max", max);
        res.put("dataList", dataList);
        return res;
    }

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private String getWorkType(String schemaContent) {
        try {
            JsonNode node = objectMapper.readTree(schemaContent);
            for (JsonNode sub : node) {
                if (CommonConst.WORK_TYPE.equals(sub.get("input").asString())) {
                    return sub.get("content").asString();
                }
            }
        } catch (Exception e) {
            throw new LocalRuntimeException("解析作品类型失败");
        }
        return null;
    }
}
