package com.sast.crs.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CompetitionTypeEnum {
    SINGLE_COMPETITION(0, "单人赛"),
    TEAM_COMPETITION(1, "团队赛");

    @EnumValue
    private final Integer type;
    private final String typeName;
}
