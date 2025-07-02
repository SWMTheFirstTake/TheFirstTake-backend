package com.thefirsttake.app.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅 세션 시작 시 반환되는 데이터")
public class ChatSessionStartResponse {

    @Schema(description = "새로 생성된 채팅방의 ID", example = "3")
    private Long newRoomId; // JSON Key: new_room_id (Spring Boot default)

    @Schema(description = "사용자의 모든 채팅방의 간략한 정보 목록", implementation = ChatRoomDto.class)
    // List<Long> 대신 List<ChatRoomDto>를 반환하는 경우를 가정했습니다.
    // 만약 여전히 List<Long>을 원하시면 allRoomIds로 바꾸고 implementation 제거
    private List<ChatRoomDto> allRooms; // JSON Key: all_rooms (Spring Boot default)
}
