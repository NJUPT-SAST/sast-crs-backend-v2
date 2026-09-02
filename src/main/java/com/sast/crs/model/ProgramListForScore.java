package com.sast.crs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramListForScore {
    private Integer id;
    private String title;
    private Integer score;
    private String opinion;
}
