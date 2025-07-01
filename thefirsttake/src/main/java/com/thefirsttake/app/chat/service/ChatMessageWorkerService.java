package com.thefirsttake.app.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefirsttake.app.chat.dto.ChatQueueItem;
import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.repository.ChatMessageRepository;
import com.thefirsttake.app.common.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageWorkerService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageProcessorService chatMessageProcessorService;
    private final ChatRoomService chatRoomService;
//    @Scheduled(fixedDelay = 5000)
    public List<String> processChatQueue(Long roomId) {
        String json = redisTemplate.opsForList().leftPop("chat_queue:"+roomId);
        if (json != null) {
            try {
                ChatQueueItem item = objectMapper.readValue(json, ChatQueueItem.class);
                UserEntity userEntity=chatRoomService.getUserEntityByRoomId(roomId);
                ChatRoom chatRoom=chatRoomService.getRoomById(roomId);
                // 예시: 응답 생성 후 DB 저장
//                String answer = "이건 예시 응답입니다: " + item.getMessage();
                List<String> answers=chatMessageProcessorService.generateCurationResponse(item.getMessage(), item.getRoomId());

                for (int i = 0; i < answers.size(); ++i) {
                    String answer = answers.get(i);
                    String sender = "BOT" + (i + 1);  // BOT1, BOT2, ...

                    ChatMessage responseMessage = ChatMessage.builder()
                            .user(userEntity)
                            .chatRoom(chatRoom)
                            .senderType(sender)
                            .message(answer)
                            .build();

                    chatMessageRepository.save(responseMessage);
                }

//                chatMessageRepository.save(responseMessage);
                log.info("✅ 처리 완료: {}", item.getMessage());
                return answers;
            } catch (Exception e) {
                log.error("❌ 큐 처리 실패: {}", json, e);
            }
        }
        return null;
    }
}
