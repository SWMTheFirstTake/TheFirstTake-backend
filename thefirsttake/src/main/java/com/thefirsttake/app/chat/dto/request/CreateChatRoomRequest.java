package com.thefirsttake.app.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "새 채팅방 생성 요청")
public class CreateChatRoomRequest {
    
    @Schema(description = "채팅방 제목", example = "새로운 채팅방", required = false)
    private String title;
}
