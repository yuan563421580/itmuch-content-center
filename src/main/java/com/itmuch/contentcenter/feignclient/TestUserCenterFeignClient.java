package com.itmuch.contentcenter.feignclient;

import com.itmuch.contentcenter.domain.dto.user.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 相同名称 @FeignClient(name = "user-center") 会导致报错：
 *  Consider renaming one of the beans or enabling overriding by setting spring.main.allow-bean-definition-overriding=true
 *  按照yml中配置这行代码即可
 */
@FeignClient(name = "user-center")
public interface TestUserCenterFeignClient {

    /**
     * query(UserDTO userDTO) 调用会报错：
     *  feign.FeignException$MethodNotAllowed: status 405 reading TestUserCenterFeignClient#query(UserDTO)
     *  由异常可知，尽管我们指定了GET方法，Feign依然会使用POST方法发送请求。于是导致了异常。
     *  正确写法如下: @SpringQueryMap: query(@SpringQueryMap UserDTO userDTO);
     *  或者使用最为直观的方式，URL有几个参数，Feign接口中的方法就有几个参数。使用@RequestParam注解指定请求的参数是什么。
     *
     */

    @GetMapping("/q")
    UserDTO query(@SpringQueryMap UserDTO userDTO);

    @GetMapping("/q")
    UserDTO query(@RequestParam("id")Integer id, @RequestParam("wxId")String wxId);

    @PostMapping("/post")
    public UserDTO post(@RequestBody UserDTO user);

}
