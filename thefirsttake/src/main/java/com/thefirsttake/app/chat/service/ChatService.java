package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// ChatService (새로운 서비스 또는 기존 chatRoomService에 추가)
@Service
@RequiredArgsConstructor
public class ChatService {
    private final UserSessionService userSessionService;
    private final ChatRoomService chatRoomService;
//    private final ChatMessageService chatMessageService; // 메시지 저장 및 큐 전송 전담 서비스

    // 이 메서드가 컨트롤러에서 호출될 핵심 로직
    public Long handleChatMessageSend(Long roomId, ChatMessageRequest chatMessageRequest, String sessionId) {
        if (roomId == null) {
            // 새 채팅방 생성 로직 캡슐화
            return processNewChatAndSendMessage(sessionId, chatMessageRequest);
        } else {
            // 기존 채팅방 메시지 전송 로직 캡슐화
            return processExistingChatAndSendMessage(roomId, chatMessageRequest);
        }
    }

    private Long processNewChatAndSendMessage(String sessionId, ChatMessageRequest chatMessageRequest) {
        UserEntity userEntity = userSessionService.getOrCreateGuestUser(sessionId);
        ChatRoom chatRoom = chatRoomService.createNewChatRoom(userEntity);
        Long newRoomId = chatRoom.getId();
        // 공통 로직 호출
        processAndEnqueueMessage(userEntity, chatMessageRequest, newRoomId);
        // chatRoomService.saveUserMessage 와 sendChatQueue를 담당할 서비스
        return newRoomId;
    }

    private Long processExistingChatAndSendMessage(Long roomId, ChatMessageRequest chatMessageRequest) {
        // userEntity 조회 로직이 여기로 이동
        UserEntity userEntity = chatRoomService.getUserEntityByRoomId(roomId);
        // 공통 로직 호출
        processAndEnqueueMessage(userEntity, chatMessageRequest, roomId);
        return roomId;
    }
    private void processAndEnqueueMessage(UserEntity userEntity, ChatMessageRequest chatMessageRequest, Long roomId) {
        chatRoomService.saveUserMessage(userEntity, chatMessageRequest, roomId);
        chatRoomService.sendChatQueue(roomId, chatMessageRequest);
    }

}
