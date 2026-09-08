package com.sast.crs.service.impl;

import cn.hutool.core.io.FileTypeUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.entity.*;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.enums.UserRoleEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.*;
import com.sast.crs.model.ComMangerVo;
import com.sast.crs.model.CompetitionVO;
import com.sast.crs.model.JudgeAccountRequest;
import com.sast.crs.model.JudgeAccountVO;
import com.sast.crs.model.WhiteList;
import com.sast.crs.service.AdminService;
import com.sast.crs.util.AccountImportUtil;
import com.sast.crs.util.COSUtil;
import com.sast.crs.util.SecureUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.sast.crs.enums.ErrorEnum.*;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {
    private static final int BATCH_COUNT = 10;

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
    private final FileMapper fileMapper;
    private final ReviewMapper reviewMapper;
    private final TeamMapper teamMapper;
    private final WorkMapper workMapper;
    private final DepartmentMapper departmentMapper;
    private final JudgeMapper judgeMapper;
    private final WhiteListMapper whiteListMapper;
    private final AccountImportUtil accountImportUtil;
    private final COSUtil cosUtil;

    public AdminServiceImpl(AdminMapper adminMapper, UserMapper userMapper, FileMapper fileMapper, ReviewMapper reviewMapper, TeamMapper teamMapper, WorkMapper workMapper, DepartmentMapper departmentMapper, JudgeMapper judgeMapper, WhiteListMapper whiteListMapper, AccountImportUtil accountImportUtil, COSUtil cosUtil) {
        this.adminMapper = adminMapper;
        this.userMapper = userMapper;
        this.fileMapper = fileMapper;
        this.reviewMapper = reviewMapper;
        this.teamMapper = teamMapper;
        this.workMapper = workMapper;
        this.departmentMapper = departmentMapper;
        this.judgeMapper = judgeMapper;
        this.whiteListMapper = whiteListMapper;
        this.accountImportUtil = accountImportUtil;
        this.cosUtil = cosUtil;
    }

    @Override
    public void createContest(Competition competition, MultipartFile cover) {
        // 比较时间设置是否正确
        if (competition.getRegBeginTime().isAfter(competition.getSubmitBeginTime()) || // 提交开始时间不早于报名开始时间
                competition.getSubmitBeginTime().isAfter(competition.getReviewBeginTime()) || // 评审开始时间不早于提交开始时间
                competition.getRegBeginTime().isAfter(competition.getRegEndTime()) || // 报名截止时间不早于报名开始时间
                competition.getRegEndTime().isAfter(competition.getSubmitEndTime()) || // 提交截止时间不早于报名截止时间
                competition.getSubmitEndTime().isAfter(competition.getReviewEndTime())) { // 评审截止时间不早于提交截止时间
            throw new LocalRuntimeException(ErrorEnum.DATE_ERROR);
        }

        // 校验审批关系数据是否正确
        Map<String, String> settings = competition.getReviewSettings();
        validateReviewSettings(settings);

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
        if (competition.getRegBeginTime().isAfter(competition.getSubmitBeginTime()) || // 提交开始时间不早于报名开始时间
                competition.getSubmitBeginTime().isAfter(competition.getReviewBeginTime()) || // 评审开始时间不早于提交开始时间
                competition.getRegBeginTime().isAfter(competition.getRegEndTime()) || // 报名截止时间不早于报名开始时间
                competition.getRegEndTime().isAfter(competition.getSubmitEndTime()) || // 提交截止时间不早于报名截止时间
                competition.getSubmitEndTime().isAfter(competition.getReviewEndTime())) { // 评审截止时间不早于提交截止时间
            throw new LocalRuntimeException(ErrorEnum.DATE_ERROR);
        }
        QueryWrapper<Competition> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", competition.getId());
        Competition temCompetition = adminMapper.selectOne(queryWrapper);
        if (temCompetition == null) {
            throw new LocalRuntimeException(CONTEST_NOT_EXIST);
        }

        // 校验审批关系数据是否正确
        Map<String, String> settings = competition.getReviewSettings();
        validateReviewSettings(settings);

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
        Page<CompetitionVO> page = new Page<>(pageNum, pageSize);
        IPage<CompetitionVO> contestListPage = adminMapper.getContestListPage(page);
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
        cosUtil.downloadPackFile(response, files, fileName);
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
        if (cover.getSize() > 5242880) {
            throw new LocalRuntimeException("图片大小超出限制(5MB)");
        }
        return cosUtil.uploadCover(cover, comId);
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

    private void validateReviewSettings(Map<String, String> settings) {
        if (settings == null || settings.isEmpty()) {
            throw new LocalRuntimeException(REVIEW_SETTINGS_ERROR);
        }

        // 1) 收集所有审核人账号
        Set<String> reviewerCodes = settings.values().stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).collect(Collectors.toSet());

        if (reviewerCodes.isEmpty()) {
            throw new LocalRuntimeException(USER_NOT_EXIST);
        }

        // 2) 收集所有非0部门ID
        Set<Integer> depIds = new HashSet<>();
        for (String key : settings.keySet()) {
            if (!"0".equals(key)) {
                try {
                    depIds.add(Integer.valueOf(key));
                } catch (NumberFormatException e) {
                    throw new LocalRuntimeException(ErrorEnum.DEP_NOT_EXIST);
                }
            }
        }

        // 3) 批量查用户
        List<User> users = userMapper.selectList(
                new QueryWrapper<User>().select("code").in("code", reviewerCodes));
        Set<String> existingCodes = users.stream().map(User::getCode).collect(Collectors.toSet());

        if (existingCodes.size() != reviewerCodes.size()) {
            throw new LocalRuntimeException(ErrorEnum.USER_NOT_EXIST);
        }

        // 4) 批量查部门（仅非0）
        if (!depIds.isEmpty()) {
            List<Department> deps = departmentMapper.selectList(
                    new QueryWrapper<Department>().select("id").in("id", depIds));
            Set<Integer> existingDepIds = deps.stream().map(Department::getId).collect(Collectors.toSet());

            if (existingDepIds.size() != depIds.size()) {
                throw new LocalRuntimeException(ErrorEnum.DEP_NOT_EXIST);
            }
        }
    }

    @Override
    public Map<String, Object> getJudgeAccountList(Integer pageNum, Integer pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("role", UserRoleEnum.JUDGE.getRole());
        IPage<User> judgePage = userMapper.selectPage(page, wrapper);
        List<JudgeAccountVO> records = judgePage.getRecords().stream().map(user -> new JudgeAccountVO(user.getCode(), user.getName(), user.getExtra() == null ? null : user.getExtra().getContact())).collect(Collectors.toList());
        return getResultMap(records, judgePage.getTotal(), pageNum, pageSize);
    }

    @Override
    public void createJudgeAccount(JudgeAccountRequest request) {
        String code = requireText(request.getCode(), "学号不能为空");
        if (userIsExist(code)) {
            throw new LocalRuntimeException("该学号已存在，不可重复创建");
        }
        String name = requireText(request.getName(), "姓名不能为空");
        String contact = requireText(request.getContact(), "联系方式不能为空");
        String password = request.getPassword();
        if (password == null || password.length() < 6) {
            throw new LocalRuntimeException("密码至少 6 位");
        }
        User user = new User();
        user.setCode(code);
        user.setName(name);
        user.setPassword(SecureUtil.encryptMD5(password));
        // 评委账号不挂靠具体学院，沿用旧版导入学生时固定的默认部门
        user.setDepId(1);
        user.setRole(UserRoleEnum.JUDGE.getRole());
        UserExtra extra = new UserExtra();
        extra.setContact(contact);
        user.setExtra(extra);
        userMapper.insert(user);
    }

    @Override
    public void editJudgeAccount(JudgeAccountRequest request) {
        String code = requireText(request.getCode(), "学号不能为空");
        User user = userMapper.selectById(code);
        if (user == null) {
            throw new LocalRuntimeException(USER_NOT_EXIST);
        }
        if (!UserRoleEnum.JUDGE.getRole().equals(user.getRole())) {
            throw new LocalRuntimeException("该学号不是评委账号");
        }
        String name = requireText(request.getName(), "姓名不能为空");
        String contact = requireText(request.getContact(), "联系方式不能为空");
        User update = new User();
        update.setCode(code);
        update.setName(name);
        UserExtra extra = user.getExtra();
        if (extra == null) {
            extra = new UserExtra();
        }
        extra.setContact(contact);
        update.setExtra(extra);
        // 密码留空表示不重置
        String password = request.getPassword();
        if (password != null && !password.isEmpty()) {
            if (password.length() < 6) {
                throw new LocalRuntimeException("密码至少 6 位");
            }
            update.setPassword(SecureUtil.encryptMD5(password));
        }
        userMapper.updateById(update);
    }

    @Override
    public void deleteJudgeAccount(String code) {
        String trimmedCode = requireText(code, "学号不能为空");
        User user = userMapper.selectById(trimmedCode);
        if (user == null) {
            throw new LocalRuntimeException(USER_NOT_EXIST);
        }
        if (!UserRoleEnum.JUDGE.getRole().equals(user.getRole())) {
            throw new LocalRuntimeException("该学号不是评委账号");
        }
        userMapper.deleteById(trimmedCode);
        // 同时清掉该评委的分配记录，避免之后重建同号账号时残留旧的分配关系
        judgeMapper.delete(new QueryWrapper<Judge>().eq("judge_code", trimmedCode));
    }

    @Override
    public List<Map<String, String>> importJudgeAccount(MultipartFile file, Integer depId) {
        return accountImportUtil.importAccounts(file, depId, UserRoleEnum.JUDGE.getRole(), "评委");
    }

    @Override
    public void importJudgeAssign(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new LocalRuntimeException("文件为空");
        }
        List<Judge> cachedDataList = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
                    String idText = data.get(0);
                    if (idText == null || idText.trim().isEmpty()) {
                        // 跳过空行
                        return;
                    }
                    // 作品id
                    Long id = Long.valueOf(idText.trim());
                    Work work = workMapper.selectById(id);
                    // 判断作品id是否存在
                    if (work == null) {
                        throw new LocalRuntimeException(WORK_NOT_EXIST);
                    }
                    // 活动id
                    Long comId = work.getComId();
                    String userCode = work.getUserCode();
                    // 判断比赛是否需要审核和作品是否审核，如果不需要则跳过这条数据
                    Competition competition = adminMapper.selectOne(new QueryWrapper<Competition>().eq("id", comId).select("is_review"));
                    Review review = reviewMapper.selectOne(new QueryWrapper<Review>().eq("com_id", comId).eq("user_code", userCode));
                    if (competition.getIsReview() && (review == null || review.getAccept() == null || !review.getAccept())) {
                        log.warn("作品id:{}未通过审核，跳过分配评委", id);
                        return;
                    }
                    // 判断这个比赛是否已经分配评委，已经分配的话此时需要覆盖掉这些数据
                    judgeMapper.delete(new QueryWrapper<Judge>().eq("user_code", userCode).eq("com_id", comId));

                    Set<String> codes = new HashSet<>();
                    // excel表从第三格开始为评委学号
                    for (int i = 2; i < data.size(); i++) {
                        String judgeCode = data.get(i);
                        if (judgeCode == null || judgeCode.trim().isEmpty()) {
                            continue;
                        }
                        codes.add(judgeCode.trim());
                    }
                    for (String judgeCode : codes) {
                        // 判断评委学号是否存在
                        if (!userIsExist(judgeCode)) {
                            log.error("学号{}不存在", judgeCode);
                            throw new LocalRuntimeException(USER_NOT_EXIST);
                        }
                        Judge judge = new Judge();
                        judge.setJudgeCode(judgeCode);
                        judge.setComId(comId);
                        judge.setUserCode(userCode);
                        cachedDataList.add(judge);
                    }
                    // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
                    if (cachedDataList.size() >= BATCH_COUNT) {
                        flushJudges(cachedDataList);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    flushJudges(cachedDataList);
                    log.info("所有数据解析完成！");
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new LocalRuntimeException(IMPORT_ERROR);
        }
    }

    @Override
    public void setWhiteList(Long comId, Boolean isWhiteList, MultipartFile file) {
        // 覆盖式导入：先删除已设置的白名单
        whiteListMapper.delete(new QueryWrapper<WhiteList>().eq("com_id", comId));
        if (!isWhiteList) {
            updateComWhiteList(comId, false);
            return;
        }
        if (file == null || file.isEmpty()) {
            throw new LocalRuntimeException("文件为空");
        }
        List<String> cachedDataList = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
                    String code = data.get(0);
                    if (code == null || code.trim().isEmpty()) {
                        // 跳过空行
                        return;
                    }
                    cachedDataList.add(code.trim());
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    if (cachedDataList.isEmpty()) {
                        return;
                    }
                    log.info("{}条数据，开始存储数据库", cachedDataList.size());
                    whiteListMapper.insert(new WhiteList(null, comId, cachedDataList));
                    log.info("存储成功！");
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (IOException e) {
            throw new LocalRuntimeException(IMPORT_ERROR);
        }
        updateComWhiteList(comId, true);
    }

    /**
     * 批量写入评委分配记录
     *
     * @param cachedDataList 待写入的分配记录
     */
    private void flushJudges(List<Judge> cachedDataList) {
        for (Judge judge : cachedDataList) {
            judgeMapper.insert(judge);
        }
        log.info("存储数据库成功！");
        cachedDataList.clear();
    }

    /**
     * 更新比赛的isWhiteList字段
     *
     * @param comId       比赛id
     * @param isWhiteList 是否设置名单
     */
    private void updateComWhiteList(Long comId, Boolean isWhiteList) {
        QueryWrapper<Competition> competitionQueryWrapper = new QueryWrapper<>();
        competitionQueryWrapper.eq("id", comId);
        Competition competition = adminMapper.selectOne(competitionQueryWrapper);
        competition.setIsWhiteList(isWhiteList);
        adminMapper.updateById(competition);
    }

    /**
     * 校验文本字段非空并去除首尾空白
     *
     * @param value   待校验文本
     * @param message 校验失败提示
     * @return 去除首尾空白后的文本
     */
    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new LocalRuntimeException(message);
        }
        return value.trim();
    }

}
