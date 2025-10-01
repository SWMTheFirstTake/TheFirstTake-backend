package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.service.UserSessionService;
import org.springframework.stereotype.Service;

/**
 * 채팅 전체 플로우를 조율하는 서비스
 * - 새 채팅방 생성 및 메시지 전송
 * - 기존 채팅방 메시지 전송
 * - 채팅 관련 비즈니스 로직 조율
 */
@Service
public class ChatOrchestrationService {
    private final UserSessionService userSessionService;
    private final ChatRoomManagementService chatRoomManagementService;
    private final ChatMessageService chatMessageService;
    private final ChatQueueService chatQueueService;
    
    public ChatOrchestrationService(UserSessionService userSessionService,
                                  ChatRoomManagementService chatRoomManagementService,
                                  ChatMessageService chatMessageService,
                                  ChatQueueService chatQueueService) {
        this.userSessionService = userSessionService;
        this.chatRoomManagementService = chatRoomManagementService;
        this.chatMessageService = chatMessageService;
        this.chatQueueService = chatQueueService;
    }

    /**
     * 채팅 메시지 전송을 처리하는 메인 메서드
     * @param roomId 채팅방 ID (null이면 새 채팅방 생성)
     * @param chatMessageRequest 메시지 요청
     * @param sessionId 세션 ID
     * @return 채팅방 ID
     */
    public Long handleChatMessageSend(Long roomId, ChatMessageRequest chatMessageRequest, String sessionId) {
        if (roomId == null) {
            return processNewChatAndSendMessage(sessionId, chatMessageRequest);
        } else {
            return processExistingChatAndSendMessage(roomId, chatMessageRequest);
        }
    }

    private Long processNewChatAndSendMessage(String sessionId, ChatMessageRequest chatMessageRequest) {
        UserEntity userEntity = userSessionService.getOrCreateGuestUser(sessionId);
        ChatRoom chatRoom = chatRoomManagementService.createNewChatRoom(userEntity);
        Long newRoomId = chatRoom.getId();
        
        processAndEnqueueMessage(userEntity, chatMessageRequest, newRoomId);
        return newRoomId;
    }

    private Long processExistingChatAndSendMessage(Long roomId, ChatMessageRequest chatMessageRequest) {
        UserEntity userEntity = chatRoomManagementService.getUserEntityByRoomId(roomId);
        processAndEnqueueMessage(userEntity, chatMessageRequest, roomId);
        return roomId;
    }

    private void processAndEnqueueMessage(UserEntity userEntity, ChatMessageRequest chatMessageRequest, Long roomId) {
        chatMessageService.saveUserMessage(userEntity, chatMessageRequest, roomId);
        chatQueueService.enqueueMessage(roomId, chatMessageRequest);
    }
} 