package com.thefirsttake.app.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.service.ChatMessageProcessorService;
import com.thefirsttake.app.chat.service.ChatMessageSaveService;
import com.thefirsttake.app.chat.service.SendMessageWorkerService;
import com.thefirsttake.app.chat.service.worker.ChatMessageWorkerService;
import com.thefirsttake.app.common.response.ApiResponse;
import com.thefirsttake.app.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatMessageProcessorService chatMessageProcessorService;
    private final ChatMessageSaveService chatMessageSaveService;
    private final SendMessageWorkerService sendMessageWorkerService;
    private final ChatMessageWorkerService chatMessageWorkerService;


    @Operation(
            summary = "채팅 메시지 전송",
            description = "유저의 채팅 메시지를 저장하고, Redis 큐에 전송합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageRequest.class),
                            examples = @ExampleObject(
                                    name = "채팅 메시지 요청 예시",
                                    summary = "사용자가 보낸 채팅 메시지",
                                    value = """
                {
                  "content": "내일 소개팅 가는데 입을 옷 추천해줘"
                }
                """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "성공 시 저장된 메시지 ID 반환",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답 예시",
                                            summary = "저장된 메시지 ID 반환",
                                            value = """
                    {
                      "status": "success",
                      "message": "요청 성공",
                      "data": 42
                    }
                    """
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "세션 없음 또는 처리 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "에러 응답 예시",
                                            summary = "세션이 없을 경우",
                                            value = """
                    {
                      "status": "fail",
                      "message": "세션이 존재하지 않습니다.",
                      "data": null
                    }
                    """
                                    )
                            )
                    )
            }
    )
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

    @Operation(
            summary = "채팅 응답 메시지 수신",
            description = "Redis 큐에서 세션에 해당하는 응답 메시지가 있는 경우, 여러 개의 메시지를 한번에 꺼내 반환합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "응답 메시지 수신 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답 예시",
                                            summary = "Redis에서 가져온 응답 메시지 리스트",
                                            value = """
                    {
                      "status": "success",
                      "message": "요청 성공",
                      "data": [
                        "당신의 소개팅은 어떤 분위기를 원하세요? 좀 더 캐주얼하고 편안한 느낌을 원하시면 깔끔한 셔츠와 슬랙스, 약간 더 포멀한 느낌을 원하시면 셔츠와 블레이저를 추천드릴게요.1번째 AI",
                        "내일 소개팅이라면 깔끔하고 세련된 느낌이 좋겠어요. 단정한 셔츠에 슬림한 팬츠 또는 치마를 추천드려요. 색상은 무난하면서도 포인트를 줄 수 있는 베이지, 하얀색 또는 연한 파스텔 톤이 좋아요. 액세서리는 과하지 않게 심플한 목걸이나 귀걸이로 마무리하면 자연스럽고 매력적으로 보일 거예요. 편안하면서도 신경 쓴 모습이 가장 좋아요!2번째 AI"
                      ]
                    }
                    """
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "세션 없음 또는 응답 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "세션 없음 예시",
                                                    summary = "세션이 없을 경우",
                                                    value = """
                        {
                          "status": "fail",
                          "message": "세션이 존재하지 않습니다.",
                          "data": null
                        }
                        """
                                            ),
                                            @ExampleObject(
                                                    name = "응답 없음 예시",
                                                    summary = "아직 응답이 Redis에 없는 경우",
                                                    value = """
                        {
                          "status": "fail",
                          "message": "응답이 아직 없습니다.",
                          "data": null
                        }
                        """
                                            )
                                    }
                            )
                    )
            }
    )
    @GetMapping("/receive")
    public CommonResponse receiveChatMessage(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }

        String sessionId = session.getId();
        List<String> responseMessage=chatMessageWorkerService.processChatQueue(sessionId);
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
