package com.itmuch.contentcenter.auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 使用注解 @Retention(RetentionPolicy.RUNTIME)
 *  使得类可以在运行时通过反射得到值
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckAuthorization {

    String value();

}
