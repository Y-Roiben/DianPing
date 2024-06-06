package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserServiceImpl userService;
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        if (isFollow) {
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean success = save(follow);
            if (!success) {
                return Result.fail("关注失败");
            }else {
                stringRedisTemplate.opsForSet()
                        .add(key, followUserId.toString());
            }

        } else {
            boolean remove = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            if (!remove) {
                return Result.fail("取消关注失败");
            }else {
                stringRedisTemplate.opsForSet()
                        .remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result followOrNot(Long id) {
        Integer count = query().eq("user_id", UserHolder.getUser().getId())
                .eq("follow_user_id", id)
                .count();
        if (count > 0) {
            return Result.ok(true);
        }
        return Result.ok(false);
    }

    @Override
    public Result common(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key1 = "follows:" + userId;
        String key2 = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        // 有交集说明有共同关注
        if (intersect != null && !intersect.isEmpty()) {
            List<Long> ids = intersect.stream().map(Long::parseLong)
                    .collect(Collectors.toList());
            List<User> users = userService.listByIds(ids);
            List<UserDTO> userDTOS = users.stream().map(user ->
                            BeanUtil.copyProperties(user, UserDTO.class))
                    .collect(Collectors.toList());
            return Result.ok(userDTOS);
        }
        return Result.ok();
    }
}
