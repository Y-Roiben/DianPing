package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.ILock;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.RedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void handlerVoucherOrder(VoucherOrder order) {
        // 一人一单
        Long userId = order.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId.toString());
        boolean success = lock.tryLock(); // 无参构造, 默认waitTime = 0, leaseTime = -1
        if (!success) {
            log.error("请勿重复提交");
            return;
        }
        try {
            saveVoucherOrder(order);
        } finally {
            lock.unlock();
        }
    }

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("secKill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result secKillVoucher(Long voucherId) {
        // 1. 执行lua脚本
        Long userId = UserHolder.getUser().getId();
        Long res = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString());
        // 2. 判断结果是否为0
        int result = res.intValue();
        if (result != 0){
            return Result.fail(result == 1 ? "库存不足" : "重复下单, 一人限购一单");
        }
        // 3. 具有购买资格
        // 3.1 生成订单信息, 保存到阻塞队列
        long orderId = redisIdWorker.nextId("order");
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setVoucherId(voucherId);
        order.setUserId(userId);

        // RabbitMQ发送消息
        rabbitTemplate.convertAndSend("voucherOrder.queue", order);

        return Result.ok(orderId);
    }




    @Override
    public void saveVoucherOrder(VoucherOrder order) {
        Long userId = order.getUserId();
        Long voucherId = order.getVoucherId();
        Integer count = query().eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        if (count > 0) {
            log.error("每人限购一张");
            return;
        }
        // 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            log.error("库存不足");
            return ;
        }
        save(order);
    }
}
