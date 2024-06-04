package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private UserServiceImpl userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
        String key = "blog:like:" + blog.getId();
        Long userId = UserHolder.getUser().getId();
        Boolean isLike = stringRedisTemplate.opsForSet()
                .isMember(key, userId.toString());
        blog.setIsLike(Boolean.TRUE.equals(isLike));
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
        String key = "blog:like:" + id;
        // 判断是否点赞过
        Boolean isLike = stringRedisTemplate.opsForSet()
                .isMember(key, userId.toString());
        if (Boolean.TRUE.equals(isLike)) {
            boolean isSuccess = update().setSql("liked = liked - 1")
                    .eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
        }else {
            boolean isSuccess = update().setSql("liked = liked + 1")
                    .eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForSet().add(key, userId.toString());
            }
        }
        return Result.ok();
    }
}
