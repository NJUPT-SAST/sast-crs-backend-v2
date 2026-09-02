package com.sast.crs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sast.crs.annotation.OperateLog;
import com.sast.crs.annotation.PassToken;
import com.sast.crs.entity.File;
import com.sast.crs.mapper.FileMapper;
import com.sast.crs.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
public class TestController {
    FileUtil fileUtil;
    FileMapper fileMapper;

    @Autowired
    public void setFileUtil(FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    @Autowired
    public void setFileMapper(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @OperateLog("测试")
    @GetMapping("/test")
    public String test() {
        return "测试";
    }

    @PassToken
    @GetMapping("/down")
    public void down(HttpServletResponse response) {
        List<File> files = fileMapper.selectList(new LambdaQueryWrapper<File>().eq(File::getUserCode, "B21012410").eq(File::getComId, 1));
        try {
            fileUtil.downloadPackFile(response, files, "测试.zip");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
