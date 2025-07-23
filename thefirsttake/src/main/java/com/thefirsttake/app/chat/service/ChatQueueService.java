//package com.thefirsttake.app.chat.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.thefirsttake.app.chat.dto.ChatQueueItem;
//import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
//import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
//import com.thefirsttake.app.chat.entity.ChatMessage;
//import com.thefirsttake.app.chat.entity.ChatRoom;
//import com.thefirsttake.app.chat.enums.ChatAgentType;
//import com.thefirsttake.app.common.user.entity.UserEntity;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.dao.DataAccessException;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * 채팅 큐 처리 전담 서비스
// * - 메시지 큐에 추가
// * - 큐에서 메시지 처리
// * - 실패한 메시지 재처리
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ChatQueueService {
//    private final RedisTemplate<String, String> redisTemplate;
//    private final ObjectMapper objectMapper;
//    private final ChatCurationOrchestrationService chatCurationOrchestrationService;
//    private final ChatRoomManagementService chatRoomManagementService;
//    private final ChatMessageService chatMessageService;
//
//    /**
//     * 메시지를 큐에 추가
//     */
//    public void enqueueMessage(Long roomId, ChatMessageRequest chatMessageRequest) {
//        String queueKey = "chat_queue:" + roomId;
//        List<ChatAgentType> allAgents = Arrays.asList(ChatAgentType.values());
//
//        for (ChatAgentType agent : allAgents) {
//            ChatQueueItem queueItem = ChatQueueItem.builder()
//                    .roomId(roomId)
//                    .message(chatMessageRequest.getContent())
//                    .agent(agent)
//                    .retryCount(0)
//                    .build();
//
//            try {
//                String json = objectMapper.writeValueAsString(queueItem);
//                redisTemplate.opsForList().rightPush(queueKey, json);
//                log.info("✅ 큐에 추가됨: roomId={}, agent={}, message='{}'",
//                        roomId, agent.getCode(), chatMessageRequest.getContent());
//            } catch (JsonProcessingException e) {
//                log.error("❌ 메시지 직렬화 실패: agent={}, message='{}'",
//                        agent.getCode(), chatMessageRequest.getContent(), e);
//                throw new RuntimeException("❌ 메시지 직렬화 실패", e);
//            } catch (Exception e) {
//                log.error("❌ 큐 추가 실패: agent={}, message='{}'",
//                        agent.getCode(), chatMessageRequest.getContent(), e);
//                throw new RuntimeException("❌ 큐 추가 실패", e);
//            }
//        }
////        ChatQueueItem queueItem = ChatQueueItem.builder()
////                .roomId(roomId)
////                .message(chatMessageRequest.getContent())
////                .retryCount(0)
////                .build();
////
////        try {
////            String json = objectMapper.writeValueAsString(queueItem);
////            redisTemplate.opsForList().rightPush("chat_queue:" + roomId, json);
////        } catch (JsonProcessingException e) {
////            throw new RuntimeException("❌ 메시지 직렬화 실패", e);
////        } catch (Exception e) {
////            throw new RuntimeException("❌ 큐 추가 실패", e);
////        }
//    }
//
//    /**
//     * 큐에서 메시지 처리
//     */
//    public List<ChatAgentResponse> processChatQueue(Long roomId) {
//        String queueKey = "chat_queue:" + roomId;
//        String json = redisTemplate.opsForList().leftPop(queueKey);
//        if (json != null) {
//            try {
//                ChatQueueItem item = objectMapper.readValue(json, ChatQueueItem.class);
//                UserEntity userEntity = chatRoomManagementService.getUserEntityByRoomId(roomId);
//                ChatRoom chatRoom = chatRoomManagementService.getRoomById(roomId);
//
//                // AI 응답 생성
//                List<ChatAgentResponse> agentResponses = chatCurationOrchestrationService.generateCurationResponse(
//                        item.getMessage(), item.getRoomId(),item.getAgent()
//                );
//
//                // DB에 응답 저장
//                try {
//                    chatMessageService.saveAIResponses(userEntity, chatRoom, agentResponses);
//                } catch (DataAccessException dbException) {
//                    log.error("❌ DB에 메시지 저장 실패: 메시지: {}, 에러: {}",
//                            item.getMessage(), dbException.getMessage(), dbException);
//                    return null;
//                }
//
//                log.info("✅ 큐 처리 완료: {}", item.getMessage());
//                return agentResponses;
//
//            } catch (Exception e) {
//                reEnqueueFailedMessage(queueKey, json);
//                log.error("❌ 큐 처리 실패: {}", json, e);
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 실패한 메시지를 큐에 다시 추가
//     */
//    private void reEnqueueFailedMessage(String queueKey, String failedJson) {
//        try {
//            ChatQueueItem item = objectMapper.readValue(failedJson, ChatQueueItem.class);
//            item.setRetryCount(item.getRetryCount() + 1);
//
//            // 재시도 횟수 제한
//            if (item.getRetryCount() > 3) {
//                log.error("❌ 메시지 재시도 횟수 초과, 폐기: agent={}, message='{}'",
//                        item.getAgent().getCode(), item.getMessage());
//                return;
//            }
//
//            String updatedJson = objectMapper.writeValueAsString(item);
//            redisTemplate.opsForList().rightPush(queueKey, updatedJson);
//            log.warn("⚠️ 실패한 메시지를 큐에 다시 추가 (재시도 {}회): agent={}, message='{}'",
//                    item.getRetryCount(), item.getAgent().getCode(), item.getMessage());
//
//        } catch (Exception e) {
//            log.error("❌ 재전송 로직 중 오류 발생: {}", failedJson, e);
//        }
////        try {
////            ChatQueueItem item = objectMapper.readValue(failedJson, ChatQueueItem.class);
////            item.setRetryCount(item.getRetryCount() + 1);
////
////            // 재시도 횟수 제한
////            if (item.getRetryCount() > 3) {
////                log.error("❌ 메시지 재시도 횟수 초과, 폐기: {}", failedJson);
////                return;
////            }
////
////            String updatedJson = objectMapper.writeValueAsString(item);
////            redisTemplate.opsForList().rightPush(queueKey, updatedJson);
////            log.warn("⚠️ 실패한 메시지를 큐에 다시 추가 (재시도): {}", updatedJson);
////
////        } catch (Exception e) {
////            log.error("❌ 재전송 로직 중 오류 발생: {}", failedJson, e);
////        }
//    }
//}

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class ChatQueueService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatCurationOrchestrationService chatCurationOrchestrationService;
    private final ChatRoomManagementService chatRoomManagementService;
    private final ChatMessageService chatMessageService;

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
                    .agent(agent)
                    .retryCount(0)
                    .build();

            try {
                String json = objectMapper.writeValueAsString(queueItem);
                redisTemplate.opsForList().rightPush(queueKey, json);
                log.info("✅ 큐에 추가됨: roomId={}, agent={}, message='{}'",
                        roomId, agent.getCode(), chatMessageRequest.getContent());
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
                ChatAgentResponse agentResponse = chatCurationOrchestrationService.generateSingleAgentResponse(
                        item.getMessage(), item.getRoomId(), item.getAgent()
                );

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