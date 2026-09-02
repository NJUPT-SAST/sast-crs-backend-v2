package com.sast.crs.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WhiteList {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long comId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> codeList;
}
