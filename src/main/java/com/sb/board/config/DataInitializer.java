package com.sb.board.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.sb.board.domain.Role;
import com.sb.board.domain.User;
import com.sb.board.repository.RoleRepository;
import com.sb.board.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 시작 시 기본 데이터를 초기화하는 클래스입니다.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. 역할 생성 (ROLE_USER, ROLE_ADMIN)
        Role userRole = roleRepository.findByRoleName("ROLE_USER").orElseGet(() -> {
            System.out.println(">>> ROLE_USER 역할 생성");
            return roleRepository.save(Role.builder().roleName("ROLE_USER").build());
        });

        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN").orElseGet(() -> {
            System.out.println(">>> ROLE_ADMIN 역할 생성");
            return roleRepository.save(Role.builder().roleName("ROLE_ADMIN").build());
        });

        // 2. 기본 사용자 생성 (일반 사용자: user/user)
        userRepository.findByUsername("user").orElseGet(() -> {
            System.out.println(">>> 일반 사용자(user) 생성");
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user"))
                    .role(userRole)
                    .build();
            return userRepository.save(user);
        });

        // 3. 관리자 사용자 생성 (관리자: admin/admin)
        userRepository.findByUsername("admin").orElseGet(() -> {
            System.out.println(">>> 관리자 사용자(admin) 생성");
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .role(adminRole)
                    .build();
            return userRepository.save(admin);
        });
    }
} 