package com.thefirsttake.app.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefirsttake.app.chat.dto.ChatQueueItem;
import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.enums.ChatAgentType;
import com.thefirsttake.app.common.user.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 채팅 큐 처리 전담 서비스
 * - 메시지 큐에 추가
 * - 큐에서 메시지 처리
 * - 실패한 메시지 재처리
 */
@Service
@Slf4j
public class ChatQueueService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatCurationOrchestrationService chatCurationOrchestrationService;
    private final ChatRoomManagementService chatRoomManagementService;
    private final ChatMessageService chatMessageService;
    
    public ChatQueueService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
                           ObjectMapper objectMapper,
                           ChatCurationOrchestrationService chatCurationOrchestrationService,
                           ChatRoomManagementService chatRoomManagementService,
                           ChatMessageService chatMessageService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.chatCurationOrchestrationService = chatCurationOrchestrationService;
        this.chatRoomManagementService = chatRoomManagementService;
        this.chatMessageService = chatMessageService;
    }

    /**
     * 메시지를 큐에 추가 (모든 에이전트별로)
     */
    public void enqueueMessage(Long roomId, ChatMessageRequest chatMessageRequest) {
        String queueKey = "chat_queue:" + roomId;
        List<ChatAgentType> allAgents = Arrays.asList(ChatAgentType.values());

        for (ChatAgentType agent : allAgents) {
            ChatQueueItem queueItem = ChatQueueItem.builder()
                    .roomId(roomId)
                    .message(chatMessageRequest.getContent())
                    .imageUrl(chatMessageRequest.getImageUrl())  // 이미지 URL 추가
                    .agent(agent)
                    .retryCount(0)
                    .build();

            try {
                String json = objectMapper.writeValueAsString(queueItem);
                redisTemplate.opsForList().rightPush(queueKey, json);
                log.info("✅ 큐에 추가됨: roomId={}, agent={}, message='{}', imageUrl={}",
                        roomId, agent.getCode(), chatMessageRequest.getContent(), 
                        chatMessageRequest.getImageUrl() != null ? "있음" : "없음");
            } catch (JsonProcessingException e) {
                log.error("❌ 메시지 직렬화 실패: agent={}, message='{}'",
                        agent.getCode(), chatMessageRequest.getContent(), e);
                throw new RuntimeException("❌ 메시지 직렬화 실패", e);
            } catch (Exception e) {
                log.error("❌ 큐 추가 실패: agent={}, message='{}'",
                        agent.getCode(), chatMessageRequest.getContent(), e);
                throw new RuntimeException("❌ 큐 추가 실패", e);
            }
        }
    }

    /**
     * 큐에서 메시지 처리
     */
    public ChatAgentResponse processChatQueue(Long roomId) {
        String queueKey = "chat_queue:" + roomId;
        String json = redisTemplate.opsForList().leftPop(queueKey);
        if (json != null) {
            try {
                ChatQueueItem item = objectMapper.readValue(json, ChatQueueItem.class);
                UserEntity userEntity = chatRoomManagementService.getUserEntityByRoomId(roomId);
                ChatRoom chatRoom = chatRoomManagementService.getRoomById(roomId);

                // 특정 에이전트로 AI 응답 생성 (단일 에이전트)
                ChatAgentResponse agentResponse;
                
                // 이미지가 있는 경우 Vision 서비스 실행
                if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
                    log.info("🖼️ 이미지가 포함된 메시지 처리: agent={}, imageUrl={}", 
                            item.getAgent().getCode(), item.getImageUrl());
                    // TODO: Vision 서비스 구현 후 아래 메서드 호출
                    // agentResponse = chatCurationOrchestrationService.generateSingleAgentResponseWithImage(
                    //         item.getMessage(), item.getRoomId(), item.getAgent(), item.getImageUrl()
                    // );
                    
                    // 현재는 기존 메서드 사용 (Vision 서비스 구현 전까지)
                    agentResponse = chatCurationOrchestrationService.generateSingleAgentResponse(
                            item.getMessage(), item.getRoomId(), item.getAgent()
                    );
                } else {
                    log.info("📝 텍스트만 포함된 메시지 처리: agent={}", item.getAgent().getCode());
                    agentResponse = chatCurationOrchestrationService.generateSingleAgentResponse(
                            item.getMessage(), item.getRoomId(), item.getAgent()
                    );
                }

                // DB에 응답 저장 (단일 객체)
                try {
                    chatMessageService.saveAIResponse(userEntity, chatRoom, agentResponse);
                } catch (DataAccessException dbException) {
                    log.error("❌ DB에 메시지 저장 실패: agent={}, 메시지={}, 에러={}",
                            item.getAgent().getCode(), item.getMessage(), dbException.getMessage(), dbException);
                    return null;
                }

                log.info("✅ 큐 처리 완료: agent={}, message='{}'",
                        item.getAgent().getCode(), item.getMessage());
                return agentResponse;

            } catch (Exception e) {
                reEnqueueFailedMessage(queueKey, json);
                log.error("❌ 큐 처리 실패: {}", json, e);
            }
        }
        return null;
    }

    /**
     * 실패한 메시지를 큐에 다시 추가
     */
    private void reEnqueueFailedMessage(String queueKey, String failedJson) {
        try {
            ChatQueueItem item = objectMapper.readValue(failedJson, ChatQueueItem.class);
            item.setRetryCount(item.getRetryCount() + 1);

            // 재시도 횟수 제한
            if (item.getRetryCount() > 3) {
                log.error("❌ 메시지 재시도 횟수 초과, 폐기: agent={}, message='{}'",
                        item.getAgent().getCode(), item.getMessage());
                return;
            }

            String updatedJson = objectMapper.writeValueAsString(item);
            redisTemplate.opsForList().rightPush(queueKey, updatedJson);
            log.warn("⚠️ 실패한 메시지를 큐에 다시 추가 (재시도 {}회): agent={}, message='{}'",
                    item.getRetryCount(), item.getAgent().getCode(), item.getMessage());

        } catch (Exception e) {
            log.error("❌ 재전송 로직 중 오류 발생: {}", failedJson, e);
        }
    }
}