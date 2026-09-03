package com.sast.crs.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.util.MapUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.sast.crs.constant.CommonConst;
import com.sast.crs.entity.*;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.enums.UserRoleEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.*;
import com.sast.crs.model.WorkForExcel;
import com.sast.crs.model.WorkOutput;
import com.sast.crs.service.FileService;
import com.sast.crs.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FileServiceImpl implements FileService {
    private final FileUtil fileUtil;
    private final FileMapper fileMapper;
    private final WorkMapper workMapper;
    private final AdminMapper adminMapper;

    public FileServiceImpl(FileUtil fileUtil, FileMapper fileMapper, WorkMapper workMapper, AdminMapper adminMapper) {
        this.fileUtil = fileUtil;
        this.fileMapper = fileMapper;
        this.workMapper = workMapper;
        this.adminMapper = adminMapper;
    }

    @Override
    public void exportComInfo(HttpServletResponse response, Long comId) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("参赛信息" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll(
                    "\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            QueryWrapper<Competition> competitionQueryWrapper = new QueryWrapper<>();
            competitionQueryWrapper.eq("id", comId);
            Competition competition = adminMapper.selectOne(competitionQueryWrapper);//todo
            // 比赛最大人数，用于生成 excel 分配队员列数
            Integer maxTeamMembers = competition.getMaxTeamMembers();
            // 获取excel数据
            List<List<String>> dataList = dataList(comId, maxTeamMembers, competition.getName());
            // 这里需要设置不关闭流
            EasyExcel.write(response.getOutputStream()).head(head(maxTeamMembers)).autoCloseStream(Boolean.FALSE).sheet("sheet1").doWrite(dataList);
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
     * 创建excel表标题
     *
     * @param maxMember 当前活动作品的最大队员人数
     * @return 标题
     */
    private List<List<String>> head(Integer maxMember) {
        List<List<String>> list = ListUtils.newArrayList();
        List<String> workId = ListUtils.newArrayList();
        workId.add("作品ID");
        List<String> comName = ListUtils.newArrayList();
        comName.add("比赛名称");
        List<String> teamName = ListUtils.newArrayList();
        teamName.add("队伍名称");
        List<String> leaName = ListUtils.newArrayList();
        leaName.add("队长名字");
        list.add(workId);
        list.add(comName);
        list.add(teamName);
        list.add(leaName);
        for (int i = 1; i < maxMember + 1; ++i) {
            List<String> member = ListUtils.newArrayList();
            member.add("队员" + i + "名字");
            list.add(member);
        }
        List<String> workName = ListUtils.newArrayList();
        workName.add("作品名称");
        list.add(workName);
        return list;
    }

    /**
     * 获取excel标题数据
     *
     * @param comId 活动id
     * @return 导出参赛信息excel所需要的数据：作品ID、比赛名称、队长学号、队员学号、作品名称、项目类别
     */
    private List<List<String>> dataList(Long comId, Integer maxTeamMembers, String competitionName) {
        List<List<String>> dataList = ListUtils.newArrayList();
        List<WorkForExcel> workList = workMapper.selectListForExcel(comId);
        if (workList.isEmpty()) {
            throw new LocalRuntimeException(ErrorEnum.FILE_NOT_EXIST);
        }
        for (WorkForExcel work : workList) {
            List<String> tempData = ListUtils.newArrayList();
            // 作品ID
            tempData.add(work.getId().toString());
            // 比赛名称
            tempData.add(competitionName);
            // 队伍名称
            tempData.add(work.getTeamName());
            //队长名字
            tempData.add(work.getCaptainName());
            // 查找队员
            JSONArray members = JSONArray.parseArray(work.getMemberJson());
            List<JSONObject> memMap = members == null ? new ArrayList<>() : members.toJavaList(JSONObject.class);
            for (JSONObject map : memMap) {
                String memName = map.getString("name");
                tempData.add(memName);
            }
            if (maxTeamMembers > memMap.size()) {
                for (int i = 0; i < maxTeamMembers - memMap.size(); ++i) {
                    tempData.add("空");
                }
            }
            tempData.add(work.getWorkName());
            tempData.add(getWorkType(work.getSchemaContent()));
            dataList.add(tempData);
        }
        return dataList;
    }

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Override
    public List<WorkOutput> exportFileData(Long comId) {
        QueryWrapper<Work> workQueryWrapper = new QueryWrapper<>();
        workQueryWrapper.select("id", "work_name", "schema_content").eq("com_id", comId);
        List<Work> workList = workMapper.selectList(workQueryWrapper);
        ArrayList<WorkOutput> outputs = new ArrayList<>();
        if (workList.isEmpty()) {
            throw new LocalRuntimeException(ErrorEnum.FILE_NOT_EXIST);
        }

        for (Work work : workList) {
            WorkOutput workOutput = new WorkOutput();
            workOutput.setName(work.getWorkName());
            workOutput.setId(work.getId());
            workOutput.setType(getWorkType(work));
            outputs.add(workOutput);
        }

        return outputs;
    }

    private String getWorkType(Work work) {
        try {
            JsonNode node = objectMapper.readTree(work.getSchemaContent());
            for (JsonNode sub : node) {
                if (CommonConst.WORK_TYPE.equals(sub.get("input").asText())) {
                    return sub.get("content").asText();
                }
            }
        } catch (Exception e) {
            throw new LocalRuntimeException("解析作品类型失败");
        }
        return null;
    }

    private String getWorkType(String schemaContent) {
        try {
            JsonNode node = objectMapper.readTree(schemaContent);
            for (JsonNode sub : node) {
                if (CommonConst.WORK_TYPE.equals(sub.get("input").asText())) {
                    return sub.get("content").asText();
                }
            }
        } catch (Exception e) {
            throw new LocalRuntimeException("解析作品类型失败");
        }
        return null;
    }

    @Override
    public String getDownloadCertificate(User user, String url) {
        if (StringUtils.isEmpty(url)) {
            log.warn("文件下载地址为空");
            throw new LocalRuntimeException(ErrorEnum.FILE_NOT_EXIST);
        }
        url = URLDecoder.decode(url, StandardCharsets.UTF_8);
        File file = fileMapper.selectOne(new LambdaQueryWrapper<File>().eq(File::getUrl, url));
        if (file == null) {
            log.warn("地址无法在数据库中找到，URL： {} ", url);
            throw new LocalRuntimeException(ErrorEnum.FILE_NOT_EXIST);
        }
        if (user.getRole().equals(UserRoleEnum.COMMON_STUDENT.getRole()) && !user.getCode().equals(file.getUserCode())) {
            // 普通用户只能下载自己上传的文件
            log.warn("用户无权限下载文件，URL： {} ", url);
            throw new LocalRuntimeException(ErrorEnum.FILE_NOT_EXIST);
        }
        return fileUtil.getDownloadCertificate(url);
    }
}
