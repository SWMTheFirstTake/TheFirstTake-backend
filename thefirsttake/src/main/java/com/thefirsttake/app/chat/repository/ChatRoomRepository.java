package com.thefirsttake.app.chat.repository;

import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.common.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
//    Optional<ChatRoom> findFirstByUserOrderByIdDesc(UserEntity userEntity);
//    Optional<ChatRoom> findByUser(UserEntity userEntity);

    // 특정 UserEntity에 연결된 모든 ChatRoom을 찾아 List로 반환
    // 만약 결과가 없다면 빈 List가 반환됩니다 (null이 아님).
    List<ChatRoom> findByUser(UserEntity userEntity);
    // 특정 UserEntity에 연결된 모든 ChatRoom을 찾아 List로 반환하되,
    // createdAt 필드를 기준으로 오름차순(ASC) 정렬합니다.
//    List<ChatRoom> findByUserOrderByCreatedAtAsc(UserEntity userEntity);
//
//    // 참고: 만약 특정 유저의 가장 최근 방 (createdAt 기준 내림차순) 하나만 찾고 싶다면:
//    Optional<ChatRoom> findFirstByUserOrderByCreatedAtDesc(UserEntity userEntity);
}
