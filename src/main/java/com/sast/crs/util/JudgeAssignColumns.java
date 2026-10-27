package com.sast.crs.util;

import com.sast.crs.constant.CommonConst;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分配评委表的列定位。
 *
 * <p>导出模板（{@link com.sast.crs.model.WorkOutput}）为「作品id / 作品名称 / 学院 / 项目类别 / 评委学号」，
 * 管理员可以往右侧继续复制出多个「评委学号」列（表头写「评委学号1」「评委学号2」也能识别）。
 *
 * <p>存量文件没有「评委学号」表头，此时回退到老的列号约定：跳过「项目类别」列，从它后面一列开始
 * （表头里连「项目类别」都没有时，才用最老的「第三格开始」）。
 */
public final class JudgeAssignColumns {

    /** 评委学号列的表头关键字 */
    public static final String HEADER_KEYWORD = "评委学号";

    /** 最老的兜底约定：第三格（列号 2）开始是评委学号 */
    private static final int LEGACY_START_INDEX = 2;

    private JudgeAssignColumns() {
    }

    /**
     * 收集表头里所有「评委学号」列，按列号升序返回。
     *
     * @param headMap 表头（列号 → 表头文字，文字可能为 null）
     * @return 评委学号列号；没有这样的表头时返回空列表
     */
    public static List<Integer> detect(Map<Integer, String> headMap) {
        List<Integer> columns = new ArrayList<>();
        if (headMap == null) {
            return columns;
        }
        for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
            String title = entry.getValue();
            if (entry.getKey() != null && title != null && title.contains(HEADER_KEYWORD)) {
                columns.add(entry.getKey());
            }
        }
        columns.sort(Integer::compareTo);
        return columns;
    }

    /**
     * 没有「评委学号」表头时的回退起点：表头里有「项目类别」就取它的下一列，否则用最老的第三格约定。
     *
     * @param headMap 表头（列号 → 表头文字，文字可能为 null）
     * @return 评委学号的起始列号（含）
     */
    public static int fallbackStart(Map<Integer, String> headMap) {
        if (headMap != null) {
            for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
                String title = entry.getValue();
                if (entry.getKey() != null && title != null && title.contains(CommonConst.WORK_TYPE)) {
                    return entry.getKey() + 1;
                }
            }
        }
        return LEGACY_START_INDEX;
    }
}
