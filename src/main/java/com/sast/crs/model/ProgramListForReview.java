package com.sast.crs.model;

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
