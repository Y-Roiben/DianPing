package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testSaveShop() {
        shopService.saveShop2Redis(1L, 1000L);
    }


    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                System.out.println("id = " + redisIdWorker.nextId("order"));
            }
            latch.countDown();
        };
        long beginTime = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
//            es.submit(task);
            new Thread(task).start();
        }
        latch.await();
        System.out.println("耗时：" + (System.currentTimeMillis() - beginTime)  + "ms");
    }
}
