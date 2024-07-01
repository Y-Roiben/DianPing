package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;

    @Override
    public Result queryBlogById(Long id) {
        // 查询博文
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        // 查询用户
        queryBlogUser(blog);
        // 判断是否点赞过
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void queryBlogUser(Blog blog) {
        User user = userService.getById(blog.getUserId());
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    private void isBlogLiked(Blog blog) {
        // 判断是否点赞过
        if (UserHolder.getUser() == null) {
            return;
        }
        String key = BLOG_LIKED_KEY + blog.getId();
        Long userId = UserHolder.getUser().getId();
        boolean b = stringRedisTemplate.opsForZSet()
                .score(key, userId.toString()) != null;
        blog.setIsLike(b);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            // 查询用户
            queryBlogUser(blog);
            // 判断是否点赞过
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + id;
        // 判断是否点赞过
        Boolean isLike = stringRedisTemplate.opsForZSet()
                .score(key, userId.toString()) != null;
        if (Boolean.TRUE.equals(isLike)) {
            boolean isSuccess = update().setSql("liked = liked - 1")
                    .eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }else {
            boolean isSuccess = update().setSql("liked = liked + 1")
                    .eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(),
                        System.currentTimeMillis());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        // 查询top5
        String key = BLOG_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 != null && !top5.isEmpty()) {
            List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
            String idString = StrUtil.join(",", ids);
            List<User> users = userService.query()
                    .in("id", ids).last("ORDER BY FIELD(id, " + idString + ")").list();
            List<UserDTO> userDTOS = new ArrayList<>();
            for (User user : users) {
                UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
                userDTOS.add(userDTO);
            }
            return Result.ok(userDTOS);
        }
        return Result.ok();
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean save = save(blog);
        if (save) {
            // 查询粉丝
            List<Follow> follows = followService.query()
                    .eq("follow_user_id", user.getId())
                    .list();
            // 通知粉丝
            for (Follow follow : follows) {
                String key = "feed:" + follow.getUserId();
                stringRedisTemplate.opsForZSet().add(
                        key, blog.getId().toString(), System.currentTimeMillis()
                );
            }
            return Result.ok();
        }
        // 返回id
        return Result.fail("保存失败");
    }

    @Override
    public Result queryBlogOfFollow(Integer offset, Long max) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 查询收件箱
        String key = "feed:" + userId;
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max,
                        offset, SystemConstants.MAX_PAGE_SIZE);
        if (tuples == null || tuples.isEmpty()) {
            return Result.ok();
        }
        // 解析数据, 获取blogId, 时间戳, offset
        List<Long> blogIds = new ArrayList<>();
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            blogIds.add(Long.valueOf(tuple.getValue()));
            long Time = tuple.getScore().longValue();
            if (Time == minTime){
                os++;
            }else {
                minTime = Time;
                os = 1;
            }
        }
        // 查询博文
        String idString = StrUtil.join(",", blogIds);
        List<Blog> blogs =  query().in("id", blogIds)
                .last("ORDER BY FIELD(id, " + idString + ")").list();
        for (Blog blog : blogs) {
            // 查询用户
            queryBlogUser(blog);
            // 判断是否点赞过
            isBlogLiked(blog);
        }
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(os);
        return Result.ok(scrollResult);
    }
}
