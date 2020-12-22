package com.itmuch.contentcenter.feignclient;

import com.itmuch.contentcenter.configuration.UserCenterFeignClientConfiguration;
import com.itmuch.contentcenter.domain.dto.user.UserDTO;
import com.itmuch.contentcenter.feignclient.fallback.UserCenterFeignClientFallback;
import com.itmuch.contentcenter.feignclient.fallbackfactory.UserCenterFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign 调用实现
 *
 * configuration 是配置信息
 *
 * fallback 和 fallbackFactory 熔断降级默认处理方法选择使用一个即可：
 *  fallback 是熔断降级进入的方法
 *  fallbackFactory 是熔断降级进入的方法，可以打印异常 Throwable，【推荐使用】
 *
 */
//@FeignClient(name = "user-center", configuration = UserCenterFeignClientConfiguration.class)
@FeignClient(name = "user-center",
        //fallback = UserCenterFeignClientFallback.class,
        fallbackFactory = UserCenterFeignClientFallbackFactory.class
)
public interface UserCenterFeignClient {

    /**
     * http://user-center/users/{id}
     * @param id
     * @return
     */
    @GetMapping("/users/{id}")
    UserDTO findById(@PathVariable Integer id);
}