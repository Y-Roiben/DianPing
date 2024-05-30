package com.hmdp.utils;

import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor{
    @Override
    // 前置拦截器
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1 获取session中的用户信息
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");
        // 2. 判断用户是否存在
        if (user == null) {
            // 3. 不存在， 拦截
            response.setStatus(401);  // 未授权
            return false;
        }
        // 4. 存在, 保存用户信息到ThreadLocal
        UserHolder.saveUser((User) user);
        // 5. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
