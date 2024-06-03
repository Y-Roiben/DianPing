package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final long BEGIN_TIMESTAMP = 1704067200L; // 2024-01-01 00:00:00 的时间戳
    // ID 生成策略
    public long nextId(String keyPrefix) {
        // 1. 获取当前时间戳
        long nowSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        // 2. 计算当前时间戳与开始时间戳的差值
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 当天日期
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 3. 生成 ID
        long count = stringRedisTemplate.opsForValue().
                increment("icr:" + keyPrefix + ":" + date);

        // 4. 拼接 ID
        return (timestamp << 32) | count;
    }

}
