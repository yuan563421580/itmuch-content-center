package com.itmuch.contentcenter.feignclient.fallbackfactory;

import com.itmuch.contentcenter.domain.dto.user.UserDTO;
import com.itmuch.contentcenter.feignclient.UserCenterFeignClient;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXX_FallbackFactory 需要实现 FallbackFactory<> 接口
 *  接口泛型是需要实现的业务接口
 */
@Slf4j
@Component
public class UserCenterFeignClientFallbackFactory
        implements FallbackFactory<UserCenterFeignClient> {


    @Override
    public UserCenterFeignClient create(Throwable cause) {
        // 匿名内部类
        return new UserCenterFeignClient() {
            @Override
            public UserDTO findById(Integer id) {
                log.warn("远程调用被限流/降级了", cause);
                UserDTO userDTO = new UserDTO();
                userDTO.setWxNickname("FallbackFactory一个默认用户");
                return userDTO;
            }
        };
    }

}
