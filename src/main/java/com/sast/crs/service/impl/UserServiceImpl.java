package com.sast.crs.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sast.crs.constant.RedisKeyConst;
import com.sast.crs.entity.*;
import com.sast.crs.enums.CompetitionTypeEnum;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.*;
import com.sast.crs.model.FileCache;
import com.sast.crs.model.TeamInfoWithCom;
import com.sast.crs.model.WorkSchema;
import com.sast.crs.pojo.UserResponse;
import com.sast.crs.service.UserService;
import com.sast.crs.util.CommonUtil;
import com.sast.crs.util.FileUtil;
import com.sast.crs.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private FileUtil fileUtil;
    private RedisUtil redisUtil;
    private WorkMapper workMapper;
    private TeamMapper teamMapper;
    private FileMapper fileMapper;
    private ReviewMapper reviewMapper;
    private DepartmentMapper departmentMapper;
    private CompetitionMapper competitionMapper;

    @Value("${app.defaultCover}")
    private String defaultCover;

    @Autowired
    public void setFileUtil(FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    @Autowired
    public void setRedisUtil(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Autowired
    public void setWorkMapper(WorkMapper workMapper) {
        this.workMapper = workMapper;
    }

    @Autowired
    public void setTeamMapper(TeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    @Autowired
    public void setFileMapper(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @Autowired
    public void setReviewMapper(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    @Autowired
    public void setDepartmentMapper(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Autowired
    public void setCompetitionMapper(CompetitionMapper competitionMapper) {
        this.competitionMapper = competitionMapper;
    }

    @Override
    public Map<String, Object> getAllComList(Integer cur, Integer limit) {
        List<Competition> competitions = competitionMapper.selectList(null);
        return resultComListMap(competitions, cur, limit);
    }

    @Override
    public Map<String, Object> getSignedComList(@NotNull String userCode, Integer cur, Integer limit) {
        // 这里仅查询队长，之后需要能查询到队员
        Page<Competition> page = new Page<>(cur, limit);
        IPage<Competition> pageRes = competitionMapper.selectSignedCompetitions(page, userCode);
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> records = new LinkedList<>();
        for (Competition competition : pageRes.getRecords()) {
            String url = competition.getCover();
            Map<String, Object> record = new HashMap<>();
            record.put("id", competition.getId());
            record.put("name", competition.getName());
            record.put("cover", StringUtils.isEmpty(url) ? defaultCover : url);
            record.put("intro", CommonUtil.getSpecifiedString(competition.getIntroduce(), 30));
            record.put("date", formatDateTime(competition.getRegBeginTime()));
            record.put("status", getCompetitionStatus(competition));
            records.add(record);
        }
        result.put("records", records);
        result.put("total", pageRes.getTotal());
        result.put("pageNum", pageRes.getCurrent());
        result.put("pageSize", pageRes.getSize());
        return result;
    }

    @Override
    public Map<String, Object> getComInfo(Long comId) {
        Competition competition = getCompetition(comId);
        String url = competition.getCover();
        Map<String, Object> result = new HashMap<>();
        result.put("name", competition.getName());
        result.put("cover", StringUtils.isEmpty(url) ? defaultCover : url);
        result.put("introduce", competition.getIntroduce());
        result.put("status", getCompetitionStatus(competition));
        result.put("regBegin", formatDateTime(competition.getRegBeginTime()));
        result.put("regEnd", formatDateTime(competition.getRegEndTime()));
        result.put("submitBegin", formatDateTime(competition.getSubmitBeginTime()));
        result.put("submitEnd", formatDateTime(competition.getSubmitEndTime()));
        result.put("reviewBegin", formatDateTime(competition.getReviewBeginTime()));
        result.put("reviewEnd", formatDateTime(competition.getReviewEndTime()));
        return result;
    }

    @Override
    public Map<String, Object> getComSignUpInfo(Long comId) {
        Map<String, Object> result = new HashMap<>();
        Competition competition = getCompetition(comId);
        if (competition.isTeamCom()) {
            result.put("isTeam", true);
            result.put("minTeamMembers", competition.getMinTeamMembers());
            result.put("maxTeamMembers", competition.getMaxTeamMembers());
        } else {
            result.put("isTeam", false);
        }
        return result;
    }

    @Override
    public Map<String, Object> searchComName(String key, Integer cur, Integer limit) {
        List<Competition> competitions = competitionMapper
                .selectList(new LambdaQueryWrapper<Competition>().like(Competition::getName, key));
        return resultComListMap(competitions, cur, limit);
    }

    @Override
    public UserResponse getUserProfile(@NotNull User user) {
        UserResponse response = new UserResponse();
        response.setCode(user.getCode());
        response.setName(user.getName());
        response.setMajor(user.getExtra() == null ? null : user.getExtra().getMajor());
        response.setContact(user.getExtra() == null ? null : user.getExtra().getContact());

        Department department = departmentMapper.selectById(user.getDepId());
        if (department != null)
            response.setCollege(department.getName());

        return response;
    }

    @Override
    public Map<String, String> getUploadCertificate(User user, Long comId, String input, String filename) {
        Competition competition = getCompetition(comId);
        Team team = getSignedTeam(user.getCode(), competition.getId());

        // 检查redis缓存
        String key = RedisKeyConst.getWorkFileCacheKey(user.getCode(), input);
        if (redisUtil.hasKey(key)) {
            FileCache cache = JSON.parseObject((String) redisUtil.get(key), FileCache.class);
            fileUtil.deleteFileCOS(cache.getUrl(), FileUtil.PRIVATE_FOLDER);
            redisUtil.del(key);
        }
        Map<String, String> urlMap = fileUtil.getUploadCertificate(filename, comId, team.getId(), input);
        FileCache uploadFile = new FileCache();
        uploadFile.setComId(comId);
        uploadFile.setUserCode(user.getCode());
        uploadFile.setInput(input);
        uploadFile.setUrl(urlMap.get("clearUrl"));
        uploadFile.setDate(LocalDateTime.now());
        redisUtil.set(key, JSON.toJSONString(uploadFile));
        return urlMap;
    }

    @Override
    public Map<String, Object> getTeamInfo(@NotNull User user, Long comId) {
        Map<String, Object> data = new HashMap<>();
        TeamInfoWithCom teamInfoWithCom = teamMapper.selectTeamInfoWithCom(comId, user.getCode());
        if (teamInfoWithCom == null) {
            throw new LocalRuntimeException(ErrorEnum.HAVE_NOT_SIGNED_COM);
        }
        if (Objects.equals(teamInfoWithCom.getComType(), CompetitionTypeEnum.TEAM_COMPETITION)) {
            data.put("teamName", teamInfoWithCom.getTeamName());
        }
        // 团队成员
        List<UserResponse> members = teamInfoWithCom.getTeamMembersJson().toJavaList(UserResponse.class);
        data.put("teamMember", members);
        // 指导老师
        List<User> teachers = teamInfoWithCom.getTeacherJson().toJavaList(User.class);
        data.put("teacherMember", teachers);
        return data;
    }

    @Override
    public void uploadComSchema(User user, Long comId, String jsonData) {
        Competition competition = getCompetition(comId);
        if (competition.getSubmitEndTime().isBefore(LocalDateTime.now())) {
            throw new LocalRuntimeException("作品提交已结束");
        }
        if (competition.getSubmitBeginTime().isAfter(LocalDateTime.now())) {
            log.info("SubmitBeginTime: {} Server Time: {}", competition.getSubmitBeginTime(), LocalDateTime.now());
            throw new LocalRuntimeException("作品提交暂未开始");
        }

        JSONArray data = JSONObject.parseObject(jsonData)
                .getJSONArray("data");
        List<WorkSchema> workSchemas = new LinkedList<>();
        String workName = null;
        Set<String> fileInputs = new LinkedHashSet<>();
        for (Object inputObj : data) {
            JSONObject input = (JSONObject) inputObj;
            String title = input.getString("input");
            String content = input.getString("content");

            // 获取作品名称，这里很不优雅 todo
            if (title.equals("作品名称") || title.equals("作品名") || title.equals("项目名称"))
                workName = content;

            if (fileUtil.isBucketURL(content)) {
                fileInputs.add(title);
            }
        }

        Map<String, File> fileDBMap = loadWorkFiles(competition.getId(), user.getCode(), fileInputs);

        for (Object inputObj : data) {
            JSONObject input = (JSONObject) inputObj;
            String title = input.getString("input");
            String content = input.getString("content");

            WorkSchema workSchema = new WorkSchema();
            workSchema.setInput(title);
            workSchema.setContent(content);
            workSchema.setIsFile(false);
            // 单独处理文件
            if (fileUtil.isBucketURL(content)) {
                String key = RedisKeyConst.getWorkFileCacheKey(user.getCode(), title);
                File fileDB = fileDBMap.get(title);
                if (fileDB == null) {
                    if (!redisUtil.hasKey(key))
                        throw new LocalRuntimeException(ErrorEnum.FILE_EXPIRED_ERROR);
                    FileCache cache = JSON.parseObject((String) redisUtil.get(key), FileCache.class);
                    File newFile = cache.toFile();
                    fileMapper.insert(newFile);
                    fileDBMap.put(title, newFile);
                } else if (!fileDB.getUrl().equalsIgnoreCase(content)) {
                    if (!redisUtil.hasKey(key))
                        throw new LocalRuntimeException(ErrorEnum.FILE_EXPIRED_ERROR);
                    fileUtil.deleteFile(fileDB.getUrl(), FileUtil.PRIVATE_BUCKET);
                    FileCache cache = JSON.parseObject((String) redisUtil.get(key), FileCache.class);
                    fileDB.setUrl(cache.getUrl());
                    fileMapper.updateById(fileDB);
                    fileDBMap.put(title, fileDB);
                }
                redisUtil.del(key);
                workSchema.setIsFile(true);
            }
            workSchemas.add(workSchema);
        }
        Work workDB = workMapper.selectOne(new LambdaQueryWrapper<Work>()
                .eq(Work::getComId, competition.getId())
                .eq(Work::getUserCode, user.getCode()));
        if (workDB != null) {
            workDB.setWorkName(workName);
            workDB.setSchemaContent(JSON.toJSONString(workSchemas));
            workMapper.updateById(workDB);

            // 修改作品信息后重置审核状态
            Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                    .eq(Review::getComId, competition.getId())
                    .eq(Review::getUserCode, user.getCode()));
            if (review == null) {
                review = new Review();
                review.setComId(competition.getId());
                review.setUserCode(user.getCode());
                reviewMapper.insert(review);
            } else {
                reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                        .eq(Review::getComId, competition.getId())
                        .eq(Review::getUserCode, user.getCode())
                        .set(Review::getAccept, null)
                        .set(Review::getOpinion, null));
            }
        } else {
            Work work = new Work();
            work.setComId(competition.getId());
            work.setUserCode(user.getCode());
            work.setWorkName(workName);
            work.setSchemaContent(JSON.toJSONString(workSchemas));
            workMapper.insert(work);

            // 创建审核关系
            Review review = new Review();
            review.setComId(competition.getId());
            review.setUserCode(user.getCode());
            reviewMapper.insert(review);
        }
    }

    @Override
    public JSONArray getComSchema(@NotNull User user, Long comId) {
        Competition competition = getCompetition(comId);
        Work work = workMapper.selectOne(new LambdaQueryWrapper<Work>()
                .eq(Work::getComId, competition.getId())
                .eq(Work::getUserCode, user.getCode()));
        if (work == null)
            throw new LocalRuntimeException(ErrorEnum.HAVE_NOT_UPLOAD_WORK);
        return JSONArray.parseArray(work.getSchemaContent());
    }

    @Override
    public JSONObject getComSchemaTemplate(Long comId) {
        Competition competition = getCompetition(comId);
        String table = competitionMapper.getTableString(competition.getId());
        return JSONObject.parseObject(table);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signUpCom(@NotNull User user, String jsonData) {
        // 考虑队员在很多个团队的情况
        // todo
        JSONObject jsonObject = JSON.parseObject(jsonData);
        Long comId = Long.valueOf(jsonObject.getInteger("comId"));
        Competition competition = getCompetition(comId);
        LocalDateTime now = LocalDateTime.now();
        log.info("Server Time: {}", now);
        if (competition.getRegEndTime().isBefore(now)) {
            throw new LocalRuntimeException("报名已结束");
        }
        if (competition.getRegBeginTime().isAfter(now)) {
            throw new LocalRuntimeException("报名暂未开始");
        }
        Team team = teamMapper.selectOne(new LambdaQueryWrapper<Team>()
                .eq(Team::getComId, comId)
                .eq(Team::getCaptain, user.getCode()));

        List<User> teamListMembers = new LinkedList<>();
        Set<String> codeSet = new HashSet<>();
        JSONArray teamJSONMembers = jsonObject.getJSONArray("teamMember");
        Optional.ofNullable(teamJSONMembers)
                .ifPresent(array -> array.forEach(teamMember -> {
                    String name = ((JSONObject) teamMember).getString("name");
                    String code = ((JSONObject) teamMember).getString("code");
                    //tring college = ((JSONObject) teamMember).getString("college");
                    //String major = ((JSONObject) teamMember).getString("major");
                    String contact = ((JSONObject) teamMember).getString("contact");

                    if (competition.isTeamCom() &&
                            (StringUtils.isEmpty(name) || StringUtils.isEmpty(code))) {
                        throw new LocalRuntimeException("队伍成员信息不能留空");
                    }
                    // 检查学号姓名是否匹配
//                    User member = userMapper.selectOne(new LambdaQueryWrapper<User>()
//                            .eq(User::getCode, code));
//                    if (member != null && !member.getName().equals(name)) {
//                        throw new LocalRuntimeException("成员"+code+"与姓名不匹配，请检查报名信息");
//                    }
                    // 获取depID
//                    Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>()
//                            .eq(Department::getName, college));
//                    if (department == null) {
//                        throw new LocalRuntimeException("学院不存在，请检查报名信息");
//                    }

                    UserExtra extra = new UserExtra() {{
                        //setMajor(major);
                        setContact(contact);
                    }};

                    if (!user.getCode().equals(code)) {
                        teamListMembers.add(new User() {{
                            setName(name);
                            setCode(code);
                            //setDepId(Integer.MAX_VALUE);
                            setExtra(extra);
                        }});
                        codeSet.add(code);
                    }
                }));
        if (codeSet.size() != teamListMembers.size()) {
            throw new LocalRuntimeException("队伍成员不能重复");
        }

        boolean isUpdate = team != null;
        if (!isUpdate)
            team = new Team();
        if (competition.isTeamCom()) {
            String teamName = jsonObject.getString("teamName");
            if (StringUtils.isEmpty(teamName) && !isUpdate) {
                throw new LocalRuntimeException("队伍名称不能为空");
            }
            if (StringUtils.isNotEmpty(teamName))
                team.setName(teamName);

            if (teamListMembers.size() + 1 < competition.getMinTeamMembers() ||
                    teamListMembers.size() + 1 > competition.getMaxTeamMembers()) {
                throw new LocalRuntimeException("队伍成员数不满足要求");
            }
        } else if (!teamListMembers.isEmpty()) {
            throw new LocalRuntimeException("该比赛为单人赛，只能个人参赛");
        }

        // 指导老师
        List<User> teacherListMembers = new LinkedList<>();
        codeSet.clear();
        JSONArray teacherJSONMembers = jsonObject.getJSONArray("teacherMember");
        Optional.ofNullable(teacherJSONMembers)
                .ifPresent(array -> array.forEach(teacherMember -> {
                    String name = ((JSONObject) teacherMember).getString("name");
                    String code = ((JSONObject) teacherMember).getString("code");

                    teacherListMembers.add(new User() {{
                        setName(name);
                        setCode(code);
                    }});
                    codeSet.add(code);
                }));
        if (codeSet.size() != teacherListMembers.size()) {
            throw new LocalRuntimeException("指导老师不能重复");
        }

        team.setComId(comId);
        team.setCaptain(user.getCode());
        team.setMember(JSON.toJSONString(teamListMembers));
        team.setTeacher(JSON.toJSONString(teacherListMembers));
        if (isUpdate) teamMapper.updateById(team);
        else teamMapper.insert(team);
    }

    private int getCompetitionStatus(@NotNull Competition competition) {
        if (competition.getRegBeginTime().isAfter(LocalDateTime.now())) {
            // 比赛未开始
            return 0;
        } else if (competition.getRegBeginTime().isBefore(LocalDateTime.now()) &&
                competition.getRegEndTime().isAfter(LocalDateTime.now())) {
            // 比赛正在进行
            return 1;
        } else {
            // 比赛已结束
            return 2;
        }
    }

    @NotNull
    private String formatDateTime(@NotNull LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
    }

    @NotNull
    private Map<String, File> loadWorkFiles(Long comId, String userCode, Collection<String> inputs) {
        Map<String, File> fileMap = new HashMap<>();
        if (inputs == null || inputs.isEmpty()) {
            return fileMap;
        }

        List<File> files = fileMapper.selectList(new LambdaQueryWrapper<File>()
                .eq(File::getComId, comId)
                .eq(File::getUserCode, userCode)
                .in(File::getInput, inputs));
        for (File file : files) {
            File previous = fileMap.putIfAbsent(file.getInput(), file);
            if (previous != null) {
                throw new LocalRuntimeException("文件记录重复，请联系管理员");
            }
        }
        return fileMap;
    }

    @NotNull
    private Map<String, Object> resultComListMap(List<Competition> competitions,
                                                 Integer cur,
                                                 Integer limit) {
        Collections.reverse(competitions);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> records = new LinkedList<>();
        competitions.stream().skip((long) (cur - 1) * limit).limit(limit).forEach(competition -> {
            String url = competition.getCover();
            Map<String, Object> record = new HashMap<>();
            record.put("id", competition.getId());
            record.put("name", competition.getName());
            record.put("cover", StringUtils.isEmpty(url) ? defaultCover : url);
            record.put("intro", CommonUtil.getSpecifiedString(competition.getIntroduce(), 30));
            record.put("date", formatDateTime(competition.getRegBeginTime()));
            record.put("status", getCompetitionStatus(competition));
            records.add(record);
        });
        result.put("records", records);
        result.put("total", competitions.size());
        result.put("pageNum", cur);
        result.put("pageSize", limit);
        return result;
    }

    @NotNull
    private Competition getCompetition(Long comId) {
        Competition competition = competitionMapper.selectById(comId);
        if (competition == null) {
            throw new LocalRuntimeException(ErrorEnum.UNKNOWN_COMPETITION_ID);
        }
        return competition;
    }

    @NotNull
    private Team getSignedTeam(String userCode, Long comId) {
        Team team = teamMapper.selectOne(new LambdaQueryWrapper<Team>()
                .eq(Team::getComId, comId)
                .eq(Team::getCaptain, userCode));
        if (team == null) {
            throw new LocalRuntimeException(ErrorEnum.HAVE_NOT_SIGNED_COM);
        }
        return team;
    }
}
