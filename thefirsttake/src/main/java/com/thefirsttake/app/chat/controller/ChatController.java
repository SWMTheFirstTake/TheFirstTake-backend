package com.thefirsttake.app.chat.controller;

import com.thefirsttake.app.chat.dto.request.ChatMessageRequest;
import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
import com.thefirsttake.app.chat.dto.response.ChatMessageListResponse;
import com.thefirsttake.app.chat.dto.response.ChatRoomDto;
import com.thefirsttake.app.chat.dto.response.ChatSessionHistoryResponse;
import com.thefirsttake.app.chat.entity.ChatRoom;
import com.thefirsttake.app.chat.service.ChatCurationOrchestrationService;
import com.thefirsttake.app.chat.service.ChatMessageService;
import com.thefirsttake.app.chat.service.ChatRoomManagementService;
import com.thefirsttake.app.chat.service.ChatQueueService;
import com.thefirsttake.app.chat.service.ChatOrchestrationService;
import com.thefirsttake.app.chat.service.ProductSearchService;
import com.thefirsttake.app.chat.service.ProductCacheService;
import com.thefirsttake.app.common.response.CommonResponse;
import com.thefirsttake.app.common.service.S3Service;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.service.UserSessionService;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
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

import java.time.LocalDateTime;
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
    private final ChatMessageService chatMessageService;
    private final S3Service s3Service;
    private final ProductSearchService productSearchService;
    private final ProductCacheService productCacheService;
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

    @Operation(
            summary = "채팅 메시지 전송",
            description = "사용자의 채팅 메시지를 받아 데이터베이스에 저장하고, AI 응답 처리를 위해 Redis 워커 큐에 전송합니다. 이미지 URL이 포함된 경우 함께 처리됩니다. 저장된 메시지의 ID를 반환합니다.",
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
                            schema = @Schema(
                                    implementation = ChatMessageRequest.class,
                                    description = "content: 필수, image_url: 선택적 (S3 이미지 URL)"
                            ),
                            examples = @ExampleObject(
                                    name = "채팅 메시지 요청 예시",
                                    summary = "사용자가 보낸 채팅 메시지 내용",
                                    value = """
                    {
                      "content": "내일 소개팅 가는데 입을 옷 추천해줘",
                      "image_url": "https://thefirsttake-file-upload.s3.ap-northeast-2.amazonaws.com/AA12CAC8A9A04D381E787DEF432ED8FC_fsttest.png"
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
        // System.out.println(sessionId);
        // System.out.println(chatMessageRequest.getContent());
        // System.out.println(chatMessageRequest.getImageUrl());
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
            description = "Redis 큐에서 해당 채팅방에 대한 AI 에이전트 응답 메시지가 있는 경우, 단일 전문가의 분석 결과를 반환합니다. 스타일 분석가, 컬러 전문가, 핏팅 코디네이터 중 하나의 전문가가 분석한 결과입니다.",
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
                            description = "단일 전문가 분석 결과 수신 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답 예시",
                                            summary = "Redis에서 가져온 단일 전문가 분석 결과",
                                                                                         value = """
                                             {
                                                  "status": "success",
                                                  "message": "요청 성공",
                                                  "data": {
                                                      "message": "소개팅에 어울리는 스타일을 분석해보겠습니다. 체형과 핏감을 중심으로 추천해드리겠습니다.\\n\\n차콜 그레이 울 슬림핏 정장 바지와 화이트 코튼 슬림핏 셔츠, 그리고 네이비 코튼 슬림핏 블레이저를 추천드립니다. 이 조합은 깔끔하면서도 세련된 느낌을 연출할 수 있습니다. \\n\\n특히 차콜 그레이 울 소재와 화이트 코튼 소재의 대비가 시각적으로 좋은 효과를 줄 것 같습니다. 여기에 네이비 블레이저를 레이어링하면 격식 있는 소개팅 룩으로 더욱 적합할 것 같습니다.\\n\\n전체적으로 이 조합은 모던하고 성숙한 느낌을 연출할 수 있어 소개팅 자리에 잘 어울릴 것 같습니다.",
                                                      "order": 1,
                                                      "agent_id": "style_analyst",
                                                      "agent_name": "스타일 분석가",
                                                      "agent_role": "체형분석과 핏감을 중심으로 추천해드려요!",
                                                      "products": [
                                                          {
                                                              "product_url": "https://sw-fashion-image-data.s3.amazonaws.com/TOP/1002/4227290/segment/0_17.jpg",
                                                              "product_id": "4227290"
                                                          },
                                                          {
                                                              "product_url": "https://sw-fashion-image-data.s3.amazonaws.com/BOTTOM/3002/3797063/segment/5_0.jpg",
                                                              "product_id": "3797063"
                                                          }
                                                      ]
                                                  }
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
//        List<ChatAgentResponse> agentResponses = chatQueueService.processChatQueue(roomId);
        ChatAgentResponse agentResponse = chatQueueService.processChatQueue(roomId);
        
        if (agentResponse == null) {
            return CommonResponse.fail("응답이 아직 없습니다."); // 또는 return ResponseEntity.noContent().build();
        }
        
        // 에이전트 응답 메시지 출력
        System.out.println("에이전트 응답 메시지: " + agentResponse.getMessage());
        
        // 상품 검색 API 호출
        Map<String, Object> searchResult = productSearchService.searchProducts(agentResponse.getMessage());
        if(searchResult == null){
            return CommonResponse.fail("상품 검색 결과가 없습니다.");
        }
        System.out.println("상품 검색 결과: " + searchResult);
        
        // 🔄 상품 정보를 Redis에 캐싱
        try {
            productCacheService.cacheProductsFromSearchResult(searchResult);
        } catch (Exception e) {
            log.warn("상품 정보 캐싱 중 오류 발생 (검색 결과 반환은 계속): {}", e.getMessage());
        }
        
        // 상품 이미지 URL 및 상품 ID 추출
        java.util.List<String> productImageUrls = productSearchService.extractProductImageUrls(searchResult);
        java.util.List<String> productIds = productCacheService.extractProductIds(searchResult);
        
        // 상품 정보를 products 배열로 구성
        if (!productImageUrls.isEmpty() && !productIds.isEmpty()) {
            java.util.List<com.thefirsttake.app.chat.dto.response.ProductInfo> products = new java.util.ArrayList<>();
            
            // URL과 ID의 개수가 같다고 가정하고 매핑
            int minSize = Math.min(productImageUrls.size(), productIds.size());
            for (int i = 0; i < minSize; i++) {
                com.thefirsttake.app.chat.dto.response.ProductInfo productInfo = 
                    com.thefirsttake.app.chat.dto.response.ProductInfo.builder()
                        .productUrl(productImageUrls.get(i))
                        .productId(productIds.get(i))
                        .build();
                products.add(productInfo);
            }
            
            agentResponse.setProducts(products);
            System.out.println("상품 정보 " + products.size() + "개 설정: " + products);
        }
        
        // 상품 정보가 있는 경우 DB에 저장
        if (!productImageUrls.isEmpty() || !productIds.isEmpty()) {
            // 상품 이미지가 포함된 응답을 데이터베이스에 저장
            try {
                UserEntity userEntity = chatRoomManagementService.getUserEntityByRoomId(roomId);
                ChatRoom chatRoom = chatRoomManagementService.getRoomById(roomId);
                chatMessageService.saveAIResponse(userEntity, chatRoom, agentResponse);
                System.out.println("상품 이미지가 포함된 AI 응답을 데이터베이스에 저장했습니다.");
            } catch (Exception e) {
                System.err.println("상품 이미지가 포함된 AI 응답 저장 실패: " + e.getMessage());
                // 저장 실패해도 응답은 반환
            }
        }
        
        return CommonResponse.success(agentResponse);
    }

    @Operation(
            summary = "상품 정보 조회",
            description = "Redis에 캐시된 상품 정보를 product_id로 조회합니다. 상품명, 설명, 스타일 태그, TPO 태그 정보를 반환합니다.",
            parameters = {
                    @Parameter(
                            name = "productId",
                            description = "조회할 상품의 ID",
                            required = true,
                            schema = @Schema(type = "string"),
                            example = "4227290"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "상품 정보 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답 예시",
                                            summary = "Redis에서 조회한 상품 정보",
                                            value = """
                                            {
                                                "status": "success",
                                                "message": "요청 성공",
                                                "data": {
                                                    "product_name": "STRIPE SUNDAY SHIRT [IVORY]",
                                                    "comprehensive_description": "베이지 색상의 세로 스트라이프 패턴이 돋보이는 반팔 셔츠입니다. 라운드넥 칼라와 버튼 여밈으로 심플한 디자인을 갖추고 있으며, 정면에는 패치 포켓이 있어 실용성을 더했습니다.",
                                                    "style_tags": ["캐주얼", "모던", "심플 베이직"],
                                                    "tpo_tags": ["데일리", "여행"]
                                                }
                                            }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "상품 정보를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "상품 없음 예시",
                                            summary = "해당 product_id로 캐시된 상품 정보가 없는 경우",
                                            value = """
                                            {
                                                "status": "fail",
                                                "message": "상품 정보를 찾을 수 없습니다.",
                                                "data": null
                                            }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (product_id 누락 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "잘못된 요청 예시",
                                            summary = "product_id가 누락되거나 잘못된 경우",
                                            value = """
                                            {
                                                "status": "fail",
                                                "message": "잘못된 요청입니다. product_id를 확인해주세요.",
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
                                            summary = "Redis 조회 중 예상치 못한 오류 발생",
                                            value = """
                                            {
                                                "status": "fail",
                                                "message": "상품 정보 조회 중 오류가 발생했습니다.",
                                                "data": null
                                            }
                                            """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/product/{productId}")
    public CommonResponse getProductInfo(@PathVariable("productId") String productId) {
        try {
            // 입력 유효성 검증
            if (productId == null || productId.trim().isEmpty()) {
                return CommonResponse.fail("잘못된 요청입니다. product_id를 확인해주세요.");
            }
            
            // Redis에서 상품 정보 조회
            Map<String, Object> productInfo = productCacheService.getProductInfo(productId.trim());
            
            if (productInfo == null) {
                return CommonResponse.fail("상품 정보를 찾을 수 없습니다.");
            }
            
            log.info("✅ 상품 정보 조회 성공: productId={}, keys={}", productId, productInfo.keySet());
            return CommonResponse.success(productInfo);
            
        } catch (Exception e) {
            log.error("❌ 상품 정보 조회 중 오류 발생: productId={}, error={}", productId, e.getMessage(), e);
            return CommonResponse.fail("상품 정보 조회 중 오류가 발생했습니다.");
        }
    }

    @Operation(
            summary = "채팅방 메시지 목록 조회 (무한 스크롤)",
            description = "특정 채팅방의 메시지들을 무한 스크롤 형태로 조회합니다. before 파라미터를 사용하여 이전 메시지들을 페이지네이션으로 가져옵니다.",
            parameters = {
                    @Parameter(
                            name = "roomId",
                            description = "메시지를 조회할 채팅방의 ID",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64")
                    ),
                                         @Parameter(
                             name = "limit",
                             description = "한 번에 가져올 메시지 개수 (기본값: 5, 최대: 50)",
                             schema = @Schema(type = "integer", example = "5")
                     ),
                    @Parameter(
                            name = "before",
                            description = "이 시간 이전의 메시지들을 조회 (ISO 8601 형식)",
                            schema = @Schema(type = "string", format = "date-time", example = "2024-01-15T10:00:00Z")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "채팅 메시지 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답 예시",
                                            summary = "채팅 메시지 목록과 페이징 정보",
                                                                                         value = """
                             {
                               "status": "success",
                               "message": "채팅 메시지 목록을 성공적으로 조회했습니다.",
                               "data": {
                                 "messages": [
                                   {
                                     "id": 1,
                                     "content": "내일 소개팅 가는데 입을 옷 추천해줘",
                                     "image_url": null,
                                     "message_type": "USER",
                                     "created_at": "2024-01-15T09:30:00Z",
                                     "agent_type": null,
                                     "agent_name": null,
                                     "product_image_url": null
                                   },
                                                                                                           {
                                       "id": 2,
                                       "content": "소개팅에 어울리는 스타일을 추천해드리겠습니다.",
                                       "image_url": null,
                                       "message_type": "STYLE",
                                       "created_at": "2024-01-15T09:35:00Z",
                                       "agent_type": "STYLE",
                                       "agent_name": "스타일 분석가",
                                       "product_image_url": [
                                           "https://sw-fashion-image-data.s3.amazonaws.com/TOP/1002/4227290/segment/0_17.jpg",
                                           "https://sw-fashion-image-data.s3.amazonaws.com/BOTTOM/3002/3797063/segment/5_0.jpg"
                                       ]
                                     }
                                 ],
                                 "has_more": true,
                                 "next_cursor": "2024-01-15T09:30:00Z",
                                 "count": 2
                               }
                             }
                             """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "유효하지 않은 요청 데이터",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "잘못된 요청 예시",
                                            summary = "roomId가 누락되거나 유효하지 않은 경우",
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
                            responseCode = "404",
                            description = "채팅방을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "채팅방 없음 예시",
                                            summary = "존재하지 않는 채팅방 ID",
                                            value = """
                            {
                              "status": "fail",
                              "message": "채팅방을 찾을 수 없습니다.",
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
                              "message": "메시지 목록 조회 중 오류가 발생했습니다: [오류 메시지]",
                              "data": null
                            }
                            """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/rooms/{roomId}/messages")
    public CommonResponse getChatMessages(
            @PathVariable("roomId") Long roomId,
            @RequestParam(value = "limit", required = false, defaultValue = "5") Integer limit,
            @RequestParam(value = "before", required = false) String beforeStr,
            HttpServletRequest httpRequest) {
        
        try {
            // 세션 확인 (다른 API들과 동일하게 세션 자동 생성)
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                System.out.println("messages: 세션 새로 생성");
                session = httpRequest.getSession(true);
            }
            
            // 채팅방 존재 여부 확인
            try {
                chatRoomManagementService.getRoomById(roomId);
            } catch (EntityNotFoundException e) {
                return CommonResponse.fail("채팅방을 찾을 수 없습니다.");
            }
            
            // before 파라미터 파싱
            LocalDateTime before = null;
            if (beforeStr != null && !beforeStr.trim().isEmpty()) {
                try {
                    before = LocalDateTime.parse(beforeStr.replace("Z", ""));
                } catch (Exception e) {
                    return CommonResponse.fail("잘못된 날짜 형식입니다. ISO 8601 형식을 사용해주세요.");
                }
            }
            
            // 메시지 목록 조회
            ChatMessageListResponse response = chatMessageService.getChatMessagesWithPagination(roomId, limit, before);
            
            return CommonResponse.success(response);
            
        } catch (Exception e) {
            log.error("채팅 메시지 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return CommonResponse.fail("메시지 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

