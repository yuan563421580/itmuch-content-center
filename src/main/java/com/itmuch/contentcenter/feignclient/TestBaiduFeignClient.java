package com.itmuch.contentcenter.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign 脱离 Ribbon 使用
 *
 * 注意：一定要配置 name , 名称可以随意起
 *  没有 name 会报错：java.lang.IllegalStateException: Either 'name' or 'value' must be provided in @FeignClient
 */
@FeignClient(name = "baidu", url = "www.baidu.com")
public interface TestBaiduFeignClient {

    @GetMapping("")
    String index();

}
