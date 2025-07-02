package com.thefirsttake.app.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "사용자 채팅방 목록 조회 응답 데이터")
public class ChatSessionHistoryResponse {
    @Schema(description = "사용자의 모든 채팅방 목록")
    private List<ChatRoomDto> all_rooms;

    public ChatSessionHistoryResponse(List<ChatRoomDto> all_rooms) {
        this.all_rooms = all_rooms;
    }

    public List<ChatRoomDto> getAll_rooms() {
        return all_rooms;
    }

    public void setAll_rooms(List<ChatRoomDto> all_rooms) {
        this.all_rooms = all_rooms;
    }
}
