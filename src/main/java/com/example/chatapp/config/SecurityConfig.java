package com.example.chatapp.config;

import com.example.chatapp.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    @Lazy
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    
    @Autowired
    @Lazy
    private CustomLogoutSuccessHandler logoutSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(authz -> authz
                // 完全に公開されるリソース（認証不要）
                .requestMatchers("/login", "/register").permitAll()
                // プロフィール作成（新規登録直後のみ）
                .requestMatchers("/profile/create").permitAll()
                // APIエンドポイント（認証済みユーザーのみ）
                .requestMatchers("/api/**").authenticated()
                // アイコンジェネレーター（認証不要）
                .requestMatchers("/icon-generator.html").permitAll()
                // 静的リソース（認証不要だが制限）
                .requestMatchers("/css/**", "/js/**", "/images/**", "/manifest.json", "/sw.js", "/icon-*.png").permitAll()
                // デバッグモード関連（特別扱い）
                .requestMatchers("/login/ondbg", "/login/offdbg", "/login/dbgmode").permitAll()
                // H2 Console（開発時のみ）
                .requestMatchers("/h2-console/**").permitAll()
                // その他すべてのリクエストは認証が必要
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("uniqueAndSecretKey")
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(30 * 24 * 60 * 60) // 30日間有効
                .userDetailsService(userDetailsService)
                .alwaysRemember(true) // チェックボックスに関係なく常に有効
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()) // 一時的にCSRF保護を無効化
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin()) // H2コンソール用
            );

        return http.build();
    }
}
