package com.sast.crs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.sast.crs.entity.Competition;
import com.sast.crs.entity.Notice;
import com.sast.crs.entity.User;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.enums.UserRoleEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.interceptor.UserInterceptor;
import com.sast.crs.mapper.CompetitionMapper;
import com.sast.crs.mapper.NoticeMapper;
import com.sast.crs.service.NoticeService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;
    private final CompetitionMapper competitionMapper;

    public NoticeServiceImpl(NoticeMapper noticeMapper, CompetitionMapper competitionMapper) {
        this.noticeMapper = noticeMapper;
        this.competitionMapper = competitionMapper;
    }

    @Override
    public List<Map<String, Object>> getNotice(Long id) {
        User user = Optional.ofNullable(UserInterceptor.userHolder.get())
                .orElse(new User() {{
                    setRole(UserRoleEnum.COMMON_STUDENT.getRole());
                }});
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getComId, id));
        List<Map<String, Object>> results = new LinkedList<>();
        for (Notice notice : notices) {
            if (UserRoleEnum.ADMIN.getRole().equals(user.getRole())) // 管理员显示所有公告
                results.add(new HashMap<>() {{
                    put("id", notice.getId());
                    put("title", notice.getTitle());
                    put("time", notice.getTime()
                            .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")));
                    put("content", notice.getContent());
                    put("role", notice.getRole());
                }});
            else if ((UserRoleEnum.TOURIST.getRole().equals(notice.getRole())) || // 针对游客的公告是所有人都能看到的
                    (notice.getRole().equals(user.getRole()) &&
                            notice.getTime().isBefore(LocalDateTime.now()))) // 其他角色只能显示相应角色的公告
                results.add(new HashMap<>() {{
                    put("id", notice.getId());
                    put("title", notice.getTitle());
                    put("time", notice.getTime()
                            .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")));
                    put("content", notice.getContent());
                }});
        }
        return results;
    }

    @Override
    public void setNotice(@NotNull Notice notice) {
        if (notice.getTime() == null) {
            notice.setTime(LocalDateTime.now());
        }
        Competition competition = competitionMapper.selectById(notice.getComId());
        if (competition == null) {
            throw new LocalRuntimeException(ErrorEnum.UNKNOWN_COMPETITION_ID);
        }
        int result = noticeMapper.insert(notice);
        if (result <= 0){
            throw new LocalRuntimeException(ErrorEnum.NOTICE_ERROR);
        }
    }

    @Override
    public void editNotice(@NotNull Notice notice) {
        QueryWrapper<Notice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", notice.getId());
        Notice tempNotice = noticeMapper.selectOne(queryWrapper);
        if (tempNotice == null) {
            throw new LocalRuntimeException(ErrorEnum.NOTICE_NOT_EXIST);
        }
        if (!StringUtils.isEmpty(notice.getTitle())) tempNotice.setTitle(notice.getTitle());
        if (!StringUtils.isEmpty(notice.getContent())) tempNotice.setContent(notice.getContent());
        if (notice.getRole() != null) tempNotice.setRole(notice.getRole());
        if (notice.getTime() != null) tempNotice.setTime(notice.getTime());
        int result = noticeMapper.updateById(tempNotice);
        if (result <= 0){
            throw new LocalRuntimeException(ErrorEnum.NOTICE_ERROR);
        }
    }

    @Override
    public void delNotice(Long id) {
        QueryWrapper<Notice> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        Notice tempNotice = noticeMapper.selectOne(wrapper);
        if (tempNotice == null) {
            throw new LocalRuntimeException(ErrorEnum.NOTICE_NOT_EXIST);
        }
        int result = noticeMapper.deleteById(id);
        if (result <= 0){
            throw new LocalRuntimeException(ErrorEnum.NOTICE_ERROR);
        }
    }
}
