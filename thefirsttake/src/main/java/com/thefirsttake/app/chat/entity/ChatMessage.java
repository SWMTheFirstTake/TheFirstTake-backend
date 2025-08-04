package com.thefirsttake.app.chat.entity;

import com.thefirsttake.app.common.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages") // 테이블 이름은 복수형으로 'chat_messages'가 더 일반적입니다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- ChatRoom과의 N:1 관계 (메시지 여러 개 -> 하나의 채팅방) ---
    // chat_messages 테이블의 room_id 컬럼이 chat_rooms 테이블의 id를 참조합니다.
    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩: 메시지를 조회할 때 당장 채팅방 정보가 필요하지 않으면 로딩하지 않음
    @JoinColumn(name = "room_id", nullable = false) // 외래 키 컬럼명 지정, 필수 값
    private ChatRoom chatRoom;

    // --- User와의 N:1 관계 (메시지 여러 개 -> 하나의 사용자) ---
    // chat_messages 테이블의 user_id 컬럼이 users 테이블의 id를 참조합니다.
    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩
    @JoinColumn(name = "user_id", nullable = false) // 외래 키 컬럼명 지정, 필수 값
    private UserEntity user; // 세션 ID 대신 User 엔티티를 직접 참조합니다.

    // sender 필드: 메시지를 보낸 주체가 누구인지 (USER 또는 BOT)
    // user 엔티티를 통해 발신자를 알 수 있지만, "시스템 메시지"나 "봇" 메시지 등
    // 특정 주체를 구분할 필요가 있다면 유지합니다.
    // user_id와 별개로 "누가 보냈는지"의 유형을 나타내는 용도.
    @Column(nullable = false, length = 10)
    private String senderType; // 'sender' 대신 'senderType'으로 이름을 변경하여 역할 명확화 (예: "USER", "BOT", "SYSTEM")

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 사용자가 채팅할 때 업로드한 이미지 URL
     * - 사용자가 "이 옷 어때?"라고 하면서 자신이 입은 옷 사진을 업로드
     * - 사용자가 "이 스타일로 추천해줘"라고 하면서 참고할 스타일 이미지 업로드
     * - 발신자: 사용자 (USER)
     */
    @Column(name = "image_url", nullable = true, columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * AI가 추천한 상품의 이미지 URL
     * - AI가 "이 상품을 추천드립니다"라고 하면서 추천 상품의 이미지
     * - 상품 검색 API에서 반환된 상품 이미지
     * - 발신자: AI 에이전트 (STYLE, TREND, COLOR, FITTING)
     */
    @Column(name = "product_image_url", nullable = true, columnDefinition = "TEXT")
    private String productImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist // 엔티티가 영속화되기 전에 실행 (INSERT 전에)
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}