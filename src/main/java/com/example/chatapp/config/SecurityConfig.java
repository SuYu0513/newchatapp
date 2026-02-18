package com.example.chatapp.config;

import com.example.chatapp.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

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

    @Value("${app.debug.enabled:false}")
    private boolean debugEnabled;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value("${app.security.remember-me-key:default-secure-key}")
    private String rememberMeKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(authz -> {
                // 完全に公開されるリソース（認証不要） - 最優先
                authz.requestMatchers("/login", "/register", "/passreset", "/api/auth/**").permitAll();
                // プロフィール作成（新規登録直後のみ）
                authz.requestMatchers("/profile/create").permitAll();
                // 静的リソース（認証不要だが制限）
                authz.requestMatchers("/css/**", "/js/**", "/images/**", "/manifest.json", "/sw.js", "/icon-*.png", "/apple-touch-icon.png").permitAll();
                // エラーページ（認証不要 - 例外時のフォワード用）
                authz.requestMatchers("/error").permitAll();

                // デバッグモード関連（開発時のみ）
                if (debugEnabled) {
                    authz.requestMatchers("/login/ondbg", "/login/offdbg", "/login/dbgmode").permitAll();
                    authz.requestMatchers("/icon-generator.html").permitAll();
                }

                // H2 Console（開発時のみ）
                if (h2ConsoleEnabled) {
                    authz.requestMatchers("/h2-console/**").permitAll();
                }

                // APIエンドポイント（認証済みユーザーのみ）
                authz.requestMatchers("/api/**").authenticated();
                // その他すべてのリクエストは認証が必要
                authz.anyRequest().authenticated();
            })
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
                .key(rememberMeKey)
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(30 * 24 * 60 * 60) // 30日間有効
                .userDetailsService(userDetailsService)
                .alwaysRemember(true)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?sessionExpired=true")
            )
            // CSRF保護を有効化（CookieベースでJavaScriptからも読み取り可能）
            // CsrfTokenRequestAttributeHandler: XOR非適用でクッキー生値をそのまま検証（SPA向け）
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // API認証エンドポイントはCSRF除外（パスワードリセット等）
                .ignoringRequestMatchers("/api/auth/**")
            )
            .headers(headers -> {
                // H2コンソール用（開発時のみ）
                if (h2ConsoleEnabled) {
                    headers.frameOptions(frameOptions -> frameOptions.sameOrigin());
                } else {
                    headers.frameOptions(frameOptions -> frameOptions.deny());
                }
            });

        return http.build();
    }
}
