package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.entity.ChatMessage;
import com.thefirsttake.app.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//@Service
//@RequiredArgsConstructor
//public class ChatMessageSaveService {
//    private final ChatMessageRepository chatMessageRepository;
//    public Long saveUserMessage(String sessionId, ChatMessageRequest chatMessageRequest) {
//        String userInput=chatMessageRequest.getContent();
//        ChatMessage message = ChatMessage.builder()
//                .sessionId(sessionId)
//                .sender("USER")
//                .message(userInput)
//                .build();
//
//        ChatMessage saved = chatMessageRepository.save(message);
//        return saved.getId(); // 저장된 메시지의 ID 반환
//    }
//}
