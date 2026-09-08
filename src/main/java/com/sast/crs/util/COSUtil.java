package com.sast.crs.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.util.IoUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.Headers;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.sast.crs.entity.File;
import com.sast.crs.enums.ErrorEnum;
import com.sast.crs.exception.LocalRuntimeException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class COSUtil {

    /**
     * Bucket中的文件夹：PUBLIC为公读（比赛封面），PRIVATE为私有（作品文件）
     */
    public enum Folder {
        PUBLIC, PRIVATE
    }

    private final COSClient cosClient;
    private final String bucketName;
    private final String endpoint;
    private final String publicFolder;
    private final String privateFolder;
    private final Integer uploadExpiredTime;
    private final Integer downloadExpiredTime;

    public COSUtil(@Value("${file.COS.secretId:}") String secretId, @Value("${file.COS.secretKey:}") String secretKey, @Value("${file.COS.region}") String region, @Value("${file.COS.uploadExpiredTime:}") Integer uploadExpiredTime, @Value("${file.COS.downloadExpiredTime:}") Integer downloadExpiredTime, @Value("${file.COS.bucketName:}") String bucketName, @Value("${file.COS.publicFolder:}") String publicFolder, @Value("${file.COS.privateFolder:}") String privateFolder) {
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        this.cosClient = new COSClient(credentials, new ClientConfig(new Region(region)));
        this.bucketName = bucketName;
        this.publicFolder = publicFolder;
        this.privateFolder = privateFolder;
        this.endpoint = "https://" + bucketName + ".cos." + region + ".myqcloud.com";
        this.uploadExpiredTime = uploadExpiredTime;
        this.downloadExpiredTime = downloadExpiredTime;
    }

    private String getBaseFolderName(Folder folder) {
        return folder == Folder.PUBLIC ? publicFolder : privateFolder;
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
        // 文件路径格式 comId/work/teamId/input-fileName
        String objectName = comId + "/work/" + teamId + "/" + input + "-" + CommonUtil.creatShortUUID() + "-" + filename;
        return createUploadCertificate(objectName, Folder.PRIVATE);
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
        // 文件路径格式 comId/cover/fileName
        String objectName = comId + "/cover/" + file.getOriginalFilename();

        return uploadFile(file, objectName, Folder.PUBLIC);
    }

    /**
     * 删除远程的文件
     *
     * @param url    文件的完整URL
     * @param folder 文件所在的文件夹
     */
    public void deleteFile(String url, Folder folder) {
        log.info("删除COS中的文件，文件地址：{}", url);
        String baseFolderName = getBaseFolderName(folder);
        String objectName = getObjectNameCOS(url);
        String key = baseFolderName + "/" + objectName;
        cosClient.deleteObject(bucketName, key);
    }

    /**
     * 获取下载凭证（仅针对私有的Bucket）
     *
     * @param url 文件url
     * @return url 带有凭证的url
     */
    public String getDownloadCertificate(String url) {
        log.info("获取从COS下载凭证，文件地址：{}", url);
        String objectName = getObjectNameCOS(url);
        String key = privateFolder + "/" + objectName;
        Date expirationDate = new Date(System.currentTimeMillis() + downloadExpiredTime * 60 * 1000);
        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucketName, key, HttpMethodName.GET);
        req.setExpiration(expirationDate);
        req.putCustomRequestHeader(Headers.HOST, cosClient.getClientConfig().getEndpointBuilder().buildGeneralApiEndpoint(bucketName));

        URL presignedUrl = cosClient.generatePresignedUrl(req);
        return presignedUrl.toString();
    }

    /**
     * 打包下载文件
     * 已知问题：
     * 在下载时浏览器不会显示下载进度
     *
     * @param response HTTP响应
     * @param files    List<File>
     * @param zipName  zip文件名
     */
    public void downloadPackFile(@NotNull HttpServletResponse response, @NotNull List<File> files, @NotNull String zipName) throws IOException {
        if (files.isEmpty()) {
            throw new LocalRuntimeException(ErrorEnum.COS_FILE_NOT_EXIST);
        }
        response.reset();
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String headerFilename = "filename=\"" + zipName + "\"; filename*=utf-8''" + URLEncoder.encode(zipName, StandardCharsets.UTF_8);
        response.addHeader("Content-disposition", "attachment; " + headerFilename);

        ServletOutputStream outputStream = response.getOutputStream();
        ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(outputStream);
        zipStream.setUseZip64(Zip64Mode.AsNeeded);
        for (File file : files) {
            log.info("获取COS中文件的比特流，文件地址：{}", file.getUrl());
            String objectName = getObjectNameCOS(file.getUrl());
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, privateFolder + "/" + objectName);
            try {
                COSObject cosObject = cosClient.getObject(getObjectRequest);
                ZipArchiveEntry entry = new ZipArchiveEntry(file.getInput() + "-" + getOriginalFilename(file.getUrl()));
                zipStream.putArchiveEntry(entry);
                zipStream.write(IoUtils.toByteArray(cosObject.getObjectContent()));
                zipStream.closeArchiveEntry();
            } catch (RuntimeException e) {
                zipStream.close();
                e.printStackTrace();
                throw new LocalRuntimeException(ErrorEnum.COS_FAILED_DOWNLOAD_ERROR);
            }
        }
        zipStream.close();
    }

    /**
     * 判断字符串是否为Bucket上的文件地址
     *
     * @param content 字符串内容
     */
    public Boolean isBucketURL(String content) {
        if (StringUtils.isEmpty(content))
            return false;
        try {
            String hostFromUser = URI.create(content).toURL().getHost();
            String host = endpoint.substring(8);
            return host.equalsIgnoreCase(hostFromUser);
        } catch (MalformedURLException | IllegalArgumentException e) {
            return false;
        }
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

    /**
     * 获取上传凭证
     *
     * @param objectName 文件在COS中的路径，如 example/example.zip
     * @param folder     文件所在的文件夹
     * @return 文件的URL和没有参数的文件URL
     */
    private Map<String, String> createUploadCertificate(String objectName, Folder folder) {
        String baseFolderName = getBaseFolderName(folder);
        String key = baseFolderName + "/" + objectName;
        String clearUrl = endpoint + "/" + key;
        log.info("向COS获取上传凭证，文件地址：{}", clearUrl);
        Date expirationDate = new Date(System.currentTimeMillis() + uploadExpiredTime * 60 * 1000);
        // 填写本次请求的参数，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的参数
        Map<String, String> params = new HashMap<>();
        params.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // 填写本次请求的头部，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的头部
        Map<String, String> headers = new HashMap<>();
        headers.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        HttpMethodName method = HttpMethodName.PUT;

        URL url = cosClient.generatePresignedUrl(bucketName, key, expirationDate, method, headers, params);
        Map<String, String> map = new HashMap<>();
        map.put("url", url.toString());
        map.put("clearUrl", clearUrl);
        return map;
    }

    /**
     * 向COS上传文件
     *
     * @param file       要上传的文件
     * @param objectName 文件在COS中的路径，如 example/example.zip
     * @param folder     文件所在的文件夹
     * @return 文件的URL
     */
    @NotNull
    private String uploadFile(MultipartFile file, @NotNull String objectName, Folder folder) {
        String baseFolderName = getBaseFolderName(folder);
        String key = baseFolderName + "/" + objectName;
        String clearUrl = endpoint + "/" + key;
        log.info("向COS上传文件，文件地址：{}", clearUrl);
        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, key, file.getInputStream(), null);
            request.setCannedAcl(CannedAccessControlList.PublicRead);
            cosClient.putObject(request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new LocalRuntimeException(ErrorEnum.COS_FAILED_UPLOAD_ERROR);
        }
        log.info("上传成功，文件URL：{}", clearUrl);
        return clearUrl;
    }
}
