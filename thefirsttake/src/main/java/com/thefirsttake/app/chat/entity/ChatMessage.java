//package com.thefirsttake.app.chat.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@Getter
//@Setter
//@AllArgsConstructor
//@NoArgsConstructor
//@Entity
//@Table(name="chat_message")
//@Builder
//public class ChatMessage {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "session_id", nullable = false, length = 200)
//    private String sessionId;
//
//    @Column(nullable = false, length = 10)
//    private String sender; // "USER" 또는 "BOT"
//
//    @Column(nullable = false, columnDefinition = "TEXT")
//    private String message;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//    @PrePersist
//    public void prePersist() {
//        this.createdAt = LocalDateTime.now();
//    }
//}
package com.thefirsttake.app.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ManyToOne: 여러 메시지가 하나의 채팅방에 속함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false, length = 100)
    private String sessionId;

    @Column(nullable = false, length = 10)
    private String sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
