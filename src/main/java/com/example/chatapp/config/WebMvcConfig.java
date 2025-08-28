package com.example.chatapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginRedirectInterceptor loginRedirectInterceptor;

    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        // ルートパスは常にloginにリダイレクト
        registry.addRedirectViewController("/", "/login");
        
        // よくアクセスされる可能性があるパスを全てloginにリダイレクト
        registry.addRedirectViewController("/index", "/login");
        registry.addRedirectViewController("/home", "/login");
        registry.addRedirectViewController("/dashboard", "/login");
        registry.addRedirectViewController("/main", "/login");
        registry.addRedirectViewController("/app", "/login");
        
        // デフォルトエラーページもloginにリダイレクト
        registry.addRedirectViewController("/error", "/login");
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // すべてのリクエストにインターセプターを適用
        registry.addInterceptor(loginRedirectInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/manifest.json", "/sw.js", "/icon-*.png");
    }
}
