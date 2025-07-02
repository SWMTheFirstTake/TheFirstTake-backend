package com.thefirsttake.app.chat.dto.response;

import com.thefirsttake.app.chat.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "채팅방의 간략한 상세 정보")
public class ChatRoomDto {
    @Schema(description = "채팅방 고유 ID", example = "2")
    private Long id;

    @Schema(description = "채팅방 제목", example = "기존 채팅방")
    private String title;

    @Schema(description = "채팅방 생성 시간", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
    // ✨✨✨ 이 생성자를 직접 추가해야 합니다! ✨✨✨
    public ChatRoomDto(ChatRoom chatRoom) {
        this.id = chatRoom.getId();
        this.title = chatRoom.getTitle();
        this.createdAt = chatRoom.getCreatedAt();
        // UserEntity 필드는 DTO에 포함시키지 않는 것이 일반적입니다.
        // 만약 필요하다면 UserEntity도 UserDto로 변환하여 여기에 할당해야 합니다.
    }
}
