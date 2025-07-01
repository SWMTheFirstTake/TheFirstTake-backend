//package com.thefirsttake.app.chat.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.thefirsttake.app.chat.dto.ChatQueueItem;
//import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
//import com.thefirsttake.app.common.response.CommonResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.RedisConnectionFailureException;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class SendMessageWorkerService {
//    // 메시지 처리
//    private final RedisTemplate<String, String> redisTemplate; // Redis 주입
//    private final ObjectMapper objectMapper;
//    public void sendChatQueue(String sessionId, ChatMessageRequest chatMessageRequest){
//        ChatQueueItem queueItem = ChatQueueItem.builder()
//                .sessionId(sessionId)
//                .message(chatMessageRequest.getContent())
//                .build();
//        try {
//            String json = objectMapper.writeValueAsString(queueItem);
//            redisTemplate.opsForList().rightPush("chat_queue:"+sessionId, json);
//        } catch (RedisConnectionFailureException e) {
//            throw new RuntimeException("❌ Redis 연결 실패", e);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException("❌ 직렬화 실패", e);
//        } catch (Exception e) {
//            throw new RuntimeException("❌ 기타 Redis 작업 실패", e);
//        }
//    }
//}
