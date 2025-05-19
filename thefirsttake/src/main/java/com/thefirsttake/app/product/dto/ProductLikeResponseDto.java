package com.thefirsttake.app.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductLikeResponseDto {
    // 좋아요 상태 응답용
    private String productId;         // 상품 ID
    private Boolean isLiked;          // true: 좋아요, false: 취소
    private Long likeCount;           // 해당 상품의 총 좋아요 수
}
