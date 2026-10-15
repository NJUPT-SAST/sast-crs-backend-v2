package com.sast.crs.model;

import com.sast.crs.enums.CompetitionTypeEnum;
import lombok.Data;

/**
 * 「我是队长的队伍」+ 比赛类型。
 *
 * <p>member / teacher 直接是 team 表里的 JSON 文本（text 列），不在 SQL 里展开：
 * 生产库是 MySQL 5.7，不支持 JSON_TABLE（8.0.4+）与 JSON_ARRAYAGG 组合的写法，
 * 队员 / 指导老师的展开改由 {@code UserServiceImpl#getTeamInfo} 在 Java 侧完成。
 */
@Data
public class TeamInfoWithCom {
    /**
     * 比赛类型（决定要不要返回队伍名称）
     */
    private CompetitionTypeEnum comType;

    /**
     * 队伍名称（仅团队赛）
     */
    private String teamName;

    /**
     * 队伍成员 JSON 文本，不含队长，可能为 null
     */
    private String member;

    /**
     * 指导老师 JSON 文本，可能为 null
     */
    private String teacher;
}
