package com.sb.board.config;

import com.sb.board.domain.User;
import com.sb.board.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import java.util.stream.Collectors;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security 설정 클래스
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CORS 설정 적용
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // CSRF 비활성화 (Stateless API 서버의 경우)
        http.csrf(csrf -> csrf.disable());

        // URL 접근 권한 설정
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 모든 OPTIONS 요청 허용
                .requestMatchers("/api/auth/**", "/login").permitAll() // 회원가입, 로그인 경로는 모두 허용
                .requestMatchers("/api/posts/**").hasAnyRole("USER", "ADMIN") // 게시판 관련 API는 USER 또는 ADMIN 역할 필요
                .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
        );

        // 폼 로그인 설정 커스터마이징 (JSON 응답 반환)
        http.formLogin(form -> form
                .loginProcessingUrl("/login") // 로그인 처리 URL
                .successHandler((request, response, authentication) -> {
                    // 로그인 성공 시 200 OK 와 사용자 정보(이름, 역할) 반환
                    try {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");

                        String username = authentication.getName(); // 인증된 사용자 이름 가져오기
                        // 역할 정보 가져오기 (여러 역할이 있을 수 있으므로 리스트로 처리)
                        String roles = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(",")); // 예: "ROLE_USER,ROLE_ADMIN" 형태

                        // JSON 응답 생성
                        String jsonResponse = String.format("{\"message\": \"Login successful\", \"username\": \"%s\", \"roles\": \"%s\"}",
                                username, roles);
                        response.getWriter().write(jsonResponse);

                    } catch (IOException e) {
                        e.printStackTrace(); // 로깅 프레임워크 사용 권장
                    }
                })
                .failureHandler((request, response, exception) -> {
                    // 로그인 실패 시 401 Unauthorized 와 에러 메시지 반환 (단순화)
                    try {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"error\": \"Login failed\"}");
                    } catch (IOException e) {
                        e.printStackTrace(); // 로깅 프레임워크 사용 권장
                    }
                })
                .permitAll() // 로그인 관련 URL 접근 허용
        );

        // 예외 처리 설정 (AuthenticationEntryPoint 커스터마이징)
        http.exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, authException) -> {
                // 인증되지 않은 접근 시 401 Unauthorized 와 JSON 응답 반환
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                try {
                    String errorMessage = authException.getMessage() != null ? authException.getMessage().replace("\"", "\\\"") : "Unauthorized";
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + errorMessage + "\"}");
                } catch (IOException e) {
                    e.printStackTrace(); // 로깅 프레임워크 사용 권장
                }
            })
        );

        return http.build();
    }

    /**
     * CORS 설정을 정의하는 Bean
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000")); // 프론트엔드 오리진 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 허용할 HTTP 메서드
        configuration.setAllowedHeaders(Arrays.asList("*")); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 자격 증명(쿠키 등) 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 CORS 설정 적용
        return source;
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 사용자 정보를 조회하는 UserDetailsService Bean
     * UserRepository를 사용하여 DB에서 사용자 정보를 가져온다.
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            // DB에서 사용자 이름으로 사용자 정보 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

            // Spring Security가 사용하는 UserDetails 객체로 변환하여 반환
            // 주의: user 객체에 getUsername(), getPassword(), getRole().getRoleName() 메소드가 존재해야 함 (Lombok @Getter 필요)
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getRoleName())) // 사용자의 역할을 GrantedAuthority로 변환
            );
        };
    }
}

