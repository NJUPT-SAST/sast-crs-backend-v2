package com.sast.crs.config;

import com.sast.crs.interceptor.RequestInterceptor;
import com.sast.crs.interceptor.UserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private UserInterceptor userInterceptor;
    private RequestInterceptor requestInterceptor;

    @Autowired
    public void setUserInterceptor(UserInterceptor userInterceptor) {
        this.userInterceptor = userInterceptor;
    }

    @Autowired
    public void setRequestInterceptor(RequestInterceptor requestInterceptor) {
        this.requestInterceptor = requestInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/getValidateCode", "/error");

        registry.addInterceptor(requestInterceptor)
                .addPathPatterns("/**");
    }
}
