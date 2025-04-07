package com.sb.board.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sb.board.domain.Role;
import com.sb.board.domain.User;
import com.sb.board.repository.RoleRepository;
import com.sb.board.repository.UserRepository;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        // userRepository / roleRepository 로직은 자유롭게
        // 예시: username 중복 체크, role 찾기 등
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        Role defaultRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        // 패스워드 암호화
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPw = encoder.encode(request.getPassword());

        // 새로운 User 엔티티 생성
        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPw)
                .role(defaultRole)
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("Signup success");
    }

    @Data
    public static class SignupRequest {
        private String username;
        private String password;
    }
}
