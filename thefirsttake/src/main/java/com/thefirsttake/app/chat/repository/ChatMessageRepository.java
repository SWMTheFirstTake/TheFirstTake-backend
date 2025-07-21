package com.thefirsttake.app.chat.repository;

import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.common.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 채팅방 ID로 메시지들을 생성 시간 순으로 조회
     */
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
    
    /**
     * 사용자로 메시지들을 조회
     */
    List<ChatMessage> findByUser(UserEntity user);
}
