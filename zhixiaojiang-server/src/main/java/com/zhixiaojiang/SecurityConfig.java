package com.zhixiaojiang;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final String secret;

    public SecurityConfig(org.springframework.core.env.Environment env) {
        secret = env.getProperty("app.jwt-secret", "change-me-in-local-env-please-32-chars");
    }

    static String sign(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String token(long userId, String username, String role, String secret) {
        String h = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        // jti 保证每次登录的令牌都不同：否则同一秒内退出后再登录会生成与已撤销令牌完全相同的字符串。
        String p = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"jti\":\"" + java.util.UUID.randomUUID() + "\",\"sub\":\"" + userId + "\",\"username\":\"" + username + "\",\"role\":\"" + role + "\",\"exp\":" + (Instant.now().getEpochSecond() + 28800) + "}").getBytes(StandardCharsets.UTF_8));
        return h + "." + p + "." + sign(h + "." + p, secret);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SessionRevocationService revocations) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setHeaderName("X-CSRF-TOKEN");
        http.csrf(c -> c.csrfTokenRepository(csrf).ignoringRequestMatchers("/api/v1/auth/login")).cors(c -> c.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(a -> a.requestMatchers("/api/v1/auth/**", "/actuator/health").permitAll().anyRequest().authenticated())
                .addFilterBefore(new JwtFilter(secret, revocations), UsernamePasswordAuthenticationFilter.class)
                .formLogin(f -> f.disable()).httpBasic(b -> b.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":\"401\",\"message\":\"未登录或会话已失效\",\"data\":{},\"requestId\":\"" + java.util.UUID.randomUUID() + "\"}");
                }));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of("http://127.0.0.1:5173", "http://localhost:5173"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    static class JwtFilter extends OncePerRequestFilter {
        private final String secret;
        private final SessionRevocationService revocations;

        JwtFilter(String secret, SessionRevocationService revocations) {
            this.secret = secret;
            this.revocations = revocations;
        }

        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
            String token = null;
            if (req.getCookies() != null)
                for (Cookie c : req.getCookies()) if ("ZJ_SESSION".equals(c.getName())) token = c.getValue();
            if (token != null && !revocations.isRevoked(token)) {
                String[] p = token.split("\\.");
                if (p.length == 3 && sign(p[0] + "." + p[1], secret).equals(p[2])) {
                    String payload = new String(Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8);
                    String exp = payload.replaceAll(".*\\\"exp\\\":([0-9]+).*", "$1");
                    long expires = exp.equals(payload) ? 0 : Long.parseLong(exp);
                    if (expires > Instant.now().getEpochSecond()) {
                        String sub = payload.replaceAll(".*\\\"sub\\\":\\\"([^\\\"]+).*", "$1");
                        String user = payload.replaceAll(".*\\\"username\\\":\\\"([^\\\"]+).*", "$1");
                        String role = payload.replaceAll(".*\\\"role\\\":\\\"([^\\\"]+).*", "$1");
                        SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                        req.setAttribute("userId", sub);
                    }
                }
            }
            chain.doFilter(req, res);
        }
    }
}
