package com.thefirsttake.app.common.user.repository;

import com.thefirsttake.app.common.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findBySessionId(String sesssionId);
    Optional<UserEntity> findByKakaoUserId(String kakaoUserId);
}
