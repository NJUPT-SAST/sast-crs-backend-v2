package com.sast.crs.util;

import com.sast.crs.entity.File;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FileUtil {
    public static final int PUBLIC_BUCKET = 1;
    public static final int PRIVATE_BUCKET = 2;
    public static final int PUBLIC_FOLDER = 1;
    public static final int PRIVATE_FOLDER = 2;

    private COSUtil cosUtil;

    @Autowired
    public void setCosUtil(COSUtil cosUtil) {
        this.cosUtil = cosUtil;
    }

    /**
     * 向私有Bucket上传作品文件 文件路径格式
     * //buckName.endpoint/comId/work/teamId/input-uuid-fileName
     *
     * @param filename 要上传的文件名
     * @param comId    比赛ID
     * @param teamId   队伍ID
     * @param input    输入框名
     * @return 文件的URL和没有参数的url
     */
    public Map<String, String> getUploadCertificate(@NotNull String filename, @NotNull Long comId, @NotNull Long teamId, @NotNull String input) {
        String typeName = CommonUtil.getTypeByFilename(filename);
        if (typeName == null || !CommonUtil.isAllowUploadType(typeName)) {
            throw new LocalRuntimeException(ErrorEnum.INVALID_FILE_TYPE_ERROR);
        }
        // 文件路径格式 comId/work/teamId/input-fileName
        String objectName = comId + "/work/" + teamId + "/" + input + "-" + CommonUtil.creatShortUUID() + "-" + filename;
        return cosUtil.getUploadCertificateCOS(objectName, PRIVATE_FOLDER);
    }

    /**
     * 向公共Bucket上传比赛封面（仅允许jpg png格式，且大小小于5M） 文件路径格式
     * //buckName.endpoint/crs-public/comId/cover/fileName
     *
     * @param file  封面
     * @param comId 比赛ID
     * @return 封面的URL
     */
    public String uploadCover(@NotNull MultipartFile file, @NotNull Long comId) {
        if (file.getSize() > 5242880) {
            throw new LocalRuntimeException("图片大小超出限制(5MB)");
        }
        // 文件路径格式 comId/cover/fileName
        String objectName = comId + "/cover/" + file.getOriginalFilename();

        return cosUtil.uploadFileCOS(file, objectName, PUBLIC_BUCKET);
    }

    /**
     * 删除远程的文件
     *
     * @param url          文件的完整URL
     * @param bucketNumber Bucket对应的编号
     */
    public void deleteFile(String url, int bucketNumber) {
        cosUtil.deleteFileCOS(url, bucketNumber);
    }

    public void deleteFileCOS(String url, Integer folderNumber) {
        cosUtil.deleteFileCOS(url, folderNumber);
    }

    /**
     * 获取下载凭证
     *
     * @param url 文件url
     */
    public String getDownloadCertificate(String url) {
        return cosUtil.getDownloadCertificate(url);
    }

    /**
     * 打包下载文件 已知问题： 在下载时浏览器不会显示文下载进度
     *
     * @param response HTTP响应
     * @param files    List<File>
     * @param zipName  zip文件名
     */
    public void downloadPackFile(@NotNull HttpServletResponse response, @NotNull List<File> files, @NotNull String zipName) throws IOException {
        cosUtil.downloadPackFileCOS(response, files, zipName);
    }

    /**
     * 获得作品文件的原始文件名
     *
     * @param url 文件在COS上的URL
     * @return 作品文件原始文件名
     */
    @NotNull
    public static String getOriginalFilename(String url) {
        String name = getFileName(url); // 得到的是 1-xxxxxxxx-example.zip
        return name.substring(name.indexOf("-") + 10); // 得到example.zip
    }

    /**
     * 判断字符串是否为Bucket上的文件地址
     *
     * @param content 字符串内容
     */
    public boolean isBucketURL(String content) {
        return cosUtil.isCOSBucketURL(content);
    }

    /**
     * 通过URL获取文件路径（COS）
     *
     * @param urlString 文件的地址 例：https://endpoint/path/filename.zip
     * @return 文件路径 例：path/filename.zip
     */
    @NotNull
    public static String getObjectName(String urlString) {
        URL url;
        try {
            url = URI.create(urlString).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new LocalRuntimeException(ErrorEnum.INVALID_URL_ERROR);
        }
        return url.getPath().substring(1);
    }

    /**
     * 通过URL获取文件路径 COS
     *
     * @param urlString 文件的地址 例：https://endpoint/baseFolder/path/filename.zip
     * @return 文件路径 例：path/filename.zip
     */
    @NotNull
    public static String getObjectNameCOS(String urlString) {
        String path = getObjectName(urlString);
        return path.substring(path.indexOf("/") + 1);
    }

    /**
     * 通过URL获取文件名
     *
     * @param urlString 文件的完整URL
     * @return 文件名 例：example.zip
     */
    @NotNull
    public static String getFileName(String urlString) {
        String objectName = getObjectNameCOS(urlString);
        return objectName.substring(objectName.lastIndexOf("/") + 1);
    }
}
