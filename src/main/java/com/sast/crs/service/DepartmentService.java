package com.sast.crs.service;

import com.sast.crs.entity.Department;

import java.util.List;

public interface DepartmentService {
    /**
     * 获取全部学院，按学院编号升序
     *
     * @return 学院列表
     */
    List<Department> listAll();
}
