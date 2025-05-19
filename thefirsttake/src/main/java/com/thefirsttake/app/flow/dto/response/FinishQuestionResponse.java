package com.thefirsttake.app.flow.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FinishQuestionResponse {
    private String productId;
    private String productDetailImages;  // 상품 이미지 URL
    private String productDescription;
    private String productLink;
    private long likeCount;  // 좋아요 수
}
