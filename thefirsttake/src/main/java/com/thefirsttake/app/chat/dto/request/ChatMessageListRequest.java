package com.thefirsttake.app.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "채팅 메시지 목록 조회 요청")
public class ChatMessageListRequest {
    
    @Schema(description = "한 번에 가져올 메시지 개수 (기본값: 5, 최대: 50)", example = "5")
    private Integer limit = 5;
    
    @Schema(description = "이 시간 이전의 메시지들을 조회 (ISO 8601 형식)", 
            example = "2024-01-15T10:00:00Z")
    private LocalDateTime before;
    
    @Schema(description = "채팅방 ID", example = "1")
    private Long roomId;
} 