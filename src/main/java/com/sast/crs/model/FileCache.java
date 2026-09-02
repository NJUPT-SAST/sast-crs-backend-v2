package com.sast.crs.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sast.crs.entity.File;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileCache {
    /**
     * 比赛ID
     */
    private Long comId;

    /**
     * 队长学号
     */
    private String userCode;

    /**
     * 输入框名
     */
    private String input;

    /**
     * 作品地址
     */
    private String url;

    /**
     * 提交时间
     */
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    public File toFile() {
        File file = new File();
        file.setComId(comId);
        file.setUserCode(userCode);
        file.setInput(input);
        file.setUrl(url);
        return file;
    }
}
