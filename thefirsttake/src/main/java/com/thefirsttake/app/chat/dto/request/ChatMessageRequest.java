package com.thefirsttake.app.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequest {
    private String content;
    @JsonProperty("image_url")  // snake_case로 변경
    private String imageUrl;
}
