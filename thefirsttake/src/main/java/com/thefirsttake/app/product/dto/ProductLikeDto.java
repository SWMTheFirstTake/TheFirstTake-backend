package com.thefirsttake.app.product.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductLikeDto {
    // Request 및 Response용
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                  // 좋아요 ID
    private String productId;         // 상품 ID
    private Long userId;              // 로그인 사용자 ID (nullable)
    private String sessionId;         // 비로그인 사용자 세션 ID (nullable)
    private LocalDateTime createdAt;  // 좋아요/취소 시각
    private Boolean isLiked;          // true: 좋아요, false: 취소
}
