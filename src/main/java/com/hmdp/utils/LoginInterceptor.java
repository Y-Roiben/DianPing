package com.hmdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

public class LoginInterceptor implements HandlerInterceptor{


    @Override
    // 前置拦截器
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 是否需要拦截
        if (UserHolder.getUser() == null){
            response.setStatus(401);  // 未授权
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
