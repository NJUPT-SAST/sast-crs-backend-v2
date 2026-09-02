package com.sast.crs.model;

import lombok.Data;

@Data
public class WorkSchema {
    private String input;
    private String content;
    private Boolean isFile;
}
