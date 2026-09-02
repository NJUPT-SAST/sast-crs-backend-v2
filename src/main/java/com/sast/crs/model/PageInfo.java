package com.sast.crs.model;

import lombok.Data;

import java.util.List;

@Data
public class PageInfo<T> {

    private Integer total;
    private List<T> list;
    private Integer pageNum;
    private Integer pageSize;
    private Integer pages;
    private Boolean isFirstPage;
    private Boolean isLastPage;

    //自动构造器
    public PageInfo(Integer total, List<T> list, Integer pageNum, Integer pageSize, Integer pages) {
        this.total = total;
        this.list = list;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pages;
        this.isFirstPage = pageNum.equals(1);
        this.isLastPage = pageNum.equals(pages);
    }
}
