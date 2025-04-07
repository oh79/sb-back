package com.sb.board.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sb.board.domain.Post;
import com.sb.board.domain.User;
import com.sb.board.repository.PostRepository;
import com.sb.board.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 전체 게시글 조회
    @GetMapping
    public List<Post> getPosts() {
        return postRepository.findAll();
    }

    // 게시글 작성
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post requestPost, Authentication authentication) {
        // 인증 정보에서 username 추출 (폼 로그인 + Security 기본 구현 시)
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post newPost = Post.builder()
                .title(requestPost.getTitle())
                .content(requestPost.getContent())
                .user(user)
                .build();

        postRepository.save(newPost);
        return ResponseEntity.ok(newPost);
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPost(@PathVariable Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return ResponseEntity.ok(post);
    }

    // 수정
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @RequestBody Post requestPost) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        post.setTitle(requestPost.getTitle());
        post.setContent(requestPost.getContent());
        postRepository.save(post);

        return ResponseEntity.ok(post);
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        postRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }
}
