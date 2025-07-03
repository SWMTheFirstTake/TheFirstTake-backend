package com.thefirsttake.app.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefirsttake.app.chat.dto.ChatQueueItem;
import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.dto.response.ChatRoomDto;
import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.repository.ChatMessageRepository;
import com.thefirsttake.app.chat.repository.ChatRoomRepository;
import com.thefirsttake.app.common.response.CommonResponse;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.service.UserSessionService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserSessionService userSessionService;
    private final RedisTemplate<String, String> redisTemplate; // Redis 주입
    private final ObjectMapper objectMapper;
    @Value("${spring.redis.host}")
    private String redisHost;
    @Value("${spring.redis.port}")
    private String redisPort;

    @PostConstruct
    public void printRedisHost() {
        System.out.println("Connecting to Redis at " + redisHost + ":" + redisPort);
    }

    public List<ChatRoomDto> getChatRoomNumbers(List<ChatRoom> chatRooms) {
        // Stream API를 사용하여 ChatRoom 객체들을 Long 타입의 id로 매핑

        return chatRooms.stream()
                .map(ChatRoomDto::new) // ChatRoom -> ChatRoomDto 변환
                .collect(Collectors.toList());

    }
    @Transactional
    public List<ChatRoom> getChatRooms(UserEntity userEntity) {
        // 해당 유저의 모든 채팅방을 조회합니다.
        List<ChatRoom> chatRooms = chatRoomRepository.findByUser(userEntity);
        return chatRooms;
    }

    @Transactional
    public ChatRoom createNewChatRoom(UserEntity userEntity){
        // 새 chatRoom객체를 생성함. 새로운 room의 sestUser는 userEntity
        ChatRoom newRoom = new ChatRoom();
        newRoom.setUser(userEntity); // ChatRoom 엔티티에 User 필드가 있다고 가정
        newRoom.setTitle("새로운 채팅방"); // 새로운 방임을 나타내는 제목 (또는 클라이언트에서 받을 수도 있음)
        newRoom.setCreatedAt(LocalDateTime.now()); // 생성 시간 설정

        // 데이터베이스에 저장
        return chatRoomRepository.save(newRoom);
    }
    public ChatRoom getRoomById(Long roomId){
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found with ID: " + roomId));
    }
    public UserEntity getUserEntityByRoomId(Long roomId){
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방이 존재하지 않습니다."));

        return chatRoom.getUser();
    }
    public Long saveUserMessage(UserEntity userEntity, ChatMessageRequest chatMessageRequest, Long roomId) {
        // roomId를 사용하여 ChatRoom 엔티티를 조회합니다.
        // 해당 ID의 채팅방이 존재하지 않으면 예외를 발생시킵니다.
        ChatRoom chatRoom = getRoomById(roomId);

        String userInput=chatMessageRequest.getContent();
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .user(userEntity)
                .senderType("USER")
                .message(userInput)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        return saved.getId(); // 저장된 메시지의 ID 반환
    }
    public void sendChatQueue(Long roomId, ChatMessageRequest chatMessageRequest){
        ChatQueueItem queueItem = ChatQueueItem.builder()
                .roomId(roomId)
                .message(chatMessageRequest.getContent())
                .retryCount(0)
                .build();
        try {
            String json = objectMapper.writeValueAsString(queueItem);
            redisTemplate.opsForList().rightPush("chat_queue:"+roomId, json);
        } catch (RedisConnectionFailureException e) {
            throw new RuntimeException("❌ Redis 연결 실패", e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("❌ 직렬화 실패", e);
        } catch (Exception e) {
            throw new RuntimeException("❌ 기타 Redis 작업 실패", e);
        }
    }
    public Long createChatRoom(String sessionId) {
        try {
            // 1. 유저 확인/생성
            UserEntity userEntity = userSessionService.getOrCreateGuestUser(sessionId);

            // 2. chatRoom 테이블에 해당 유저의 이름을 가지는 채팅방 하나 더 생성
            ChatRoom chatRoom = createNewChatRoom(userEntity); // chatRoomService의 메서드 사용

            // 3. 생성된 새로운 채팅방의 아이디 값을 반환
            return chatRoom.getId();

        } catch (Exception e) {
            // 원본 예외와 함께 오류 메시지를 로그로 남깁니다.
            log.error("❌ 채팅방 생성 중 오류가 발생했습니다: {}", e.getMessage(), e);
            // 커스텀 예외를 던져서 호출하는 쪽(컨트롤러 등)에서 처리하도록 합니다.
            throw new RuntimeException("채팅방 생성 중 오류가 발생했습니다.", e);
        }
    }
    /**
     * 사용자 세션 ID를 기반으로 해당 유저의 모든 채팅방 DTO 목록을 조회합니다.
     * 이 메서드가 컨트롤러에서 직접 호출될 핵심 비즈니스 로직입니다.
     */
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 성능 최적화
    public List<ChatRoomDto> getAllChatRoomsForUser(String sessionId) {
        // 1. 유저 확인
        // UserSessionService에서 사용자 정보를 가져옵니다.
        // 만약 유저가 없으면 예외를 던지도록 getUser 메서드를 수정하거나,
        // Optional<UserEntity>를 반환하도록 하여 여기서 처리할 수 있습니다.
        UserEntity userEntity = userSessionService.getUser(sessionId); // getUser가 Optional을 반환한다면 .orElseThrow(...)

        // 2. 해당 유저의 모든 ChatRoom 엔티티 목록 가져오기
        List<ChatRoom> chatRooms = chatRoomRepository.findByUser(userEntity);

        // 3. ChatRoom 엔티티 목록을 ChatRoomDto 목록으로 변환
        return chatRooms.stream()
                .map(ChatRoomDto::new) // ChatRoom -> ChatRoomDto 변환
                .collect(Collectors.toList());
    }
}
