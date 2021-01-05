package com.itmuch.contentcenter.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 实现 RestTemplate 传递 token 拦截器 实现
 * 同时需要在启动类 Application 中修改配置，
 *  在spring容器中创建RestTemplate时候设置interceptor
 */
public class RestTemplateTokenRelayInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // RequestContextHolder: 持有上下文的Request容器, 通过静态方法getRequestAttributes获取Request实例
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 转型成ServletRequestAttributes，因为RequestAttributes类是无法方便操作request、session这些原生servlet相关的对象或者属性
        ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest httpRequest = attributes.getRequest();

        String token = httpRequest.getHeader("X-Token");

        HttpHeaders headers = request.getHeaders();
        headers.add("X-Token", token);

        // 保证请求继续执行
        return execution.execute(request, body);
    }
}
