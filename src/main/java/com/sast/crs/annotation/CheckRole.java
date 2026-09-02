package com.sast.crs.annotation;

import com.sast.crs.enums.UserRoleEnum;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckRole {
    @AliasFor("role")
    UserRoleEnum value() default UserRoleEnum.COMMON_STUDENT;
    @AliasFor("value")
    UserRoleEnum role() default UserRoleEnum.COMMON_STUDENT;
}
