package com.thefirsttake.app.common.user.service;

import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSessionService {
    private final UserEntityRepository userEntityRepository;
    public UserEntity getUser(String sessionId){
        // findBySessionId는 Optional<UserEntity>를 반환합니다.
        // Optional 안에 UserEntity가 있으면 그 UserEntity를 반환하고,
        // 없으면 null을 반환합니다.
        return userEntityRepository.findBySessionId(sessionId).orElse(null);
    }
    public UserEntity getOrCreateGuestUser(String sessionId){
        return userEntityRepository.findBySessionId(sessionId)
                .orElseGet(()->{
                   UserEntity newUserEntity=new UserEntity();
                   newUserEntity.setSessionId(sessionId);
                   newUserEntity.setIsGuest(true);
                   return userEntityRepository.save(newUserEntity);
                });
    }
}
