package com.thefirsttake.app.chat.dto;

import com.thefirsttake.app.chat.enums.ChatAgentType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatQueueItem {
//    private String sessionId;
    private Long roomId;
    private String message;
    private String imageUrl;  // 이미지 URL 필드 추가
    private ChatAgentType agent;
    private int retryCount;
}
