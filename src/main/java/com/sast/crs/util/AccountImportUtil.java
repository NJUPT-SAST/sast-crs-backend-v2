package com.sast.crs.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sast.crs.entity.User;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;
import com.sast.crs.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * 账号批量导入：解析excel（学号/姓名/联系方式）并创建账号，学生与评委共用。
 * 使用无状态的 ReadListener，监听器随请求创建，不保留任何单例状态。
 */
@Component
public class AccountImportUtil {

    @Resource
    UserMapper userMapper;

    /**
     * 导入账号并生成随机初始密码
     *
     * @param file     excel文件（学号/姓名/联系方式）
     * @param depId    账号所属部门
     * @param role     账号角色
     * @param roleName 角色名，用于错误提示
     * @return 账号及初始密码列表
     */
    public List<Map<String, String>> importAccounts(MultipartFile file, Integer depId, Integer role, String roleName) {
        if (file == null || file.isEmpty()) {
            throw new LocalRuntimeException("文件为空");
        }
        Map<String, String> userPasswordMap = new LinkedHashMap<>();
        List<User> userList = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), User.class, new ReadListener<User>() {
                @Override
                public void invoke(User user, AnalysisContext analysisContext) {
                    if (user.getCode() == null || user.getCode().trim().isEmpty()) {
                        // 跳过空行
                        return;
                    }
                    userList.add(user);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    Set<String> codes = new HashSet<>();
                    for (User user : userList) {
                        String code = user.getCode().trim();
                        if (!codes.add(code)) {
                            throw new LocalRuntimeException("表格中学号" + code + "重复");
                        }
                        if (userIsExist(code)) {
                            throw new LocalRuntimeException("学号为" + code + "的" + roleName + "已存在，不可重复导入");
                        }
                        if (user.getName() == null || user.getName().trim().isEmpty()) {
                            throw new LocalRuntimeException("学号为" + code + "的" + roleName + "姓名不能为空");
                        }
                        user.setCode(code);
                        user.setName(user.getName().trim());
                        user.setDepId(depId);
                        user.setRole(role);
                        String originPass = code + CommonUtil.genetateRandomString(6, "abcdefghjkmnpqstwxyz");
                        userPasswordMap.put(code, originPass);
                        user.setPassword(SecureUtil.encryptMD5(originPass));
                        userMapper.insert(user);
                    }
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new LocalRuntimeException(ErrorEnum.IMPORT_ERROR);
        }
        List<Map<String, String>> list = new ArrayList<>();
        userPasswordMap.forEach((code, password) -> {
            Map<String, String> map = new HashMap<>();
            map.put("code", code);
            map.put("password", password);
            list.add(map);
        });
        return list;
    }

    /**
     * 判断是否存在这个用户
     *
     * @param userCode 用户学号
     * @return 判断结果
     */
    private boolean userIsExist(String userCode) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("code", userCode);
        return userMapper.exists(wrapper);
    }
}
