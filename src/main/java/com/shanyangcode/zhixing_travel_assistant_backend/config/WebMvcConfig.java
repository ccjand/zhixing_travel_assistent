package com.shanyangcode.zhixing_travel_assistant_backend.config;

import com.shanyangcode.zhixing_travel_assistant_backend.interceptors.CollectionInterceptor;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@AllArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AsyncTaskExecutor taskExecutor;

    private final CollectionInterceptor collectionInterceptor;

    @Override
    public void configureAsyncSupport(@NonNull AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(taskExecutor);
        configurer.setDefaultTimeout(120_000L);
    }

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    /**
     * 拦截器配置
     */

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //收集用户信息
        registry.addInterceptor(collectionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/register", "/user/login");
    }
}
