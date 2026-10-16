package com.sast.crs.util;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.sast.crs.entity.UserExtra;

import java.math.BigDecimal;

public class UserExtraConverter implements Converter<UserExtra> {
    @Override
    public UserExtra convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        // 联系方式允许不填：空单元格返回 null，extra 列按 NULL 落库
        if (cellData == null) {
            return null;
        }
        String contact;
        BigDecimal bigNumber = cellData.getNumberValue();
        if (bigNumber != null) {
            contact = bigNumber.toString();
            if (contact.contains("E+10")) {
                contact = contact.replace(".", "");
                var length = contact.length();
                if (length != 14) {
                    var loop = 14 - length;
                    StringBuilder sb = new StringBuilder(); // 创建一个 StringBuilder 对象
                    sb.append("0".repeat(Math.max(0, loop + 1))); // 在 StringBuilder 中追加字符 "0"
                    contact = contact.replace("E+10", sb.toString());
                } else {
                    contact = contact.replace("E+10", "0");
                }
            }
        } else {
            // 文本单元格同样接受：手机号被存成文本很常见，留空按不填处理
            String text = cellData.getStringValue();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            contact = text.trim();
        }
        UserExtra userExtra = new UserExtra();
        userExtra.setContact(contact);
        return userExtra;
    }
}
