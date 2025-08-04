package com.thefirsttake.app.chat.repository;

import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.common.user.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    
    /**
     * 채팅방 ID와 시간 기준으로 메시지들을 페이징하여 조회 (무한 스크롤용)
     */
    List<ChatMessage> findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            Long chatRoomId, LocalDateTime before, Pageable pageable);
}
