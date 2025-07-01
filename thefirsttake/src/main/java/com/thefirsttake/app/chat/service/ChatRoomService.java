package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.repository.ChatRoomRepository;
import com.thefirsttake.app.common.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
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
//    public ChatRoom getRoomById(Long roomId){
//        return chatRoomRepository.findById(roomId)
//                .orElseThrow(() -> new RuntimeException("채팅방이 존재하지 않습니다."));
//    }
    public UserEntity getUserEntityByRoomId(Long roomId){
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방이 존재하지 않습니다."));

        return chatRoom.getUser();
    }
}
