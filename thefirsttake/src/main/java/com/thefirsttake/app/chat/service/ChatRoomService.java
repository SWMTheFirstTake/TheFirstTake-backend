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
import com.thefirsttake.app.common.user.entity.UserEntity;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
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
        // 1. 해당 유저의 모든 채팅방을 조회합니다.
        List<ChatRoom> chatRooms = chatRoomRepository.findByUser(userEntity);

        // 2. 만약 조회된 채팅방이 하나도 없다면, 새로운 기본 채팅방을 생성하고 리스트에 추가합니다.
//        if (chatRooms.isEmpty()) {
//            ChatRoom newRoom = new ChatRoom();
//            newRoom.setUser(userEntity);
//            newRoom.setTitle("기본 채팅방"); // 적절한 기본 제목 설정
//            newRoom.setCreatedAt(LocalDateTime.now()); // 생성 시간 설정
//
//            ChatRoom savedRoom = chatRoomRepository.save(newRoom); // DB에 저장
//            chatRooms.add(savedRoom); // 새로 생성된 방을 리스트에 추가
//        }

        // 3. (새로 생성된 방이 있다면 그것 포함하여) 해당 유저의 모든 채팅방 리스트를 반환합니다.
        // 만약 항상 새로운 방이 생성되더라도 기존 방들을 다시 조회해서 포함하고 싶다면
        // if (chatRooms.isEmpty()) 로직 후에 findByUser(userEntity)를 다시 호출해도 됩니다.
        // 하지만 위의 로직은 새로 만든 방을 직접 리스트에 추가하므로 굳이 다시 조회할 필요는 없습니다.
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
        // 1. roomId를 사용하여 ChatRoom 엔티티를 조회합니다.
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
}
