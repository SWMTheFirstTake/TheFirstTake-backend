package com.thefirsttake.app.chat.dto.response;

import com.thefirsttake.app.chat.entity.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@Schema(description = "채팅 메시지 목록 조회 응답")
public class ChatMessageListResponse {
    
    @Schema(description = "채팅 메시지 목록")
    private List<ChatMessageDto> messages;
    
    @Schema(description = "더 많은 메시지가 있는지 여부")
    private Boolean hasMore;
    
    @Schema(description = "다음 페이지 조회를 위한 커서 (이 시간 이전의 메시지들을 조회)", 
            example = "2024-01-15T09:30:00Z")
    private LocalDateTime nextCursor;
    
    @Schema(description = "현재 조회된 메시지 개수")
    private Integer count;
    
    @Getter
    @Setter
    @Builder
    @Schema(description = "채팅 메시지 정보")
    public static class ChatMessageDto {
        @Schema(description = "메시지 ID")
        private Long id;
        
        @Schema(description = "메시지 내용")
        private String content;
        
        @Schema(description = "이미지 URL (있는 경우)")
        private String imageUrl;
        
        @Schema(description = "메시지 타입 (USER, AI)")
        private String messageType;
        
        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;
        
        @Schema(description = "AI 에이전트 타입 (AI 메시지인 경우)")
        private String agentType;
        
        @Schema(description = "AI 에이전트 이름 (AI 메시지인 경우)")
        private String agentName;
        
        @Schema(description = "상품 이미지 URL (AI 메시지인 경우)")
        private String productImageUrl;
    }
} 