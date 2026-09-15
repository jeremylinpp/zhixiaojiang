package com.zhixiaojiang.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixiaojiang.auth.SessionRevocationService;
import com.zhixiaojiang.auth.SessionToken;
import com.zhixiaojiang.common.ApiResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/** Web 安全配置：会话 Cookie、CSRF、CORS 与登录态过滤器。 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /** 会话 Cookie 名称，登录、退出与过滤器共用。 */
    public static final String SESSION_COOKIE = "ZJ_SESSION";

    /** 允许跨域访问的前端来源；部署到其他域名时通过配置或网关同源代理解决。 */
    private static final List<String> ALLOWED_ORIGINS = List.of("http://127.0.0.1:5173", "http://localhost:5173");

    private final String secret;

    public SecurityConfig(org.springframework.core.env.Environment env) {
        secret = env.getProperty("app.jwt-secret", "change-me-in-local-env-please-32-chars");
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SessionRevocationService revocations, ObjectMapper json) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setHeaderName("X-CSRF-TOKEN");
        http.csrf(c -> c.csrfTokenRepository(csrf).ignoringRequestMatchers("/api/v1/auth/login")).cors(c -> c.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(a -> a.requestMatchers("/api/v1/auth/**", "/actuator/health").permitAll().anyRequest().authenticated())
                .addFilterBefore(new JwtFilter(secret, revocations), UsernamePasswordAuthenticationFilter.class)
                .formLogin(f -> f.disable()).httpBasic(b -> b.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write(json.writeValueAsString(ApiResult.fail("401", "未登录或会话已失效")));
                }));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(ALLOWED_ORIGINS);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    /** 校验会话 Cookie 中的令牌，通过后把教师 id 写入请求属性供业务层使用。 */
    static class JwtFilter extends OncePerRequestFilter {
        private final String secret;
        private final SessionRevocationService revocations;

        JwtFilter(String secret, SessionRevocationService revocations) {
            this.secret = secret;
            this.revocations = revocations;
        }

        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
            String token = readToken(req);
            if (token != null && !revocations.isRevoked(token)) {
                String[] parts = token.split("\\.");
                if (parts.length == 3 && SessionToken.sign(parts[0] + "." + parts[1], secret).equals(parts[2])) {
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    long expires = longClaim(payload, "exp");
                    if (expires > Instant.now().getEpochSecond()) {
                        String subject = stringClaim(payload, "sub");
                        String username = stringClaim(payload, "username");
                        String role = stringClaim(payload, "role");
                        SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                        req.setAttribute("userId", subject);
                    }
                }
            }
            chain.doFilter(req, res);
        }

        private String readToken(HttpServletRequest req) {
            if (req.getCookies() == null) return null;
            for (Cookie cookie : req.getCookies()) if (SESSION_COOKIE.equals(cookie.getName())) return cookie.getValue();
            return null;
        }

        private long longClaim(String payload, String name) {
            String raw = claim(payload, name);
            return raw == null ? 0 : Long.parseLong(raw);
        }

        private String stringClaim(String payload, String name) {
            return claim(payload, name);
        }

        /**
         * 极简声明解析，沿用与原实现一致的正则语义（贪婪匹配取最后一个同名声明）。
         *
         * <p>声明缺失或格式不符时返回 null，令牌因缺少教师 id 而被当作未登录处理。
         * 严格解析（JSON 序列化与反序列化）以及对用户名引号的转义，见后续改造项。
         */
        private String claim(String payload, String name) {
            String pattern = name.equals("exp")
                    ? ".*\"exp\":([0-9]+).*"
                    : ".*\"" + name + "\":\"([^\"]+).*";
            String extracted = payload.replaceAll(pattern, "$1");
            return extracted.equals(payload) ? null : extracted;
        }
    }
}
