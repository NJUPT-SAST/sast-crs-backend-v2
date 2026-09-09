package com.sast.crs.service.impl;

import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.sast.crs.entity.Judge;
import com.sast.crs.mapper.JudgeMapper;
import com.sast.crs.service.JudgeService;
import org.springframework.stereotype.Service;

@Service
public class JudgeServiceImpl extends CrudRepository<JudgeMapper, Judge> implements JudgeService {
}
