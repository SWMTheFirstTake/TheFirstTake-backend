package com.thefirsttake.app.common.user.service;

import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSessionService {
    private final UserEntityRepository userEntityRepository;
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
