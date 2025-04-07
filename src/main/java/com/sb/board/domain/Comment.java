package com.sb.board.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post; // 어느 게시글에 속하는지

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // 댓글 작성자
}
