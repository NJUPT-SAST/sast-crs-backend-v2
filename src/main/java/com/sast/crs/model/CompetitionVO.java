package com.sast.crs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionVO {

    private Long id;

    private String name;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private String introduce;

    private String reviewer;

    private String status;

    private Long regNum;

    private Long subNum;

    private Long revNum;
}
