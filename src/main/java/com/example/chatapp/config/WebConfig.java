package com.example.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web設定クラス
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // アップロードされたアバター画像の静的リソースマッピング
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:src/main/resources/static/uploads/avatars/");
    }
}
