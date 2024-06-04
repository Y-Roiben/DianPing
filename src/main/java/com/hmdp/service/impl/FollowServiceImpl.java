package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

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

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        Follow follow = new Follow();
        follow.setFollowUserId(followUserId);
        follow.setUserId(userId);
        if (isFollow) {
            save(follow);
        } else {
            remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
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
        return Result.ok();
    }
}
