package com.thefirsttake.app.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefirsttake.app.chat.dto.ChatQueueItem;
import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.repository.ChatMessageRepository;
import com.thefirsttake.app.common.user.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatQueueConsumerService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatCurationGeneratorService chatCurationGeneratorService;
    private final ChatRoomService chatRoomService;
    public List<String> processChatQueue(Long roomId) {
        // chat_queue:[roomId]라는 키에 해당하는 리스트의 가장 왼쪽(맨 앞)에 있는 요소를 하나만 꺼내오는 작업을 수행
        String queueKey = "chat_queue:" + roomId;
        String json = redisTemplate.opsForList().leftPop(queueKey);
        if (json != null) {
            try {
                ChatQueueItem item = objectMapper.readValue(json, ChatQueueItem.class);
                UserEntity userEntity=chatRoomService.getUserEntityByRoomId(roomId);
                ChatRoom chatRoom=chatRoomService.getRoomById(roomId);
                // 예시: 응답 생성 후 DB 저장
                List<String> answers= chatCurationGeneratorService.generateCurationResponse(item.getMessage(), item.getRoomId());
                try {
                    // DB 저장 메서드 호출
                    saveCurationResponses(userEntity, chatRoom, answers);
                } catch (DataAccessException dbException) {
                    // DB 접근 관련 예외만 여기서 처리 (예: 네트워크 문제, 제약 조건 위반 등)
                    log.error("❌ POSTGRSQL DB에 메시지 저장 실패: 메시지: {}, 에러: {}", item.getMessage(), dbException.getMessage(), dbException);
                    return null; // 또는 다른 오류 처리
                }
                log.info("✅ 처리 완료: {}", item.getMessage());
                return answers;
            } catch (Exception e) {
                reEnqueueFailedMessage(queueKey,json);
                log.error("❌ 큐 처리 실패: {}", json, e);
            }
        }
        return null;
    }
    // ChatMessageWorkerService 내부에 private 메서드로 분리
    @Transactional
    private void saveCurationResponses(UserEntity user, ChatRoom chatRoom, List<String> answers) {
        for (int i = 0; i < answers.size(); ++i) {
            String answer = answers.get(i);
            String sender = "BOT" + (i + 1);

            ChatMessage responseMessage = ChatMessage.builder()
                    .user(user)
                    .chatRoom(chatRoom)
                    .senderType(sender)
                    .message(answer)
                    .build();

            chatMessageRepository.save(responseMessage);
        }
    }
    // 실패한 메시지를 큐에 다시 넣는 메서드
    private void reEnqueueFailedMessage(String queueKey, String failedJson) {
        // 재시도 횟수를 고려한 재전송
        try {
            ChatQueueItem item = objectMapper.readValue(failedJson, ChatQueueItem.class);
            // 재시도 횟수 증가
            item.setRetryCount(item.getRetryCount() + 1);

            // 특정 재시도 횟수를 초과하면 Dead Letter Queue (DLQ)로 보내거나 로깅 후 폐기
             if (item.getRetryCount() > 3) {
                 log.error("❌ 메시지 재시도 횟수 초과, DLQ로 이동 또는 폐기: {}", failedJson);
                 // deadLetterQueue.send(failedJson); // DLQ로 보내는 로직
                 return;
             }

            // 다시 JSON으로 변환하여 큐에 넣기
            String updatedJson = objectMapper.writeValueAsString(item);
            redisTemplate.opsForList().rightPush(queueKey, updatedJson); // 큐의 뒤에 다시 넣습니다.
            log.warn("⚠️ 실패한 메시지를 큐에 다시 넣었습니다 (재시도): {}", updatedJson);

        } catch (Exception e) {
            log.error("❌ 재전송 로직 중 오류 발생: {}", failedJson, e);
            // 이 경우, 메시지가 유실될 수 있으므로 별도의 모니터링 필요
        }
    }
}
