package com.sast.crs.schedule;

import com.alibaba.fastjson2.JSON;
import com.sast.crs.constant.RedisKeyConst;
import com.sast.crs.model.FileCache;
import com.sast.crs.util.COSUtil;
import com.sast.crs.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
public class CacheSchedule {
    private COSUtil cosUtil;
    private RedisUtil redisUtil;

    @Autowired
    public void setCosUtil(COSUtil cosUtil) {
        this.cosUtil = cosUtil;
    }

    @Autowired
    public void setRedisUtil(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 每天凌晨2点执行
     * 检查Redis中的缓存是否超过24小时
     * 超过之后删除缓存和文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanCacheTask() {
        // 按前缀扫描：既覆盖新格式（学号:比赛:字段），也能清掉历史遗留的两段式 key
        Set<String> keys = redisUtil.getKeys(RedisKeyConst.WORK_FILE_PREFIX + "*");
        for (String key : keys) {
            FileCache cache = JSON.parseObject((String) redisUtil.getOriginal(key), FileCache.class);
            Duration duration = Duration.between(cache.getDate(), LocalDateTime.now());
            if (duration.toHours() > 24) {
                cosUtil.deleteFile(cache.getUrl(), COSUtil.Folder.PRIVATE);
                redisUtil.deleteOriginal(key);
                log.info("上传的文件缓存已删除，Key：{}", key);
            }
        }
    }
}
