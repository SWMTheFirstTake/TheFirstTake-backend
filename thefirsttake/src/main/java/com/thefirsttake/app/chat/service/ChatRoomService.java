package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.repository.ChatMessageRepository;
import com.thefirsttake.app.chat.repository.ChatRoomRepository;
import com.thefirsttake.app.common.user.entity.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    public ChatRoom getOrCreateChatRoom(UserEntity userEntity) {
        return chatRoomRepository.findFirstByUserOrderByIdAsc(userEntity)
                .orElseGet(() -> {
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setUser(userEntity);
                    newRoom.setTitle("기본 채팅방"); // 또는 UUID, 생성시점 등
                    newRoom.setCreatedAt(LocalDateTime.now());
                    return chatRoomRepository.save(newRoom);
                });
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
}
