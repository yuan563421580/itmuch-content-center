package com.itmuch.contentcenter.annotation;

import java.lang.annotation.*;

/**
 * 自定义@ScanIgnore实现
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScanIgnore {
}
