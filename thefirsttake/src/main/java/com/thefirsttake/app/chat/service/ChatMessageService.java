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
import com.thefirsttake.app.chat.dto.response.ChatMessageListResponse;
import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.repository.ChatMessageRepository;
import com.thefirsttake.app.common.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
     * - 사용자가 업로드한 이미지(imageUrl)를 포함하여 저장
     * - productImageUrl은 사용자 메시지에는 없으므로 null
     */
    @Transactional
    public Long saveUserMessage(UserEntity userEntity, ChatMessageRequest chatMessageRequest, Long roomId) {
        ChatRoom chatRoom = chatRoomManagementService.getRoomById(roomId);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .user(userEntity)
                .senderType("USER")
                .message(chatMessageRequest.getContent())
                .imageUrl(chatMessageRequest.getImageUrl())  // 사용자가 업로드한 이미지
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        return saved.getId();
    }

    /**
     * 단일 AI 응답 메시지 저장
     * - AI가 추천한 상품 이미지(productImageUrl)를 포함하여 저장
     * - imageUrl은 AI 응답에는 없으므로 null
     */
    @Transactional
    public void saveAIResponse(UserEntity user, ChatRoom chatRoom, ChatAgentResponse agentResponse) {
        // DB 저장용으로 짧은 에이전트 ID 사용
        String dbAgentId = ChatAgentConstants.AGENT_ID_MAPPING.getOrDefault(
                agentResponse.getAgentId(),
                agentResponse.getAgentId()
        );

        // 1. AI 응답 메시지 저장
        ChatMessage responseMessage = ChatMessage.builder()
                .user(user)
                .chatRoom(chatRoom)
                .senderType(dbAgentId)
                .message(agentResponse.getMessage())
                .productImageUrl(null)  // AI 응답 메시지에는 상품 이미지 없음
                .build();

        chatMessageRepository.save(responseMessage);

        // 2. 각 상품 정보를 개별 메시지로 저장
        if (agentResponse.getProducts() != null && !agentResponse.getProducts().isEmpty()) {
            for (com.thefirsttake.app.chat.dto.response.ProductInfo product : agentResponse.getProducts()) {
                ChatMessage productMessage = ChatMessage.builder()
                        .user(user)
                        .chatRoom(chatRoom)
                        .senderType(dbAgentId + "_PRODUCT")  // 상품 이미지 메시지 구분
                        .message("추천 상품: " + product.getProductId())
                        .productImageUrl(product.getProductUrl())  // 개별 상품 이미지 URL
                        .build();

                chatMessageRepository.save(productMessage);
            }
        }
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

    /**
     * 무한 스크롤을 위한 채팅 메시지 목록 조회
     * @param roomId 채팅방 ID
     * @param limit 한 번에 가져올 메시지 개수
     * @param before 이 시간 이전의 메시지들을 조회
     * @return 채팅 메시지 목록 응답
     */
    @Transactional(readOnly = true)
    public ChatMessageListResponse getChatMessagesWithPagination(Long roomId, Integer limit, LocalDateTime before) {
        // limit 검증 (최대 50개로 제한)
        int actualLimit = Math.min(limit != null ? limit : 5, 50);
        
        // before가 null이면 현재 시간으로 설정
        LocalDateTime cursorTime = before != null ? before : LocalDateTime.now();
        
        // 메시지 조회 (최신순으로 정렬)
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                roomId, cursorTime, PageRequest.of(0, actualLimit + 1)); // 다음 페이지 확인을 위해 +1
        
        boolean hasMore = messages.size() > actualLimit;
        if (hasMore) {
            messages = messages.subList(0, actualLimit); // 마지막 하나 제거
        }
        
        // 시간순으로 정렬 (오래된 순)
        messages.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));
        
        // 다음 커서 계산
        LocalDateTime nextCursor = null;
        if (!messages.isEmpty()) {
            nextCursor = messages.get(0).getCreatedAt(); // 가장 오래된 메시지의 시간
        }
        
        // DTO 변환
        List<ChatMessageListResponse.ChatMessageDto> messageDtos = messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return ChatMessageListResponse.builder()
                .messages(messageDtos)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .count(messageDtos.size())
                .build();
    }
    
    /**
     * ChatMessage 엔티티를 DTO로 변환
     * - imageUrl: 사용자가 업로드한 이미지 (USER 메시지에만 존재)
     * - productImageUrl: AI가 추천한 상품 이미지 (AI 에이전트 메시지에만 존재)
     */
    private ChatMessageListResponse.ChatMessageDto convertToDto(ChatMessage message) {
        // 상품 이미지 메시지인 경우 개별 URL을 리스트로 변환
        java.util.List<String> productImageUrlList = null;
        if (message.getSenderType().endsWith("_PRODUCT") && message.getProductImageUrl() != null) {
            productImageUrlList = java.util.List.of(message.getProductImageUrl());
        }
        
        return ChatMessageListResponse.ChatMessageDto.builder()
                .id(message.getId())
                .content(message.getMessage())
                .imageUrl(message.getImageUrl())  // 사용자 업로드 이미지
                .messageType(message.getSenderType())
                .createdAt(message.getCreatedAt())
                .agentType(message.getSenderType().equals("USER") ? null : message.getSenderType())
                .agentName(message.getSenderType().equals("USER") ? null : 
                        ChatAgentConstants.DB_AGENT_NAME_MAPPING.getOrDefault(message.getSenderType(), message.getSenderType()))
                .productImageUrl(productImageUrlList)  // AI 추천 상품 이미지
                .build();
    }
}