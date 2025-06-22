package com.thefirsttake.app.chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatQueueItem {
    private String sessionId;
    private String message;
}
