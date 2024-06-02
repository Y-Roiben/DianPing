package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        //  Shop shop = queryWithPassThrough(id);


        // 互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null){
            return Result.fail("商铺不存在");
        }
        return Result.ok(shop);
    }


    // 通过缓存穿透
    public Shop queryWithPassThrough(Long id){
        // 1 从redis 查询缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String stringJson = stringRedisTemplate.opsForValue().get(shopKey);
        // 判断是否存在
        if (StrUtil.isNotBlank(stringJson)){
            // 存在, 直接返回
            return JSONUtil.toBean(stringJson, Shop.class);
        }
        // 缓存命中空值
        if (stringJson != null){
            return null;
        }
        // 2 不存在从数据库查询
        Shop shop = getById(id);
        if (shop == null){
            // 将空值存入redis, 防止缓存穿透
            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 3 存入redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return shop;

    }


    public Shop queryWithMutex(Long id){
        // 1 从redis 查询缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String stringJson = stringRedisTemplate.opsForValue().get(shopKey);
        // 判断是否存在
        if (StrUtil.isNotBlank(stringJson)){
            // 存在, 直接返回
            return JSONUtil.toBean(stringJson, Shop.class);
        }
        // 缓存命中空值
        if (stringJson != null){
            return null;
        }
        // 2 未命中, 实现缓存重建
        // 2.1 尝试获取锁

        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            if (!tryLock(lockKey)){
                // 未获取到锁, 休眠后重试
                Thread.sleep(1000);
                return queryWithMutex(id);
            }
            shop = getById(id);
            if (shop == null){
                // 将空值存入redis, 防止缓存穿透
                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // 3 存入redis
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 释放锁
            releaseLock(lockKey);
        }
        return shop;

    }

    @Override
    @Transactional // 开启事务
    public Result update(Shop shop) {
        // 更新数据库
        updateById(shop);
        // 删除缓存
        if (shop.getId() == null){
            return Result.fail("商铺id不能为空");
        }
        String shopKey = CACHE_SHOP_KEY + shop.getId();
        stringRedisTemplate.delete(shopKey);

        return Result.ok();
    }


    //  尝试获取锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(
                key, "1",
                LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    // 释放锁
    private void releaseLock(String key){
        stringRedisTemplate.delete(LOCK_SHOP_KEY + key);
    }


}
