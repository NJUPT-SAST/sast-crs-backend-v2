package com.sast.crs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramListForReview {
    private Integer id;
    private String title;
    private Boolean isPass;
    private String opinion;
}
