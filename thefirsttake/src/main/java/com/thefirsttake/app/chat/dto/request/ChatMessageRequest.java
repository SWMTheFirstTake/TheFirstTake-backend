package com.thefirsttake.app.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequest {
    private String content;
    @JsonProperty("imageUrl")  // JSON 필드명 명시
    private String imageUrl;
}
