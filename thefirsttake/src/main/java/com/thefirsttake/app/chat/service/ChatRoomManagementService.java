package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.response.ChatRoomDto;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.repository.ChatRoomRepository;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.service.UserSessionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅방 관리 전담 서비스
 * - 채팅방 CRUD 작업
 * - 채팅방 조회 및 생성
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomManagementService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserSessionService userSessionService;

    /**
     * 사용자의 모든 채팅방을 DTO 형태로 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getAllChatRoomsForUser(String sessionId) {
        UserEntity userEntity = userSessionService.getUser(sessionId);
        List<ChatRoom> chatRooms = chatRoomRepository.findByUser(userEntity);
        
        return chatRooms.stream()
                .map(ChatRoomDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 모든 채팅방 엔티티 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> getChatRooms(UserEntity userEntity) {
        return chatRoomRepository.findByUser(userEntity);
    }

    /**
     * 새 채팅방 생성
     */
    @Transactional
    public ChatRoom createNewChatRoom(UserEntity userEntity) {
        ChatRoom newRoom = new ChatRoom();
        newRoom.setUser(userEntity);
        newRoom.setTitle("새로운 채팅방");
        newRoom.setCreatedAt(LocalDateTime.now());
        
        return chatRoomRepository.save(newRoom);
    }

    /**
     * 세션 ID로 새 채팅방 생성
     */
    @Transactional
    public Long createChatRoom(String sessionId) {
        try {
            UserEntity userEntity = userSessionService.getOrCreateGuestUser(sessionId);
            ChatRoom chatRoom = createNewChatRoom(userEntity);
            return chatRoom.getId();
        } catch (Exception e) {
            log.error("❌ 채팅방 생성 중 오류가 발생했습니다: {}", e.getMessage(), e);
            throw new RuntimeException("채팅방 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * ID로 채팅방 조회
     */
    @Transactional(readOnly = true)
    public ChatRoom getRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("ChatRoom not found with ID: " + roomId));
    }

    /**
     * 채팅방 ID로 사용자 엔티티 조회
     */
    @Transactional(readOnly = true)
    public UserEntity getUserEntityByRoomId(Long roomId) {
        ChatRoom chatRoom = getRoomById(roomId);
        return chatRoom.getUser();
    }

    /**
     * ChatRoom 엔티티를 DTO로 변환
     */
    public List<ChatRoomDto> getChatRoomNumbers(List<ChatRoom> chatRooms) {
        return chatRooms.stream()
                .map(ChatRoomDto::new)
                .collect(Collectors.toList());
    }
} 