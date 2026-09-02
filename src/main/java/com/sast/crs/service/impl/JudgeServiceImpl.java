package com.sast.crs.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.sast.crs.entity.Judge;
import com.sast.crs.mapper.JudgeMapper;
import com.sast.crs.service.JudgeService;
import org.springframework.stereotype.Service;

@Service
public class JudgeServiceImpl extends ServiceImpl<JudgeMapper, Judge> implements JudgeService {
}
