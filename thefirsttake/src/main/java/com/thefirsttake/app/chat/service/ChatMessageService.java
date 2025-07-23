//package com.thefirsttake.app.chat.service;
//
//import com.thefirsttake.app.chat.constant.ChatAgentConstants;
//import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
//import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
//import com.thefirsttake.app.chat.entity.ChatMessage;
//import com.thefirsttake.app.chat.entity.ChatRoom;
//import com.thefirsttake.app.chat.repository.ChatMessageRepository;
//import com.thefirsttake.app.common.user.entity.UserEntity;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
///**
// * 채팅 메시지 관리 전담 서비스
// * - 메시지 저장
// * - 메시지 조회
// * - 메시지 히스토리 관리
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ChatMessageService {
//    private final ChatMessageRepository chatMessageRepository;
//    private final ChatRoomManagementService chatRoomManagementService;
//
//    /**
//     * 사용자 메시지 저장
//     */
//    @Transactional
//    public Long saveUserMessage(UserEntity userEntity, ChatMessageRequest chatMessageRequest, Long roomId) {
//        ChatRoom chatRoom = chatRoomManagementService.getRoomById(roomId);
//
//        ChatMessage message = ChatMessage.builder()
//                .chatRoom(chatRoom)
//                .user(userEntity)
//                .senderType("USER")
//                .message(chatMessageRequest.getContent())
//                .build();
//
//        ChatMessage saved = chatMessageRepository.save(message);
//        return saved.getId();
//    }
//
//    /**
//     * AI 응답 메시지들 저장
//     */
//    @Transactional
//    public void saveAIResponses(UserEntity user, ChatRoom chatRoom, List<ChatAgentResponse> agentResponses) {
//        for (ChatAgentResponse agentResponse : agentResponses) {
//            // DB 저장용으로 짧은 에이전트 ID 사용
//            String dbAgentId = ChatAgentConstants.AGENT_ID_MAPPING.getOrDefault(
//                    agentResponse.getAgentId(),
//                    agentResponse.getAgentId()
//            );
//
//            ChatMessage responseMessage = ChatMessage.builder()
//                    .user(user)
//                    .chatRoom(chatRoom)
//                    .senderType(dbAgentId)
//                    .message(agentResponse.getMessage())
//                    .build();
//
//            chatMessageRepository.save(responseMessage);
//        }
//    }
//
//    /**
//     * 채팅방의 메시지 히스토리 조회
//     */
//    @Transactional(readOnly = true)
//    public List<ChatMessage> getChatHistory(Long roomId) {
//        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId);
//    }
//
//    /**
//     * 특정 사용자의 메시지 조회
//     */
//    @Transactional(readOnly = true)
//    public List<ChatMessage> getMessagesByUser(UserEntity userEntity) {
//        return chatMessageRepository.findByUser(userEntity);
//    }
//}
package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.constant.ChatAgentConstants;
import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.repository.ChatMessageRepository;
import com.thefirsttake.app.common.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 채팅 메시지 관리 전담 서비스
 * - 메시지 저장
 * - 메시지 조회
 * - 메시지 히스토리 관리
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomManagementService chatRoomManagementService;

    /**
     * 사용자 메시지 저장
     */
    @Transactional
    public Long saveUserMessage(UserEntity userEntity, ChatMessageRequest chatMessageRequest, Long roomId) {
        ChatRoom chatRoom = chatRoomManagementService.getRoomById(roomId);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .user(userEntity)
                .senderType("USER")
                .message(chatMessageRequest.getContent())
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        return saved.getId();
    }

    /**
     * 단일 AI 응답 메시지 저장
     */
    @Transactional
    public void saveAIResponse(UserEntity user, ChatRoom chatRoom, ChatAgentResponse agentResponse) {
        // DB 저장용으로 짧은 에이전트 ID 사용
        String dbAgentId = ChatAgentConstants.AGENT_ID_MAPPING.getOrDefault(
                agentResponse.getAgentId(),
                agentResponse.getAgentId()
        );

        ChatMessage responseMessage = ChatMessage.builder()
                .user(user)
                .chatRoom(chatRoom)
                .senderType(dbAgentId)
                .message(agentResponse.getMessage())
                .build();

        chatMessageRepository.save(responseMessage);
    }

    /**
     * AI 응답 메시지들 저장 (다중 - 기존 호환성 유지)
     */
    @Transactional
    public void saveAIResponses(UserEntity user, ChatRoom chatRoom, List<ChatAgentResponse> agentResponses) {
        for (ChatAgentResponse agentResponse : agentResponses) {
            saveAIResponse(user, chatRoom, agentResponse);
        }
    }

    /**
     * 채팅방의 메시지 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(Long roomId) {
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId);
    }

    /**
     * 특정 사용자의 메시지 조회
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesByUser(UserEntity userEntity) {
        return chatMessageRepository.findByUser(userEntity);
    }
}