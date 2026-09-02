package com.sast.crs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComListForScore {
    private Integer id;
    private String title;
    private Integer totalNum;
    private Integer completedNum;
    private String startDate;
    private String endDate;
    @JsonIgnore
    private String code;
}
