package com.example.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * Web設定クラス
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // アップロードされたアバター画像の静的リソースマッピング
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:src/main/resources/static/uploads/avatars/");

        // マッチング用写真の静的リソースマッピング
        registry.addResourceHandler("/uploads/matching/**")
                .addResourceLocations("file:src/main/resources/static/uploads/matching/");
        
        // CSS/JSファイルにキャッシュ制御ヘッダーを追加（開発環境向け）
        registry.addResourceHandler("/css/**", "/js/**")
                .addResourceLocations("classpath:/static/css/", "classpath:/static/js/")
                .setCacheControl(CacheControl.noCache()
                    .mustRevalidate()
                    .cachePrivate());
        
        // その他の静的リソース（画像など）は短いキャッシュを設定
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES));
    }
}
