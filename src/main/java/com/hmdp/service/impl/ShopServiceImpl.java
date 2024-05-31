package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.Serializable;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

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
        // 1 从redis 查询缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String stringJson = stringRedisTemplate.opsForValue().get(shopKey);
        // 判断是否存在
        if (StrUtil.isNotBlank(stringJson)){
            // 存在, 直接返回
            Shop shop = JSONUtil.toBean(stringJson, Shop.class);
            return Result.ok(shop);
        }
        // 2 不存在从数据库查询
        Shop shop = getById(id);
        if (shop == null){
            return Result.fail("商铺不存在");
        }
        // 3 存入redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop));

        return Result.ok(shop);
    }
}
