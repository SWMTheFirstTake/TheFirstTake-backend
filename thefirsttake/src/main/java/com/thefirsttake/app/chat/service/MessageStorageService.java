package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스트림 처리 중 메시지 저장을 담당하는 서비스
 * - 사용자 메시지 저장
 * - AI 응답 저장
 * - DB 트랜잭션 관리
 */
@Service
@Slf4j
public class MessageStorageService {
    
    private final ChatMessageService chatMessageService;
    private final UserSessionService userSessionService;
    private final ChatRoomManagementService chatRoomManagementService;
    
    public MessageStorageService(ChatMessageService chatMessageService,
                               UserSessionService userSessionService,
                               ChatRoomManagementService chatRoomManagementService) {
        this.chatMessageService = chatMessageService;
        this.userSessionService = userSessionService;
        this.chatRoomManagementService = chatRoomManagementService;
    }
    
    // 캐시를 위한 Map (요청 단위로 사용)
    private final Map<String, UserEntity> userEntityCache = new ConcurrentHashMap<>();
    private final Map<String, ChatRoom> chatRoomCache = new ConcurrentHashMap<>();
    private final Map<String, String> userMessageCache = new ConcurrentHashMap<>();
    private final Map<String, List<ChatAgentResponse>> aiResponseCache = new ConcurrentHashMap<>();
    
    /**
     * 사용자 메시지를 캐시에 임시 저장 (배치 저장을 위해)
     * @param sessionId 세션 ID
     * @param userInput 사용자 입력 메시지
     * @param roomId 방 ID
     */
    public void saveUserMessageToCache(String sessionId, String userInput, String roomId) {
        try {
            log.info("사용자 메시지 캐시 저장 시작: roomId={}, userInput='{}', sessionId='{}'", roomId, userInput, sessionId);
            
            // 캐시에서 사용자 엔티티 조회 (없으면 생성 후 캐시에 저장)
            userEntityCache.computeIfAbsent(sessionId, 
                id -> userSessionService.getOrCreateGuestUser(id));
            
            // 캐시에서 채팅방 엔티티 조회 (없으면 조회 후 캐시에 저장)
            chatRoomCache.computeIfAbsent(roomId,
                id -> chatRoomManagementService.getRoomById(Long.valueOf(id)));
            
            // 사용자 메시지를 캐시에 저장
            String cacheKey = "user_message:" + sessionId + ":" + roomId;
            userMessageCache.put(cacheKey, userInput);
            
            log.info("사용자 메시지를 캐시에 저장했습니다. roomId={}, message='{}', cacheKey={}", roomId, userInput, cacheKey);
            
        } catch (Exception e) {
            log.error("사용자 메시지 캐시 저장 실패: roomId={}, sessionId={}, error={}", 
                    roomId, sessionId, e.getMessage(), e);
            throw new RuntimeException("사용자 메시지 캐시 저장 실패", e);
        }
    }
    
    /**
     * 사용자 메시지를 데이터베이스에 저장 (기존 방식 - 호환성 유지)
     * @param sessionId 세션 ID
     * @param userInput 사용자 입력 메시지
     * @param roomId 방 ID
     */
    public void saveUserMessage(String sessionId, String userInput, String roomId) {
        try {
            log.info("사용자 메시지 저장 시작: roomId={}, userInput='{}', sessionId='{}'", roomId, userInput, sessionId);
            
            // 캐시에서 사용자 엔티티 조회 (없으면 생성 후 캐시에 저장)
            UserEntity userEntity = userEntityCache.computeIfAbsent(sessionId, 
                id -> userSessionService.getOrCreateGuestUser(id));
            
            // 캐시에서 채팅방 엔티티 조회 (없으면 조회 후 캐시에 저장)
            ChatRoom chatRoom = chatRoomCache.computeIfAbsent(roomId,
                id -> chatRoomManagementService.getRoomById(Long.valueOf(id)));
            
            log.info("캐시된 엔티티 사용: userEntity={}, chatRoom={}", 
                    userEntity.getId(), chatRoom.getId());
            
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
     * AI 응답을 캐시에 임시 저장 (배치 저장을 위해)
     * @param sessionId 세션 ID
     * @param agentId 에이전트 ID
     * @param message AI 응답 메시지
     * @param products 상품 정보 리스트
     * @param roomId 방 ID
     */
    public void saveAIResponseToCache(String sessionId, String agentId, String message, Object products, String roomId) {
        try {
            log.info("AI 응답 캐시 저장 시작: agent={}, roomId={}", agentId, roomId);
            
            // 캐시에서 사용자 엔티티 조회 (없으면 생성 후 캐시에 저장)
            userEntityCache.computeIfAbsent(sessionId, 
                id -> userSessionService.getOrCreateGuestUser(id));
            
            // 캐시에서 채팅방 엔티티 조회 (없으면 조회 후 캐시에 저장)
            chatRoomCache.computeIfAbsent(roomId,
                id -> chatRoomManagementService.getRoomById(Long.valueOf(id)));
            
            // ChatAgentResponse 객체 생성
            ChatAgentResponse agentResponse = new ChatAgentResponse();
            agentResponse.setAgentId(agentId);
            agentResponse.setMessage(message);
            // products는 List<ProductInfo> 타입이어야 함
            if (products instanceof List) {
                @SuppressWarnings("unchecked")
                List<com.thefirsttake.app.chat.dto.response.ProductInfo> productList = (List<com.thefirsttake.app.chat.dto.response.ProductInfo>) products;
                agentResponse.setProducts(productList);
            }
            
            // 캐시 키 생성
            String cacheKey = "ai_response:" + sessionId + ":" + roomId;
            
            // AI 응답을 캐시에 추가
            aiResponseCache.computeIfAbsent(cacheKey, k -> new java.util.ArrayList<>()).add(agentResponse);
            
            log.info("AI 응답을 캐시에 저장했습니다. agent={}, roomId={}, cacheKey={}", agentId, roomId, cacheKey);
            
        } catch (Exception e) {
            log.error("AI 응답 캐시 저장 실패: agent={}, roomId={}, error={}", 
                    agentId, roomId, e.getMessage(), e);
            throw new RuntimeException("AI 응답 캐시 저장 실패", e);
        }
    }
    
    /**
     * AI 응답을 데이터베이스에 저장 (기존 방식 - 호환성 유지)
     * @param sessionId 세션 ID
     * @param agentId 에이전트 ID
     * @param message AI 응답 메시지
     * @param products 상품 정보 리스트
     * @param roomId 방 ID
     */
    public void saveAIResponse(String sessionId, String agentId, String message, Object products, String roomId) {
        try {
            log.info("AI 응답 저장 시작: agent={}, roomId={}", agentId, roomId);
            
            // 캐시에서 사용자 엔티티 조회 (없으면 생성 후 캐시에 저장)
            UserEntity userEntity = userEntityCache.computeIfAbsent(sessionId, 
                id -> userSessionService.getOrCreateGuestUser(id));
            
            // 캐시에서 채팅방 엔티티 조회 (없으면 조회 후 캐시에 저장)
            ChatRoom chatRoom = chatRoomCache.computeIfAbsent(roomId,
                id -> chatRoomManagementService.getRoomById(Long.valueOf(id)));
            
            log.info("캐시된 엔티티 사용: userEntity={}, chatRoom={}", 
                    userEntity.getId(), chatRoom.getId());
            
            // ChatAgentResponse 객체 생성
            ChatAgentResponse agentResponse = new ChatAgentResponse();
            agentResponse.setAgentId(agentId);
            agentResponse.setMessage(message);
            // products는 List<ProductInfo> 타입이어야 함
            if (products instanceof List) {
                @SuppressWarnings("unchecked")
                List<com.thefirsttake.app.chat.dto.response.ProductInfo> productList = (List<com.thefirsttake.app.chat.dto.response.ProductInfo>) products;
                agentResponse.setProducts(productList);
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
     * 캐시된 모든 메시지를 한 번에 DB에 저장 (통합 배치 저장)
     * @param sessionId 세션 ID
     * @param roomId 방 ID
     */
    @Transactional
    public void saveAllMessagesFromCache(String sessionId, String roomId) {
        try {
            String cacheKey = "user_message:" + sessionId + ":" + roomId;
            
            // 캐시에서 엔티티 조회
            UserEntity userEntity = userEntityCache.get(sessionId);
            ChatRoom chatRoom = chatRoomCache.get(roomId);
            
            if (userEntity == null || chatRoom == null) {
                log.error("캐시된 엔티티가 없습니다. userEntity={}, chatRoom={}", userEntity, chatRoom);
                return;
            }
            
            log.info("통합 배치 저장 시작: sessionId={}, roomId={}", sessionId, roomId);
            
            // 1. 사용자 메시지 저장
            String userMessage = userMessageCache.get(cacheKey);
            if (userMessage != null) {
                ChatMessageRequest userMessageRequest = new ChatMessageRequest();
                userMessageRequest.setContent(userMessage);
                userMessageRequest.setImageUrl(null);
                chatMessageService.saveUserMessage(userEntity, userMessageRequest, Long.valueOf(roomId));
                log.info("사용자 메시지 저장 완료: message='{}'", userMessage);
            }
            
            // 2. 모든 AI 응답 저장
            String aiCacheKey = "ai_response:" + sessionId + ":" + roomId;
            List<ChatAgentResponse> responses = aiResponseCache.get(aiCacheKey);
            if (responses != null && !responses.isEmpty()) {
                for (ChatAgentResponse response : responses) {
                    chatMessageService.saveAIResponse(userEntity, chatRoom, response);
                }
                log.info("AI 응답 저장 완료: 응답 개수={}", responses.size());
            }
            
            // 캐시 정리
            userMessageCache.remove(cacheKey);
            aiResponseCache.remove(aiCacheKey);
            
            log.info("통합 배치 저장 완료: sessionId={}, roomId={}", sessionId, roomId);
            
        } catch (Exception e) {
            log.error("통합 배치 저장 실패: sessionId={}, roomId={}, error={}", 
                    sessionId, roomId, e.getMessage(), e);
            throw new RuntimeException("통합 배치 저장 실패", e);
        }
    }
    
    /**
     * 캐시된 모든 AI 응답을 한 번에 DB에 저장 (배치 저장) - 호환성 유지
     * @param sessionId 세션 ID
     * @param roomId 방 ID
     */
    @Transactional
    public void saveAllResponsesFromCache(String sessionId, String roomId) {
        try {
            String cacheKey = "ai_response:" + sessionId + ":" + roomId;
            List<ChatAgentResponse> responses = aiResponseCache.get(cacheKey);
            
            if (responses == null || responses.isEmpty()) {
                log.info("캐시된 AI 응답이 없습니다. sessionId={}, roomId={}", sessionId, roomId);
                return;
            }
            
            // 캐시에서 엔티티 조회
            UserEntity userEntity = userEntityCache.get(sessionId);
            ChatRoom chatRoom = chatRoomCache.get(roomId);
            
            if (userEntity == null || chatRoom == null) {
                log.error("캐시된 엔티티가 없습니다. userEntity={}, chatRoom={}", userEntity, chatRoom);
                return;
            }
            
            log.info("배치 저장 시작: sessionId={}, roomId={}, 응답 개수={}", sessionId, roomId, responses.size());
            
            // 모든 AI 응답을 한 번에 DB 저장
            for (ChatAgentResponse response : responses) {
                chatMessageService.saveAIResponse(userEntity, chatRoom, response);
            }
            
            // 캐시 정리
            aiResponseCache.remove(cacheKey);
            
            log.info("배치 저장 완료: sessionId={}, roomId={}, 저장된 응답 개수={}", sessionId, roomId, responses.size());
            
        } catch (Exception e) {
            log.error("배치 저장 실패: sessionId={}, roomId={}, error={}", 
                    sessionId, roomId, e.getMessage(), e);
            throw new RuntimeException("배치 저장 실패", e);
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
    
    /**
     * 캐시 정리 (요청 완료 시 호출)
     * @param sessionId 세션 ID
     * @param roomId 방 ID
     */
    public void clearCache(String sessionId, String roomId) {
        try {
            String userCacheKey = "user_message:" + sessionId + ":" + roomId;
            String aiCacheKey = "ai_response:" + sessionId + ":" + roomId;
            
            // 메시지 캐시 정리
            userMessageCache.remove(userCacheKey);
            aiResponseCache.remove(aiCacheKey);
            
            // 엔티티 캐시는 요청 단위로 유지 (다른 요청에서 재사용 가능)
            // userEntityCache.remove(sessionId);
            // chatRoomCache.remove(roomId);
            
            log.info("캐시 정리 완료: sessionId={}, roomId={}", sessionId, roomId);
            
        } catch (Exception e) {
            log.error("캐시 정리 실패: sessionId={}, roomId={}, error={}", 
                    sessionId, roomId, e.getMessage(), e);
        }
    }
}
