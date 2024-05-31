package com.hmdp;


import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
//@RunWith(SpringRunner.class)
public class RedisTemplateTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
}
