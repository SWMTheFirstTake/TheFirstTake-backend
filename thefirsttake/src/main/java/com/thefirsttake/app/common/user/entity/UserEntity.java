package com.thefirsttake.app.common.user.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "users")  // 테이블 이름을 "user"로 지정
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
//    private String email;

    @Column(name = "is_guest")
    private Boolean isGuest=true;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Column(name = "session_id")
    private String sessionId;
}
