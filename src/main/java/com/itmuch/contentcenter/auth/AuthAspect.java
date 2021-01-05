package com.itmuch.contentcenter.auth;

import com.itmuch.contentcenter.util.JwtOperator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 登录校验切面实现类
 *
 *  注解 @Aspect 标识类是一个切面
 *  @Around("@annotation(com.itmuch.usercenter.auth.CheckLogin)") 表示只要方法中加入了 @CheckLogin 就会进入到该方法
 *
 */
@Aspect
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthAspect {

    private final JwtOperator jwtOperator;

    /**
     * 校验token
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.itmuch.contentcenter.auth.CheckLogin)")
    public Object checkLogin(ProceedingJoinPoint point) throws Throwable {
        this.checkToken();
        return point.proceed();
    }

    @Around("@annotation(com.itmuch.contentcenter.auth.CheckAuthorization)")
    public Object checkAuthorization(ProceedingJoinPoint point) throws Throwable {
        try {
            // 1. 验证token是否合法；
            this.checkToken();

            // 2. 验证用户角色是否匹配
            HttpServletRequest request = this.getHttpServletRequest();
            String role = (String) request.getAttribute("role");

            // 通过反射获取到当前需要检验的角色值
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            CheckAuthorization annotation = method.getAnnotation(CheckAuthorization.class);

            String value = annotation.value();

            if (!Objects.equals(role, value)) {
                throw new SecurityException("用户无权访问！");
            }

        } catch (Throwable throwable) {
            throw new SecurityException("用户无权访问！", throwable);
        }
        return point.proceed();
    }

    /**
     * 获取 HttpServletRequest
     * @return
     */
    private HttpServletRequest getHttpServletRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = attributes.getRequest();
        return request;
    }

    /**
     * 实现 token 校验
     */
    private void checkToken() {
        try {
            // 1. 从header里面获取token
            HttpServletRequest request = this.getHttpServletRequest();

            String token = request.getHeader("X-Token");

            // 2. 校验token是否合法&是否过期；如果不合法或已过期直接抛异常；如果合法放行
            Boolean isValid = jwtOperator.validateToken(token);
            if (!isValid) {
                throw new SecurityException("Token不合法！");
            }

            // 3. 如果校验成功，那么就将用户的信息设置到request的attribute里面
            Claims claims = jwtOperator.getClaimsFromToken(token);
            request.setAttribute("id", claims.get("id"));
            request.setAttribute("wxNickname", claims.get("wxNickname"));
            request.setAttribute("role", claims.get("role"));
        } catch (Throwable throwable) {
            throw new SecurityException("Token不合法");
        }
    }

}
