package com.sast.crs.util;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class CommonUtil {
    /**
     * 获取UA
     *
     * @param request 请求
     * @return java.lang.String
     */
    public static String getUserAgent(HttpServletRequest request) {
        UserAgent ua = UserAgentUtil.parse(request.getHeader("User-Agent"));
        if (ua == null)
            return "浏览器:null";
        return "浏览器:" + ua.getBrowser() + " " + ua.getVersion() + "，os:" + ua.getOs() + " " + ua.getOsVersion() + "，是否移动设备:" + ua.isMobile();
    }

    /**
     * aop获取请求参数
     *
     * @param joinPoint  切点
     * @param excludeSet 排查参数set
     * @return java.util.Map<java.lang.String, java.lang.Object>
     */
    public static Map<String, Object> getRequestParamMap(JoinPoint joinPoint, Set<String> excludeSet) {
        Map<String, Object> param = new HashMap<>();
        Object[] paramValues = joinPoint.getArgs();
        String[] paramNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
        for (int i = 0; i < paramNames.length; i++) {
            if (excludeSet != null && excludeSet.contains(paramNames[i])) {
                continue;
            }
            param.put(paramNames[i], paramValues[i]);
        }
        return param;
    }

    /**
     * 根据文件流的头部信息获得文件类型
     *
     * <pre>
     * 1、无法识别类型默认按照扩展名识别
     * 2、xls、doc、msi头信息无法区分，按照扩展名区分
     * 3、zip可能为docx、xlsx、pptx、jar、war头信息无法区分，按照扩展名区分
     * </pre>
     *
     * @param file 文件
     * @return 类型，文件的扩展名，未找到为null
     */
    @Nullable
    public static String getType(@NotNull MultipartFile file) {
        String typeName;
        try {
            typeName = FileTypeUtil.getType(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (null == typeName) {
            // 未成功识别类型，扩展名辅助识别
            typeName = FileUtil.extName(file.getOriginalFilename());
        } else if ("xls".equals(typeName)) {
            // xls、doc、msi的头一样，使用扩展名辅助判断
            String extName = FileUtil.extName(file.getOriginalFilename());
            if ("doc".equalsIgnoreCase(extName)) {
                typeName = "doc";
            } else if ("msi".equalsIgnoreCase(extName)) {
                typeName = "msi";
            }
        } else if ("zip".equals(typeName)) {
            // zip可能为docx、xlsx、pptx、jar、war等格式，扩展名辅助判断
            String extName = FileUtil.extName(file.getOriginalFilename());
            if ("docx".equalsIgnoreCase(extName)) {
                typeName = "docx";
            } else if ("xlsx".equalsIgnoreCase(extName)) {
                typeName = "xlsx";
            } else if ("pptx".equalsIgnoreCase(extName)) {
                typeName = "pptx";
            } else if ("jar".equalsIgnoreCase(extName)) {
                typeName = "jar";
            } else if ("war".equalsIgnoreCase(extName)) {
                typeName = "war";
            }
        }
        return typeName;
    }

    /**
     * 根据文件名获取文件类型
     *
     * @param filename 文件名
     * @return typename
     */
    public static String getTypeByFilename(String filename) {
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 通过文件名获取Content-Type
     *
     * @param typename 文件格式名
     * @return Content-Type
     */
    public static String getContentType(@NotNull String typename) {
        return switch (typename) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "zip" -> "application/zip";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    /**
     * 判断文件是否为图片（png jpg格式）
     *
     * @param file 文件
     */
    public static boolean isImage(@NotNull MultipartFile file) {
        String type = getType(file);
        return "jpg".equals(type) || "png".equals(type) || "jpeg".equals(type);
    }

    /**
     * 是否为不允许上传的文件格式
     *
     * @param typeName 文件格式名
     */
    public static boolean isAllowUploadType(@NotNull String typeName) {
        return !typeName.equalsIgnoreCase("exe") && !typeName.equalsIgnoreCase("html") && !typeName.equalsIgnoreCase("htm") && !typeName.equalsIgnoreCase("deb") && !typeName.equalsIgnoreCase("php");
    }

    /**
     * 是否为浏览器内能打开的文件
     *
     * @param typeName 文件名
     */
    public static boolean isInlineFile(@NotNull String typeName) {
        return switch (typeName) {
            case "jpg", "jpeg", "png",
                    "pdf",
                    "mp3", "mp4" -> true;
            default -> false;
        };
    }

    /**
     * 截取指定长度的字符串
     *
     * @param str    要截取的字符串
     * @param length 指定长度
     * @return 截取的结果
     */
    public static String getSpecifiedString(String str, int length) {
        if (str == null) return null;
        return (str.length() > length) ? str.substring(0, length - 1) + "……" : str;
    }

    /**
     * 获得方法或类上的注解
     *
     * @param method          方法
     * @param annotationClass 注解
     */
    @Nullable
    public static <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
        if (method == null) return null;
        if (method.isAnnotationPresent(annotationClass)) {
            return AnnotationUtils.getAnnotation(method, annotationClass);
        } else {
            return AnnotationUtils.getAnnotation(method.getDeclaringClass(), annotationClass);
        }
    }

    /**
     * 获得八位的UUID
     *
     * @return UUID
     */
    @NotNull
    public static String creatShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @NotNull
    public static String genetateRandomString(@NotNull Integer length, String allowedChars) {
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(allowedChars.length());
            char randomChar = allowedChars.charAt(randomIndex);
            stringBuilder.append(randomChar);
        }

        return stringBuilder.toString();
    }
}
