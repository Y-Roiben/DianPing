package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryTypeList() {
        String typeKey = "cache:shop:type";
        List<String> shopTypeCache = stringRedisTemplate.opsForList().range(typeKey, 0, -1);
        List<ShopType> shopTypeList;
        if (shopTypeCache == null || shopTypeCache.isEmpty()){
            // 从数据库查询
            shopTypeList = query().orderByAsc("sort").list();
            List<String> typeListJson = new ArrayList<>();
            for (ShopType shopType : shopTypeList) {
                String jsonStr = JSONUtil.toJsonStr(shopType);
                typeListJson.add(jsonStr);
            }
            // 存入redis
            stringRedisTemplate.opsForList().rightPushAll(typeKey, typeListJson);
        }else {
            shopTypeList = new ArrayList<>();
            for (String s : shopTypeCache) {
                shopTypeList.add(JSONUtil.toBean(s, ShopType.class));
            }
        }

        return Result.ok(shopTypeList);
    }
}
