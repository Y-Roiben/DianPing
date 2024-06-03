package com.hmdp;


import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
//@RunWith(SpringRunner.class)
public class RedisTemplateTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    @Test
    public void testList() {
        String typeKey = "cache:user:type";
//        List<stu> typeList = new ArrayList<>();
//        typeList.add(new stu("虎哥", 18));
//        typeList.add(new stu("虎弟", 19));
//        typeList.add(new stu("虎三", 20));
//
//        // 将typeList中元素转换为json字符串
//        List<String> typeListJson = new ArrayList<>();
//        for (stu stu : typeList) {
//            typeListJson.add(JSONUtil.toJsonStr(stu));
//        }
//
//        // 存入redis
//        stringRedisTemplate.opsForList().leftPushAll(typeKey, typeListJson);
        // 读取
        List<String> cacheList = stringRedisTemplate.opsForList().range(typeKey, 0, -1);
        // 将json字符串转换为stu对象
        List<stu> stuList = new ArrayList<>();
        if (cacheList != null) {
            for (String s : cacheList) {
                stuList.add(JSONUtil.toBean(s, stu.class));
            }
        }
        System.out.println(stuList);

    }

    @Test
    void testRedisson() throws InterruptedException {
        RLock lock = redissonClient.getLock("anyLock");
        boolean success = lock.tryLock(1, 10, TimeUnit.SECONDS);
        if (success) {
            try {
                System.out.println("获取锁成功，执行业务逻辑");
                Thread.sleep(1000);
            } finally {
                lock.unlock();
                System.out.println("释放锁");
            }
        }
    }
}
