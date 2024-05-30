package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

public class RefreshTokenInterceptor implements HandlerInterceptor{

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    // 前置拦截器
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1 获取session中的用户信息
        String token = request.getHeader("authorization");
        // 1.1 判断token是否存在
        if (StrUtil.isBlank(token)) {
            //1.2 不存在， 无需刷新token
            return true;
        }
        // 2. 基于token获取redis中的用户信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
//        Object user = session.getAttribute("user");

        // 2. 判断用户是否存在
        if (userMap.isEmpty()) {
            // 3. 不存在, 无需刷新token
            return true;
        }
        // 4. 存在,
        // 4.1 将HashMap转换为UserDTO
        UserDTO user = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 保存用户信息到ThreadLocal
        UserHolder.saveUser(user);
        // 4.2 刷新token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, 30, TimeUnit.MINUTES);
        // 5. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
