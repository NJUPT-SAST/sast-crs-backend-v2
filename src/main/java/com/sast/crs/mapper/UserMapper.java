package com.sast.crs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sast.crs.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends BaseMapper<User> {
}
