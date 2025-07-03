package com.thefirsttake.app.chat.controller;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.dto.response.ChatRoomDto;
import com.thefirsttake.app.chat.dto.response.ChatSessionHistoryResponse;
import com.thefirsttake.app.chat.dto.response.ChatSessionStartResponse;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.service.ChatMessageProcessorService;
//import com.thefirsttake.app.chat.service.ChatMessageSaveService;
import com.thefirsttake.app.chat.service.ChatRoomService;
//import com.thefirsttake.app.chat.service.SendMessageWorkerService;
import com.thefirsttake.app.chat.service.ChatMessageWorkerService;
import com.thefirsttake.app.common.response.CommonResponse;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatMessageProcessorService chatMessageProcessorService;
    private final ChatMessageWorkerService chatMessageWorkerService;
    private final UserSessionService userSessionService;
    private final ChatRoomService chatRoomService;

//    @Operation(
//            summary = "채팅 세션 시작 및 채팅방 생성/조회",
//            description = "클라이언트 세션을 기반으로 게스트 사용자를 식별하거나 생성하고, 해당 사용자에 연결된 채팅방을 가져오거나 새로 생성합니다. 생성/조회된 채팅방의 ID를 반환합니다.",
//            responses = {
//                    @ApiResponse(
//                            responseCode = "200",
//                            description = "성공 시 채팅방 ID 반환",
//                            content = @Content(
//                                    mediaType = "application/json",
//                                    schema = @Schema(implementation = CommonResponse.class),
//                                    examples = @ExampleObject(
//                                            name = "성공 응답 예시",
//                                            summary = "생성/조회된 채팅방 ID",
//                                            value = """
//                    {
//                      "status": "success",
//                      "message": "요청 성공",
//                      "data": 12345
//                    }
//                    """
//                                    )
//                            )
//                    ),
//                    @ApiResponse(
//                            responseCode = "400",
//                            description = "세션 없음 또는 처리 실패",
//                            content = @Content(
//                                    mediaType = "application/json",
//                                    examples = @ExampleObject(
//                                            name = "세션 없음 예시",
//                                            summary = "세션이 존재하지 않는 경우",
//                                            value = """
//                    {
//                      "status": "fail",
//                      "message": "세션이 존재하지 않습니다.",
//                      "data": null
//                    }
//                    """
//                                    )
//                            )
//                    ),
//                    @ApiResponse(
//                            responseCode = "500",
//                            description = "서버 내부 오류",
//                            content = @Content(
//                                    mediaType = "application/json",
//                                    examples = @ExampleObject(
//                                            name = "서버 오류 예시",
//                                            summary = "예상치 못한 서버 오류 발생",
//                                            value = """
//                    {
//                      "status": "fail",
//                      "message": "채팅방 생성 중 오류가 발생했습니다: [오류 메시지]",
//                      "data": null
//                    }
//                    """
//                                    )
//                            )
//                    )
//            }
//    )
@Operation(
        summary = "사용자의 채팅방 목록 조회",
        description = "클라이언트 세션을 기반으로 게스트 사용자를 식별하고, 해당 사용자에 연결된 모든 채팅방 목록을 반환합니다. 새로운 채팅방은 이 API에서 생성하지 않습니다.",
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "성공 시 사용자의 모든 채팅방 목록 반환",
                        content = @Content(
                                mediaType = "application/json",
                                // ✨✨✨ CommonResponse.data의 실제 구현 DTO를 지정 ✨✨✨
                                schema = @Schema(implementation = ChatSessionHistoryResponse.class), // 응답 DTO 변경 가능성
                                examples = @ExampleObject(
                                        name = "성공 응답 예시",
                                        summary = "사용자의 모든 채팅방 목록",
                                        value = """
                        {
                          "status": "success",
                          "message": "채팅방 목록을 성공적으로 조회했습니다.",
                          "data": {
                            "all_rooms": [
                              {
                                "id": 1,
                                "title": "기존 채팅방1",
                                "createdAt": "2024-01-01T10:00:00"
                              },
                              {
                                "id": 2,
                                "title": "두 번째 채팅방",
                                "createdAt": "2024-01-02T11:00:00"
                              }
                            ]
                          }
                        }
                        """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "세션 없음 또는 처리 실패",
                        content = @Content(
                                mediaType = "application/json",
                                examples = @ExampleObject(
                                        name = "세션 없음 예시",
                                        summary = "세션이 존재하지 않는 경우",
                                        value = """
                        {
                          "status": "fail",
                          "message": "세션이 존재하지 않습니다.",
                          "data": null
                        }
                        """
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "500",
                        description = "서버 내부 오류",
                        content = @Content(
                                mediaType = "application/json",
                                examples = @ExampleObject(
                                        name = "서버 오류 예시",
                                        summary = "예상치 못한 서버 오류 발생",
                                        value = """
                        {
                          "status": "fail",
                          "message": "채팅방 목록 조회 중 오류가 발생했습니다: [오류 메시지]",
                          "data": null
                        }
                        """
                                )
                        )
                )
        }
)
@GetMapping("/rooms/history")
    public CommonResponse getOrCreateChatRoom(HttpServletRequest httpRequest){
        HttpSession session = httpRequest.getSession(true);

        // 로그인된 유저인지 확인하는 로직(나중에 유저 로직 넣으면 개발 예정)

        // 지금은 전부 비로그인 사용자 대상이므로, 세션 여부 확인
        if (session == null) {
            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }

        String sessionId = session.getId();
        System.out.println(sessionId);
        try {
            // 1. 유저 확인/생성
//            UserEntity userEntity = userSessionService.getOrCreateGuestUser(sessionId);
            UserEntity userEntity = userSessionService.getUser(sessionId);
            if(userEntity==null){

            }
            // 2. 새로운 채팅방 생성 (무조건 생성)
//            ChatRoom newChatRoom = chatRoomService.createNewChatRoom(userEntity);

            // 3. 해당 유저의 모든 채팅방 목록 가져오기 (새로 생성된 방 포함)
            // chatRoomService.getOrCreateChatRoom은 기존 방이 없으면 하나 만들고, 있으면 기존 방을 모두 반환합니다.
            // 여기서는 `createNewChatRoom`으로 새 방을 만들었으므로, 모든 방을 다시 조회하는 것이 좋습니다.
//            System.out.println(userEntity.getId());
            List<ChatRoom> allChatRooms = chatRoomService.getChatRooms(userEntity); // chatRoomService에 getAllChatRooms(UserEntity userEntity) 같은 메서드가 필요합니다.
            System.out.println(allChatRooms.size());
            List<ChatRoomDto> allChatRoomNumbers=chatRoomService.getChatRoomNumbers(allChatRooms);
            // 응답에 담을 데이터를 Map으로 구성
            Map<String, Object> responseData = new HashMap<>();
//            responseData.put("new_room_id", newChatRoom.getId());
            responseData.put("all_rooms", allChatRoomNumbers); // ChatRoom 객체들이 JSON으로 직렬화됩니다.

            return CommonResponse.success(responseData);
//            // 1. 유저 확인/생성
//            UserEntity userEntity=userSessionService.getOrCreateGuestUser(sessionId);
//            // 2. 채팅방 확인/생성
//            // ✅ chatRoomService를 통해 채팅방 가져오기 or 생성
//            List<ChatRoom> chatRooms = chatRoomService.getChatRooms(userEntity);
//            ChatRoom chatRoom=chatRoomService.createNewChatRoom(userEntity);
//            return CommonResponse.success(chatRoom.getId());


        } catch (Exception e) {
            e.printStackTrace(); // 로그로 남기기 (혹은 log.error(...))

            return CommonResponse.fail("채팅방 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
            summary = "새로운 채팅방 생성",
            description = "현재 세션의 사용자에 대해 항상 새로운 채팅방을 생성합니다. 기존 채팅방이 있어도 새로운 방이 생성되며, 생성된 채팅방의 ID를 반환합니다."
                    ,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공 시 새로 생성된 채팅방 ID 반환",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답 예시",
                                            summary = "새로 생성된 채팅방 ID",
                                            value = """
                    {
                      "status": "success",
                      "message": "요청 성공",
                      "data": 56789
                    }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "세션 없음 또는 유효하지 않은 요청",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "세션 없음 예시",
                                            summary = "세션이 존재하지 않는 경우",
                                            value = """
                    {
                      "status": "fail",
                      "message": "세션이 존재하지 않습니다.",
                      "data": null
                    }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "서버 오류 예시",
                                            summary = "예상치 못한 서버 오류 발생",
                                            value = """
                    {
                      "status": "fail",
                      "message": "채팅방 생성 중 오류가 발생했습니다: [오류 메시지]",
                      "data": null
                    }
                    """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/rooms/new")
    public CommonResponse createNewChatRoom(HttpServletRequest httpRequest){
        // 해당 유저에 대해 새로운 방 생성
        // 1. 유저 확인/생성
        // 2. chatRoom테이블에 하나 더 생성. 이 때 user_id는 userEntity의 것이어야함.
        HttpSession session = httpRequest.getSession(true);

        // 로그인된 유저인지 확인하는 로직(나중에 유저 로직 넣으면 개발 예정)

        // 지금은 전부 비로그인 사용자 대상이므로, 세션 여부 확인
        if (session == null) {
            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }

        String sessionId = session.getId();
        try {
            // 1. 유저 확인/생성
            UserEntity userEntity=userSessionService.getOrCreateGuestUser(sessionId);
            ChatRoom chatRoom = chatRoomService.createNewChatRoom(userEntity);
            return CommonResponse.success(chatRoom.getId());
        } catch (Exception e) {
            e.printStackTrace(); // 로그로 남기기 (혹은 log.error(...))

            return CommonResponse.fail("채팅방 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
            summary = "채팅 메시지 전송",
            description = "사용자의 채팅 메시지를 받아 데이터베이스에 저장하고, AI 응답 처리를 위해 Redis 워커 큐에 전송합니다. 저장된 메시지의 ID를 반환합니다.",
            parameters = {
                    @Parameter(
                            name = "roomId",
                            description = "메시지를 보낼 채팅방의 ID",
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageRequest.class),
                            examples = @ExampleObject(
                                    name = "채팅 메시지 요청 예시",
                                    summary = "사용자가 보낸 채팅 메시지 내용",
                                    value = """
                    {
                      "content": "내일 소개팅 가는데 입을 옷 추천해줘"
                    }
                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "메시지 저장 및 큐 전송 성공 시 저장된 메시지 ID 반환",
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
                    @ApiResponse(
                            responseCode = "400",
                            description = "유효하지 않은 요청 데이터 (예: 존재하지 않는 roomId) 또는 세션 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "세션 없음 예시",
                                                    summary = "세션이 존재하지 않는 경우",
                                                    value = """
                            {
                              "status": "fail",
                              "message": "세션이 존재하지 않습니다.",
                              "data": null
                            }
                            """
                                            ),
                                            @ExampleObject(
                                                    name = "채팅방/사용자 없음 예시",
                                                    summary = "roomId 또는 UserEntity를 찾을 수 없는 경우",
                                                    value = """
                            {
                              "status": "fail",
                              "message": "채팅방을 찾을 수 없습니다. (또는 사용자)",
                              "data": null
                            }
                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "서버 오류 예시",
                                            summary = "예상치 못한 서버 오류 발생",
                                            value = """
                        {
                          "status": "fail",
                          "message": "메시지 전송 중 오류가 발생했습니다: [오류 메시지]",
                          "data": null
                        }
                        """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/send")
    public CommonResponse sendChatMessage(@RequestParam(value="roomId",required = false) Long roomId, @RequestBody ChatMessageRequest chatMessageRequest, HttpServletRequest httpRequest){
        HttpSession session = httpRequest.getSession(true);

        // 로그인된 유저인지 확인하는 로직(나중에 유저 로직 넣으면 개발 예정)

        // 지금은 전부 비로그인 사용자 대상이므로, 세션 여부 확인
        if (session == null) {
            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }
        String sessionId = session.getId();
        if(roomId==null){
            // 새 채팅방 생성하고 roomId가져오기
            try {
                // 1. 유저 확인/생성
                UserEntity userEntity=userSessionService.getOrCreateGuestUser(sessionId);
                ChatRoom chatRoom = chatRoomService.createNewChatRoom(userEntity);
                roomId=chatRoom.getId();
            } catch (Exception e) {
                e.printStackTrace(); // 로그로 남기기 (혹은 log.error(...))

                return CommonResponse.fail("채팅방 생성 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        try {
            // 1. roomid를 바탕으로 -> 채팅방 확인 -> 유저 확인
            UserEntity userEntity=chatRoomService.getUserEntityByRoomId(roomId);
            // 2. 메시지 PostgreSQL에 저장
            Long savedId=chatRoomService.saveUserMessage(userEntity,chatMessageRequest,roomId);
            // 3. Redis 워커 큐에 푸시
            chatRoomService.sendChatQueue(roomId, chatMessageRequest);
            // 1. 유저 확인/생성
            // 2. 채팅방 확인/생성
            return CommonResponse.success(roomId);

        } catch (Exception e) {
            e.printStackTrace(); // 로그로 남기기 (혹은 log.error(...))

            return CommonResponse.fail("메시지 전송 중 오류가 발생했습니다: " + e.getMessage());
        }

    }

//    @Operation(
//            summary = "채팅 응답 메시지 수신",
//            description = "Redis 큐에서 세션에 해당하는 응답 메시지가 있는 경우, 여러 개의 메시지를 한번에 꺼내 반환합니다.",
//            responses = {
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                            responseCode = "200",
//                            description = "응답 메시지 수신 성공",
//                            content = @Content(
//                                    mediaType = "application/json",
//                                    schema = @Schema(implementation = CommonResponse.class),
//                                    examples = @ExampleObject(
//                                            name = "성공 응답 예시",
//                                            summary = "Redis에서 가져온 응답 메시지 리스트",
//                                            value = """
//                    {
//                      "status": "success",
//                      "message": "요청 성공",
//                      "data": [
//                        "당신의 소개팅은 어떤 분위기를 원하세요? 좀 더 캐주얼하고 편안한 느낌을 원하시면 깔끔한 셔츠와 슬랙스, 약간 더 포멀한 느낌을 원하시면 셔츠와 블레이저를 추천드릴게요.1번째 AI",
//                        "내일 소개팅이라면 깔끔하고 세련된 느낌이 좋겠어요. 단정한 셔츠에 슬림한 팬츠를 추천드려요. 색상은 무난하면서도 포인트를 줄 수 있는 베이지, 하얀색 또는 연한 파스텔 톤이 좋아요. 액세서리는 과하지 않게 심플한 목걸이나 귀걸이로 마무리하면 자연스럽고 매력적으로 보일 거예요. 편안하면서도 신경 쓴 모습이 가장 좋아요!2번째 AI"
//                      ]
//                    }
//                    """
//                                    )
//                            )
//                    ),
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                            responseCode = "400",
//                            description = "세션 없음 또는 응답 없음",
//                            content = @Content(
//                                    mediaType = "application/json",
//                                    examples = {
//                                            @ExampleObject(
//                                                    name = "세션 없음 예시",
//                                                    summary = "세션이 없을 경우",
//                                                    value = """
//                        {
//                          "status": "fail",
//                          "message": "세션이 존재하지 않습니다.",
//                          "data": null
//                        }
//                        """
//                                            ),
//                                            @ExampleObject(
//                                                    name = "응답 없음 예시",
//                                                    summary = "아직 응답이 Redis에 없는 경우",
//                                                    value = """
//                        {
//                          "status": "fail",
//                          "message": "응답이 아직 없습니다.",
//                          "data": null
//                        }
//                        """
//                                            )
//                                    }
//                            )
//                    )
//            }
//    )
    @Operation(
            summary = "채팅 응답 메시지 수신",
            description = "Redis 큐에서 해당 채팅방에 대한 AI 응답 메시지가 있는 경우, 여러 개의 메시지를 한 번에 가져와 반환합니다.",
            parameters = {
                    @Parameter(
                            name = "roomId",
                            description = "응답 메시지를 받을 채팅방의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    )
            },
            responses = {
                    @ApiResponse(
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
                            "내일 소개팅이라면 깔끔하고 세련된 느낌이 좋겠어요. 단정한 셔츠에 슬림한 팬츠를 추천드려요. 색상은 무난하면서도 포인트를 줄 수 있는 베이지, 하얀색 또는 연한 파스텔 톤이 좋아요. 액세서리는 과하지 않게 심플한 목걸이나 귀걸이로 마무리하면 자연스럽고 매력적으로 보일 거예요. 편안하면서도 신경 쓴 모습이 가장 좋아요!2번째 AI"
                          ]
                        }
                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "204", // No Content
                            description = "Redis에 아직 응답 메시지가 없는 경우",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
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
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 파라미터 오류 (예: roomId 누락)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "유효하지 않은 roomId 예시",
                                            summary = "필수 roomId 파라미터가 누락되거나 유효하지 않은 경우",
                                            value = """
                            {
                              "status": "fail",
                              "message": "잘못된 요청입니다. roomId를 확인해주세요.",
                              "data": null
                            }
                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "서버 오류 예시",
                                            summary = "예상치 못한 서버 오류 발생",
                                            value = """
                        {
                          "status": "fail",
                          "message": "메시지 수신 중 오류가 발생했습니다: [오류 메시지]",
                          "data": null
                        }
                        """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/receive")
    public CommonResponse receiveChatMessage(@RequestParam("roomId") Long roomId, HttpServletRequest httpRequest) {
        List<String> responseMessage=chatMessageWorkerService.processChatQueue(roomId);

        if (responseMessage == null) {
            return CommonResponse.fail("응답이 아직 없습니다."); // 또는 return ResponseEntity.noContent().build();
        }
        return CommonResponse.success(responseMessage);
    }


}

