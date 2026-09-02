package com.sast.crs.util;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.sast.crs.entity.UserExtra;
import com.sast.crs.exception.LocalRuntimeException;

import java.math.BigDecimal;

public class UserExtraConverter implements Converter<UserExtra> {
    @Override
    public UserExtra convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        BigDecimal bigNumber = cellData.getNumberValue();
        if (bigNumber == null) throw new LocalRuntimeException("联系方式格式不对");
        String phoneNumber = bigNumber.toString();
        if (phoneNumber.contains("E+10")) {
            phoneNumber = phoneNumber.replace(".", "");
            var length = phoneNumber.length();
            if (length != 14) {
                var loop = 14 -length;
                StringBuilder sb = new StringBuilder(); // 创建一个 StringBuilder 对象
                sb.append("0".repeat(Math.max(0, loop+1))); // 在 StringBuilder 中追加字符 "0"
                phoneNumber = phoneNumber.replace("E+10", sb.toString());
            }else {
            phoneNumber = phoneNumber.replace("E+10", "0");
        }}
        UserExtra userExtra = new UserExtra();
        userExtra.setContact(phoneNumber);
        return userExtra;
    }
}
