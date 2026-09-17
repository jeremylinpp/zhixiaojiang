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
    SecurityFilterChain filterChain(HttpSecurity http, SessionRevocationService revocations, ObjectMapper json, org.springframework.jdbc.core.JdbcTemplate db) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setHeaderName("X-CSRF-TOKEN");
        http.csrf(c -> c.csrfTokenRepository(csrf).ignoringRequestMatchers("/api/v1/auth/login")).cors(c -> c.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(a -> a.requestMatchers("/api/v1/auth/login", "/api/v1/auth/csrf", "/actuator/health").permitAll()
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .requestMatchers("/api/v1/student-portal/**").hasRole("STUDENT")
                        .anyRequest().hasRole("TEACHER"))
                .sessionManagement(s -> s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtFilter(secret, revocations, json, db), UsernamePasswordAuthenticationFilter.class)
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
        private final ObjectMapper json;
        private final org.springframework.jdbc.core.JdbcTemplate db;

        JwtFilter(String secret, SessionRevocationService revocations, ObjectMapper json, org.springframework.jdbc.core.JdbcTemplate db) {
            this.secret = secret;
            this.revocations = revocations;
            this.json=json;
            this.db=db;
        }

        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
            String token = readToken(req);
            if (token != null && !revocations.isRevoked(token)) {
                String[] parts = token.split("\\.");
                if (parts.length == 3 && SessionToken.sign(parts[0] + "." + parts[1], secret).equals(parts[2])) {
                    try {
                    var payload = json.readTree(Base64.getUrlDecoder().decode(parts[1]));
                    long expires = payload.path("exp").asLong();
                    if (expires > Instant.now().getEpochSecond()) {
                        String subject = payload.path("sub").asText();
                        String username = payload.path("username").asText();
                        String role = payload.path("role").asText();
                        long id=Long.parseLong(subject);
                        boolean valid=id>0 && !username.isBlank() && List.of("TEACHER","STUDENT").contains(role);
                        if(valid && "STUDENT".equals(role)) valid=!db.queryForList("select a.user_id from student_account a join sys_user u on u.id=a.user_id join student s on s.id=a.student_id where a.user_id=? and a.session_version=? and u.role='STUDENT' and s.status='ACTIVE'",Long.class,id,payload.path("sv").asLong()).isEmpty();
                        if(valid) {
                        SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                        req.setAttribute("userId", subject);
                        }
                    }
                    } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException ignored) {
                        SecurityContextHolder.clearContext();
                    } catch (org.springframework.dao.DataAccessException unavailable) {
                        res.setStatus(503);res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write(json.writeValueAsString(ApiResult.fail("503","账号服务暂时不可用")));return;
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

    }
}
