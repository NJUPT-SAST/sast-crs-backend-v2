package com.sast.crs.service.impl;

import cn.hutool.core.io.FileTypeUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.entity.*;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.*;
import com.sast.crs.model.ComMangerVo;
import com.sast.crs.model.CompetitionVO;
import com.sast.crs.service.AdminService;
import com.sast.crs.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static com.sast.crs.enums.ErrorEnum.*;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {
    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
    private final FileMapper fileMapper;
    private final ReviewMapper reviewMapper;
    private final TeamMapper teamMapper;
    private final WorkMapper workMapper;
    private final DepartmentMapper departmentMapper;
    private final FileUtil fileUtil;

    public AdminServiceImpl(AdminMapper adminMapper, UserMapper userMapper, FileMapper fileMapper,
                            ReviewMapper reviewMapper, TeamMapper teamMapper,
                            WorkMapper workMapper, DepartmentMapper departmentMapper, FileUtil fileUtil) {
        this.adminMapper = adminMapper;
        this.userMapper = userMapper;
        this.fileMapper = fileMapper;
        this.reviewMapper = reviewMapper;
        this.teamMapper = teamMapper;
        this.workMapper = workMapper;
        this.departmentMapper = departmentMapper;
        this.fileUtil = fileUtil;
    }

    @Override
    public void createContest(Competition competition, MultipartFile cover) {
        // 比较时间设置是否正确
        if (competition.getRegBeginTime().isAfter(competition.getSubmitBeginTime()) ||  // 提交开始时间不早于报名开始时间
                competition.getSubmitBeginTime().isAfter(competition.getReviewBeginTime()) ||  // 评审开始时间不早于提交开始时间
                competition.getRegBeginTime().isAfter(competition.getRegEndTime()) ||  // 报名截止时间不早于报名开始时间
                competition.getRegEndTime().isAfter(competition.getSubmitEndTime()) ||  // 提交截止时间不早于报名截止时间
                competition.getSubmitEndTime().isAfter(competition.getReviewEndTime())) {  // 评审截止时间不早于提交截止时间
            throw new LocalRuntimeException(ErrorEnum.DATE_ERROR);
        }

        // 校验审批关系数据是否正确
        // 主要是判断部门跟用户是否存在
        Map<String, String> settings = competition.getReviewSettings();
        if (settings == null) {
            throw new LocalRuntimeException(REVIEW_SETTINGS_ERROR);
        }
        settings.forEach((s, o) -> {
            boolean userRes = userIsExist(o);
            if (!"0".equals(s)) {
                boolean depRes = depIsExist(Integer.valueOf(s));
                if (!depRes) {
                    throw new LocalRuntimeException(ErrorEnum.DEP_NOT_EXIST);
                }
            }
            if (!userRes) {
                throw new LocalRuntimeException(ErrorEnum.USER_NOT_EXIST);
            }
        });
        // 判断活动负责人是否存在
        if (!userIsExist(competition.getUserCode())) {
            throw new LocalRuntimeException(USER_NOT_EXIST);
        }
        // 判断比赛团队人数限制是否正确
        if (competition.getMinTeamMembers() > competition.getMaxTeamMembers()) {
            throw new LocalRuntimeException(LIMIT_ERROR);
        }
        // 判断比赛表单是否为空
        if (competition.getTable() == null) {
            throw new LocalRuntimeException(SCHEMA_ERROR);
        }
        int result = adminMapper.insert(competition);
        // 是否成功插入到数据库
        if (result <= 0) {
            throw new LocalRuntimeException(ErrorEnum.CONTEST_ERROR);
        }
        if (cover != null && !cover.isEmpty()) {
            String url = writeUploadImage(cover, competition.getId());
            competition.setCover(url);
            adminMapper.updateById(competition);
        }
    }

    @Override
    public void editContest(Competition competition, MultipartFile cover) {
        // 比较时间设置是否正确
        if (competition.getRegBeginTime().isAfter(competition.getSubmitBeginTime()) ||  // 提交开始时间不早于报名开始时间
                competition.getSubmitBeginTime().isAfter(competition.getReviewBeginTime()) ||  // 评审开始时间不早于提交开始时间
                competition.getRegBeginTime().isAfter(competition.getRegEndTime()) ||  // 报名截止时间不早于报名开始时间
                competition.getRegEndTime().isAfter(competition.getSubmitEndTime()) ||  // 提交截止时间不早于报名截止时间
                competition.getSubmitEndTime().isAfter(competition.getReviewEndTime())) {  // 评审截止时间不早于提交截止时间
            throw new LocalRuntimeException(ErrorEnum.DATE_ERROR);
        }
        QueryWrapper<Competition> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", competition.getId());
        Competition temCompetition = adminMapper.selectOne(queryWrapper);
        if (temCompetition == null) {
            throw new LocalRuntimeException(CONTEST_NOT_EXIST);
        }

        // 校验审批关系数据是否正确
        // 主要是判断部门跟用户是否存在
        Map<String, String> settings = competition.getReviewSettings();
        if (settings == null) {
            throw new LocalRuntimeException(REVIEW_SETTINGS_ERROR);
        }
        settings.forEach((s, o) -> {
            boolean userRes = userIsExist(o);
            if (!"0".equals(s)) {
                boolean depRes = depIsExist(Integer.valueOf(s));
                if (!depRes) {
                    throw new LocalRuntimeException(ErrorEnum.DEP_NOT_EXIST);
                }
            }
            if (!userRes) {
                throw new LocalRuntimeException(ErrorEnum.USER_NOT_EXIST);
            }
        });
        // 判断活动负责人是否存在
        if (!userIsExist(competition.getUserCode())) {
            throw new LocalRuntimeException(USER_NOT_EXIST);
        }
        // 判断比赛团队人数限制是否正确
        if (competition.getMinTeamMembers() > competition.getMaxTeamMembers()) {
            throw new LocalRuntimeException(LIMIT_ERROR);
        }
        // 判断比赛表单是否为空
        if (competition.getTable() == null) {
            throw new LocalRuntimeException(SCHEMA_ERROR);
        }

        if (cover != null && !cover.isEmpty()) {
            String url = writeUploadImage(cover, competition.getId());
            competition.setCover(url);
        }

        int result = adminMapper.updateById(competition);
        if (result <= 0) {
            throw new LocalRuntimeException(ErrorEnum.CONTEST_ERROR);
        }
    }

    @Override
    public JSONObject getSchema(Long comId) {
        Competition competition = adminMapper.selectById(comId);
        // 比赛不存在
        if (competition == null) {
            throw new LocalRuntimeException(CONTEST_NOT_EXIST);
        }
        return competition.getTable();
    }

    @Override
    public void deleteContest(Long id) {
        QueryWrapper<Competition> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        Competition competition = adminMapper.selectOne(queryWrapper);
        if (competition == null) {
            throw new LocalRuntimeException(CONTEST_NOT_EXIST);
        }
        adminMapper.delete(queryWrapper);
    }

    @Override
    public Map<String, Object> getContestList(Integer pageNum, Integer pageSize) {
        Page<CompetitionVO> Page = new Page<>(pageNum, pageSize);
        IPage<CompetitionVO> contestListPage = adminMapper.getContestListPage(Page);
        List<CompetitionVO> resList = contestListPage.getRecords();
        return getResultMap(resList, contestListPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<String, Object> getComMangerInfo(Integer pageNum, Integer pageSize, Long comId) {
        // 比赛名
        QueryWrapper<Competition> competitionQueryWrapper = new QueryWrapper<>();
        competitionQueryWrapper.eq("id", comId);
        String comName = adminMapper.selectOne(competitionQueryWrapper).getName();
        // 注册数
        Long regNum = teamMapper.selectCount(new QueryWrapper<Team>().eq("com_id", comId));
        // 提交材料数
        Long subNum = workMapper.selectCount(new QueryWrapper<Work>().eq("com_id", comId));
        // 已审批数
        Long revNum = reviewMapper.getReviewNum(comId);
        Page<ComMangerVo> page = new Page<>(pageNum, pageSize);
        page.setOptimizeCountSql(false);
        List<ComMangerVo> resList = adminMapper.getComMangerInfo(page, comId).getRecords();
        // 返回结果集、提交作品数量、报名数、提交材料数、评审数
        return getComMangerMap(resList, Math.toIntExact(subNum), pageNum, pageSize, regNum, subNum, revNum, comName);
    }

    @Override
    public Competition getContestInfo(Long id) {
        Competition competition = adminMapper.selectOne(new QueryWrapper<Competition>().eq("id", id));
        if (competition == null) {
            throw new LocalRuntimeException(CONTEST_NOT_EXIST);
        }
        return competition;
    }

    @Override
    public User getUserInfo(String code) {
        User user = userMapper.selectById(code);
        if (user == null) {
            throw new LocalRuntimeException(ErrorEnum.USER_NOT_EXIST);
        }
        return user;
    }

    @Override
    public void download(HttpServletResponse response, Long comId, String userCode) throws IOException {
        QueryWrapper<File> fileWrapper = new QueryWrapper<>();
        fileWrapper.eq("com_id", comId).eq("user_code", userCode);
        List<File> files = fileMapper.selectList(fileWrapper);
        QueryWrapper<Competition> competitionWrapper = new QueryWrapper<>();
        competitionWrapper.select("name").eq("id", comId);
        String comName = adminMapper.selectOne(competitionWrapper).getName();
        String fileName = comName + '-' + userCode + ".zip";
        fileUtil.downloadPackFile(response, files, fileName);
    }

    public <T> Map<String, Object> getResultMap(List<T> objects, Long num, Integer pageNum, Integer pageSize) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("records", objects);
        resultMap.put("total", num);
        resultMap.put("pageNum", pageNum);
        resultMap.put("pageSize", pageSize);
        return resultMap;
    }

    public Map<String, Object> getComMangerMap(List<ComMangerVo> comMangerVo, int num, Integer pageNum, Integer pageSize, Long regNum, Long subNum, Long revNum, String comName) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("records", comMangerVo);
        resultMap.put("total", num);
        resultMap.put("pageNum", pageNum);
        resultMap.put("pageSize", pageSize);
        resultMap.put("regNum", regNum);
        resultMap.put("subNum", subNum);
        resultMap.put("revNum", revNum);
        // 如果不为空就添加comId
        if (!comMangerVo.isEmpty()) {
            resultMap.put("comId", comMangerVo.get(0).getComId());
        }
        resultMap.put("comName", comName);
        return resultMap;
    }

    /**
     * 判断是否存在这个部门
     *
     * @param id 部门 id
     * @return 判断结果
     */
    public boolean depIsExist(Integer id) {
        QueryWrapper<Department> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        return departmentMapper.exists(wrapper);
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
     * 上传比赛封面图
     *
     * @param cover 比赛封面
     * @param comId 比赛id
     * @return 封面在COS里的url
     */
    public String writeUploadImage(MultipartFile cover, Long comId) {
        // 获取后缀
        String typeName;
        try {
            typeName = FileTypeUtil.getType(cover.getInputStream());
        } catch (IOException e) {
            log.error("获取文件类型出错", e);
            return null;
        }
        if (typeName != null && !isImage(typeName)) {
            throw new LocalRuntimeException(ErrorEnum.INVALID_FILE_TYPE_ERROR);
        }
        return fileUtil.uploadCover(cover, comId);
    }

    /**
     * 格式是否正确
     *
     * @param typeName 文件格式名
     */
    public boolean isImage(@NotNull String typeName) {
        return switch (typeName) {
            case "jpg", "jpeg", "png" -> true;
            default -> false;
        };
    }

}
