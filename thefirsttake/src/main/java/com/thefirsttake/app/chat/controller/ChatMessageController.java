package com.thefirsttake.app.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.service.ChatMessageProcessorService;
import com.thefirsttake.app.chat.service.ChatMessageSaveService;
import com.thefirsttake.app.chat.service.SendMessageWorkerService;
import com.thefirsttake.app.chat.service.worker.ChatMessageWorkerService;
import com.thefirsttake.app.common.response.ApiResponse;
import com.thefirsttake.app.common.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatMessageProcessorService chatMessageProcessorService;
    private final ChatMessageSaveService chatMessageSaveService;
    private final SendMessageWorkerService sendMessageWorkerService;
    private final ChatMessageWorkerService chatMessageWorkerService;

    @PostMapping("/send")
    public CommonResponse sendChatMessage(@RequestBody ChatMessageRequest chatMessageRequest, HttpServletRequest httpRequest){
        HttpSession session = httpRequest.getSession(true);

        if (session == null) {
            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }

        String sessionId = session.getId();
        try {
            // 1. PostgreSQL 저장
            Long savedId = chatMessageSaveService.saveUserMessage(sessionId, chatMessageRequest);

            // 2. chat_request:sessionId에 유저의 request 넣기

            // 3. Redis 워커 큐에 푸시
            sendMessageWorkerService.sendChatQueue(sessionId, chatMessageRequest);

            return CommonResponse.success(savedId);

        } catch (Exception e) {
            e.printStackTrace(); // 로그로 남기기 (혹은 log.error(...))

            return CommonResponse.fail("메시지 전송 중 오류가 발생했습니다: " + e.getMessage());
        }

    }
    @GetMapping("/receive")
    public CommonResponse receiveChatMessage(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }

        String sessionId = session.getId();
        String responseMessage=chatMessageWorkerService.processChatQueue(sessionId);
//        String redisKey = "chat_response:" + sessionId;
//
//        // Redis에서 메시지를 pop (꺼내고 제거)
//        String responseMessage = redisTemplate.opsForList().leftPop(redisKey);
//
        if (responseMessage == null) {
            return CommonResponse.fail("응답이 아직 없습니다."); // 또는 return ResponseEntity.noContent().build();
        }
        return CommonResponse.success(responseMessage);
    }


}
