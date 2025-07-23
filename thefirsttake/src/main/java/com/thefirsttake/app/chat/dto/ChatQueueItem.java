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
    private ChatAgentType agent;
    private int retryCount;
}
