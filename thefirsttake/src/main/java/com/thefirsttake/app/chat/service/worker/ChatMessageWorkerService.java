package com.thefirsttake.app.chat.service.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefirsttake.app.chat.dto.ChatQueueItem;
import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.chat.repository.ChatMessageRepository;
import com.thefirsttake.app.chat.service.ChatMessageProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageWorkerService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageProcessorService chatMessageProcessorService;
//    @Scheduled(fixedDelay = 5000)
    public String processChatQueue(String sessionId) {
        String json = redisTemplate.opsForList().leftPop("chat_queue:"+sessionId);
        if (json != null) {
            try {
                ChatQueueItem item = objectMapper.readValue(json, ChatQueueItem.class);

                // 예시: 응답 생성 후 DB 저장
//                String answer = "이건 예시 응답입니다: " + item.getMessage();
                String answer=chatMessageProcessorService.generateCurationResponse(item.getMessage(), item.getSessionId());

                ChatMessage responseMessage = ChatMessage.builder()
                        .sessionId(item.getSessionId())
                        .sender("BOT")
                        .message(answer)
                        .build();

                chatMessageRepository.save(responseMessage);
                log.info("✅ 처리 완료: {}", item.getMessage());
                return answer;
            } catch (Exception e) {
                log.error("❌ 큐 처리 실패: {}", json, e);
            }
        }
        return json;
    }
}
