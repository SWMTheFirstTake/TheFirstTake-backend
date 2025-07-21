package com.thefirsttake.app.chat.controller;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
import com.thefirsttake.app.chat.dto.response.ChatRoomDto;
import com.thefirsttake.app.chat.dto.response.ChatSessionHistoryResponse;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.service.ChatCurationOrchestrationService;
import com.thefirsttake.app.chat.service.ChatRoomManagementService;
import com.thefirsttake.app.chat.service.ChatQueueService;
import com.thefirsttake.app.chat.service.ChatOrchestrationService;
import com.thefirsttake.app.common.response.CommonResponse;
import com.thefirsttake.app.common.service.S3Service;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Slf4j
@RequiredArgsConstructor
public class ChatController {
    private final ChatCurationOrchestrationService chatCurationOrchestrationService;
    private final ChatQueueService chatQueueService;
    private final UserSessionService userSessionService;
    private final ChatRoomManagementService chatRoomManagementService;
    private final ChatOrchestrationService chatOrchestrationService;
    private final S3Service s3Service;
    @Operation(
            summary = "사용자의 채팅방 목록 조회",
            description = "클라이언트 세션을 기반으로 게스트 사용자를 식별하고, 해당 사용자에 연결된 모든 채팅방 목록을 반환합니다. 새로운 채팅방은 이 API에서 생성하지 않습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공 시 사용자의 모든 채팅방 목록 반환. 사용자 정보가 없다면 빈 리스트[] 반환",
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
    public CommonResponse getAllChatRooms(HttpServletRequest httpRequest){
        HttpSession session = httpRequest.getSession(false);
        // 로그인된 유저인지 확인하는 로직(나중에 유저 로직 넣으면 개발 예정)
        // 지금은 전부 비로그인 사용자 대상이므로, 세션 여부 확인
        if (session == null) {
            System.out.println("history: 세션 새로 생성");
            session=httpRequest.getSession(true);
//            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }

        String sessionId = session.getId();
        System.out.println(sessionId);
        try {
            // ChatRoomManagementService에서 모든 로직을 처리한 DTO 목록을 바로 받아옵니다.
            List<ChatRoomDto> allChatRoomDtos = chatRoomManagementService.getAllChatRoomsForUser(sessionId);

            // 응답에 담을 데이터를 Map으로 구성 (컨트롤러에서 DTO를 한 번 더 감싸는 경우)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("all_rooms", allChatRoomDtos);

            return CommonResponse.success(responseData);

        } catch (EntityNotFoundException e) { // UserEntity를 찾지 못했을 경우 등
            // log.warn("사용자 세션 ID({})에 해당하는 유저를 찾을 수 없습니다: {}", sessionId, e.getMessage());
            return CommonResponse.fail("유저 정보를 찾을 수 없습니다: " + e.getMessage());
        }
        catch (Exception e) {
             log.error("채팅방 조회 중 오류 발생: {}", e.getMessage(), e);
            return CommonResponse.fail("채팅방 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

//    @Operation(
//            summary = "새로운 채팅방 생성",
//            description = "현재 세션의 사용자에 대해 항상 새로운 채팅방을 생성합니다. 기존 채팅방이 있어도 새로운 방이 생성되며, 생성된 채팅방의 ID를 반환합니다."
//                    ,
//            responses = {
//                    @ApiResponse(
//                            responseCode = "200",
//                            description = "성공 시 새로 생성된 채팅방 ID 반환",
//                            content = @Content(
//                                    mediaType = "application/json",
//                                    schema = @Schema(implementation = CommonResponse.class),
//                                    examples = @ExampleObject(
//                                            name = "성공 응답 예시",
//                                            summary = "새로 생성된 채팅방 ID",
//                                            value = """
//                    {
//                      "status": "success",
//                      "message": "요청 성공",
//                      "data": 56789
//                    }
//                    """
//                                    )
//                            )
//                    ),
//                    @ApiResponse(
//                            responseCode = "400",
//                            description = "세션 없음 또는 유효하지 않은 요청",
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
//    @PostMapping("/rooms/new")
//    public CommonResponse createChatRoom(HttpServletRequest httpRequest){
//        // 해당 유저에 대해 새로운 방 생성
//        // 1. 유저 확인/생성
//        // 2. chatRoom테이블에 하나 더 생성. 이 때 user_id는 userEntity의 것이어야함.
//        // 3. 생성된 새로운 채팅방의 id값 반환
//        HttpSession session = httpRequest.getSession(true);
//
//        // 로그인된 유저인지 확인하는 로직(나중에 유저 로직 넣으면 개발 예정)
//
//        // 지금은 전부 비로그인 사용자 대상이므로, 세션 여부 확인
//        if (session == null) {
//            return CommonResponse.fail("세션이 존재하지 않습니다.");
//        }
//
//        String sessionId = session.getId();
//        try {
//            // chatRoomService.createChatRoom 메서드에서 던진 예외를 여기서 잡습니다.
//            Long newRoomId = chatRoomService.createChatRoom(sessionId);
//            return CommonResponse.success(newRoomId); // 성공 시 생성된 roomId 반환
//        }catch (Exception e) {
//            // 혹시 ChatCreationException 외의 예상치 못한 다른 예외가 발생할 경우를 대비합니다.
//            // 이 경우, 일반적인 오류 메시지를 반환하거나, e.getMessage()를 포함할 수 있습니다.
//            // log.error("예상치 못한 오류 발생", e); // 실제 사용 시에는 로그를 남기는 것이 좋습니다.
//            return CommonResponse.fail("채팅방 생성 중 알 수 없는 오류가 발생했습니다.");
//        }
//    }

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
                      "content": "내일 소개팅 가는데 입을 옷 추천해줘",
                      "imageUrl": "https://thefirsttake-file-upload.s3.ap-northeast-2.amazonaws.com/AA12CAC8A9A04D381E787DEF432ED8FC_fsttest.png"
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
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            System.out.println("send: 세션 새로 생성");
            session=httpRequest.getSession(true);
//            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }
        String sessionId = session.getId();
        System.out.println(sessionId);
        System.out.println(chatMessageRequest.getContent());
        System.out.println(chatMessageRequest.getImageUrl());
        try {
            // 새롭게 분리된 서비스 메서드를 호출
            Long resultRoomId = chatOrchestrationService.handleChatMessageSend(roomId, chatMessageRequest, sessionId);
            return CommonResponse.success(resultRoomId);
        } catch (Exception e) {
            log.error("메시지 전송 중 오류 발생: {}", e.getMessage(), e);
            return CommonResponse.fail("메시지 전송 중 오류가 발생했습니다: " + e.getMessage());
        }

    }

    @Operation(
            summary = "이미지 파일 업로드",
            description = "사용자가 업로드한 이미지 파일을 AWS S3에 저장하고, 저장된 파일의 URL을 반환합니다. 세션 ID를 기반으로 파일명을 생성하여 사용자별로 구분됩니다.",

            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "이미지 업로드 성공 시 S3에 저장된 파일의 URL 반환",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답 예시",
                                            summary = "S3에 저장된 이미지 URL 반환",
                                            value = """
                    {
                      "status": "success",
                      "message": "요청 성공",
                      "data": "https://thefirsttake-file-upload.s3.ap-northeast-2.amazonaws.com/sessionId_uuid_filename.jpg"
                    }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "유효하지 않은 요청 데이터 (파일이 없거나 이미지가 아닌 경우)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "파일 없음 예시",
                                                    summary = "업로드할 파일이 비어있는 경우",
                                                    value = """
                        {
                          "status": "fail",
                          "message": "파일이 비어있습니다",
                          "data": null
                        }
                        """
                                            ),
                                            @ExampleObject(
                                                    name = "이미지 파일 아님 예시",
                                                    summary = "이미지가 아닌 파일을 업로드한 경우",
                                                    value = """
                        {
                          "status": "fail",
                          "message": "이미지 파일만 업로드 가능합니다",
                          "data": null
                        }
                        """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류 (S3 업로드 실패 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "서버 오류 예시",
                                            summary = "S3 업로드 중 예상치 못한 오류 발생",
                                            value = """
                    {
                      "status": "fail",
                      "message": "업로드 실패",
                      "data": null
                    }
                    """
                                    )
                            )
                    )
            }
    )
    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse uploadImage(
            @Parameter(description = "업로드할 이미지 파일 (JPG, PNG, GIF 등)")
            @RequestParam("file") MultipartFile file, HttpServletRequest httpRequest){

        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            System.out.println("send: 세션 새로 생성");
            session=httpRequest.getSession(true);
//            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }
        String sessionId = session.getId();
        System.out.println(sessionId);
        try {
            // 파일 검증
            if (file.isEmpty()) {
                return CommonResponse.fail("파일이 비어있습니다");
            }

            if (!file.getContentType().startsWith("image/")) {
                return CommonResponse.fail("이미지 파일만 업로드 가능합니다");
            }

            // S3에 업로드
            String fileUrl = s3Service.uploadFile(file,sessionId);

            return CommonResponse.success(fileUrl);

        } catch (Exception e) {
            return CommonResponse.fail("업로드 실패");
        }

    }
    
    @Operation(
            summary = "채팅 에이전트 응답 메시지 수신",
            description = "Redis 큐에서 해당 채팅방에 대한 AI 에이전트 응답 메시지가 있는 경우, 여러 에이전트의 응답을 한 번에 가져와 반환합니다. 각 에이전트는 고유한 전문 분야를 가지고 있어 다양한 관점에서 패션 추천을 제공합니다.",
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
                            description = "에이전트 응답 메시지 수신 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답 예시",
                                            summary = "Redis에서 가져온 에이전트 응답 리스트",
                                            value = """
                                            {
                                                 "status": "success",
                                                 "message": "요청 성공",
                                                 "data": [
                                                     {
                                                         "agent_id": "style_analyst",
                                                         "agent_name": "스타일 분석가",
                                                         "agent_role": "체형분석과 핏감을 중심으로 추천해드려요!",
                                                         "message": "20대 남성으로 가정하고 추천드리겠습니다. '화이트 린넨 레귤러핏 셔츠와 차콜 데님 슬림핏 팬츠를 추천드려요. 시원하고 깔끔한 느낌으로 소개팅에 적합할 것입니다.'",
                                                         "order": 1
                                                     },
                                                     {
                                                         "agent_id": "trend_expert",
                                                         "agent_name": "트렌드 전문가",
                                                         "agent_role": "최신트렌드, 인플루언서의 스타일을 중심으로 추천해드려요!",
                                                         "message": "라벤더 코튼 레귤러핏 셔츠와 화이트 린넨 슬림핏 팬츠를 추천드려요. 부드러운 색감과 시원한 소재로 소개팅에 편안하면서도 세련된 인상을 줄 수 있을 거예요.",
                                                         "order": 2
                                                     },
                                                     {
                                                         "agent_id": "color_expert",
                                                         "agent_name": "컬러 전문가",
                                                         "agent_role": "피부톤에 어울리는 색상 조합을 바탕으로 추천해드려요!",
                                                         "message": "20대 남성으로 가정하고 추천드리면, \\"화이트 코튼 레귤러핏 셔츠와 네이비 울 슬림핏 팬츠를 추천드려요. 깔끔하고 세련된 인상을 주며, 소개팅에 적합한 조합이 될 것입니다.\\" \\n\\n이 조합은 시원한 느낌을 주면서도 격식을 갖추기에 좋은 선택입니다.",
                                                         "order": 3
                                                     },
                                                     {
                                                         "agent_id": "fitting_coordinator",
                                                         "agent_name": "핏팅 코디네이터",
                                                         "agent_role": "종합적으로 딱 하나의 추천을 해드려요!",
                                                         "message": "소개팅을 위한 최적의 조합으로 \\"화이트 린넨 레귤러핏 셔츠와 네이비 데님 슬림핏 팬츠\\"를 추천드려요. 이 조합은 시원하고 깔끔한 느낌을 주어 소개팅에 적합합니다. 특히 화이트 셔츠는 어떤 상황에서든 좋은 인상을 줄 수 있으며, 네이비 팬츠는 세련된 분위기를 더해줄 것입니다. \\n\\n이 조합으로 자신감 있게 소개팅에 임하실 수 있을 거예요!",
                                                         "order": 4
                                                     }
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
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            session=httpRequest.getSession(true);
//            return CommonResponse.fail("세션이 존재하지 않습니다.");
        }
        // 해당 roomId를 가지는 채팅방에서 처리가 가능한 메시지가 있는지 확인
        List<ChatAgentResponse> agentResponses = chatQueueService.processChatQueue(roomId);

        if (agentResponses == null) {
            return CommonResponse.fail("응답이 아직 없습니다."); // 또는 return ResponseEntity.noContent().build();
        }
        return CommonResponse.success(agentResponses);
    }


}

