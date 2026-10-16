package com.sast.crs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sast.crs.entity.Department;
import com.sast.crs.mapper.DepartmentMapper;
import com.sast.crs.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public List<Department> listAll() {
        // department.id 是学院代号，实体里是 Integer；建表语句里曾写成 varchar，
        // 那样 ORDER BY id 会得到字典序（1,10,11,…,2,20,3），学院下拉顺序就乱了。
        // schema.sql 已改为 int，这里保持数据库排序即可。
        return departmentMapper.selectList(
                new LambdaQueryWrapper<Department>().orderByAsc(Department::getId));
    }
}
