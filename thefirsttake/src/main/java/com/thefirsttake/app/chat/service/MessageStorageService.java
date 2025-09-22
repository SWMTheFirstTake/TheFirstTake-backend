package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 스트림 처리 중 메시지 저장을 담당하는 서비스
 * - 사용자 메시지 저장
 * - AI 응답 저장
 * - DB 트랜잭션 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageStorageService {
    
    private final ChatMessageService chatMessageService;
    private final UserSessionService userSessionService;
    private final ChatRoomManagementService chatRoomManagementService;
    
    /**
     * 사용자 메시지를 데이터베이스에 저장
     * @param sessionId 세션 ID
     * @param userInput 사용자 입력 메시지
     * @param roomId 방 ID
     */
    public void saveUserMessage(String sessionId, String userInput, String roomId) {
        try {
            log.info("사용자 메시지 저장 시작: roomId={}, userInput='{}', sessionId='{}'", roomId, userInput, sessionId);
            
            // 세션 ID 기반으로 사용자 생성/조회
            UserEntity userEntity = userSessionService.getOrCreateGuestUser(sessionId);
            log.info("세션 기반 사용자 엔티티 조회/생성 완료: userEntity={}, userId={}", 
                    userEntity, userEntity != null ? userEntity.getId() : "null");
            
            if (userEntity == null) {
                log.error("사용자 엔티티가 null입니다. sessionId={}", sessionId);
                return;
            }
            
            // 사용자 메시지 요청 객체 생성
            ChatMessageRequest userMessageRequest = new ChatMessageRequest();
            userMessageRequest.setContent(userInput);
            userMessageRequest.setImageUrl(null); // 스트림 API에서는 이미지 없음
            
            // DB에 사용자 메시지 저장
            chatMessageService.saveUserMessage(userEntity, userMessageRequest, Long.valueOf(roomId));
            log.info("스트림 API 사용자 메시지를 데이터베이스에 저장했습니다. roomId={}, message='{}'", roomId, userInput);
            
        } catch (Exception e) {
            log.error("스트림 API 사용자 메시지 저장 실패: roomId={}, sessionId={}, error={}", 
                    roomId, sessionId, e.getMessage(), e);
            throw new RuntimeException("사용자 메시지 저장 실패", e);
        }
    }
    
    /**
     * AI 응답을 데이터베이스에 저장
     * @param sessionId 세션 ID
     * @param agentId 에이전트 ID
     * @param message AI 응답 메시지
     * @param products 상품 정보 리스트
     * @param roomId 방 ID
     */
    public void saveAIResponse(String sessionId, String agentId, String message, Object products, String roomId) {
        try {
            log.info("AI 응답 저장 시작: agent={}, roomId={}", agentId, roomId);
            
            // 사용자 엔티티 조회
            UserEntity userEntity = userSessionService.getOrCreateGuestUser(sessionId);
            if (userEntity == null) {
                log.error("사용자 엔티티가 null입니다. sessionId={}", sessionId);
                return;
            }
            
            // 채팅방 엔티티 조회
            ChatRoom chatRoom = chatRoomManagementService.getRoomById(Long.valueOf(roomId));
            if (chatRoom == null) {
                log.error("채팅방 엔티티가 null입니다. roomId={}", roomId);
                return;
            }
            
            log.info("AI 응답 저장용 엔티티 조회 완료: userEntity={}, chatRoom={}", 
                    userEntity.getId(), chatRoom.getId());
            
            // ChatAgentResponse 객체 생성
            ChatAgentResponse agentResponse = new ChatAgentResponse();
            agentResponse.setAgentId(agentId);
            agentResponse.setMessage(message);
            // products는 List<ProductInfo> 타입이어야 함
            if (products instanceof List) {
                agentResponse.setProducts((List<com.thefirsttake.app.chat.dto.response.ProductInfo>) products);
            }
            
            // DB에 AI 응답 저장
            chatMessageService.saveAIResponse(userEntity, chatRoom, agentResponse);
            log.info("스트림 완료 시 AI 응답을 데이터베이스에 저장했습니다. agent={}, roomId={}", agentId, roomId);
            
        } catch (Exception e) {
            log.error("스트림 완료 시 DB 저장 실패: agent={}, roomId={}, error={}", 
                    agentId, roomId, e.getMessage(), e);
            throw new RuntimeException("AI 응답 저장 실패", e);
        }
    }
    
    /**
     * 사용자 엔티티 조회/생성
     * @param sessionId 세션 ID
     * @return 사용자 엔티티
     */
    public UserEntity getUserEntity(String sessionId) {
        try {
            return userSessionService.getOrCreateGuestUser(sessionId);
        } catch (Exception e) {
            log.error("사용자 엔티티 조회/생성 실패: sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw new RuntimeException("사용자 엔티티 조회/생성 실패", e);
        }
    }
    
    /**
     * 채팅방 엔티티 조회
     * @param roomId 방 ID
     * @return 채팅방 엔티티
     */
    public ChatRoom getChatRoom(String roomId) {
        try {
            return chatRoomManagementService.getRoomById(Long.valueOf(roomId));
        } catch (Exception e) {
            log.error("채팅방 엔티티 조회 실패: roomId={}, error={}", roomId, e.getMessage(), e);
            throw new RuntimeException("채팅방 엔티티 조회 실패", e);
        }
    }
    
    /**
     * 사용자 메시지와 AI 응답을 한 번에 저장 (트랜잭션)
     * @param sessionId 세션 ID
     * @param userInput 사용자 입력 메시지
     * @param agentId 에이전트 ID
     * @param message AI 응답 메시지
     * @param products 상품 정보 리스트
     * @param roomId 방 ID
     */
    public void saveMessageAndResponse(String sessionId, String userInput, String agentId, 
                                     String message, Object products, String roomId) {
        try {
            // 사용자 메시지 저장
            saveUserMessage(sessionId, userInput, roomId);
            
            // AI 응답 저장
            saveAIResponse(sessionId, agentId, message, products, roomId);
            
            log.info("메시지와 응답 저장 완료: sessionId={}, roomId={}, agentId={}", sessionId, roomId, agentId);
            
        } catch (Exception e) {
            log.error("메시지와 응답 저장 실패: sessionId={}, roomId={}, agentId={}, error={}", 
                    sessionId, roomId, agentId, e.getMessage(), e);
            throw new RuntimeException("메시지와 응답 저장 실패", e);
        }
    }
}
