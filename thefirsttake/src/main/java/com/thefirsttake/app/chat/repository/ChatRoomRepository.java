package com.thefirsttake.app.chat.repository;

import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.common.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findFirstByUserOrderByIdAsc(UserEntity userEntity);
}
