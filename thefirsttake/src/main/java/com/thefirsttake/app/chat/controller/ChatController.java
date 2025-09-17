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
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.io.IOException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {
    
    // ObjectMapper 싱글톤으로 메모리 최적화
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // SSE 연결별 메모리 추적을 위한 맵
    private final Map<String, Long> connectionMemoryMap = new ConcurrentHashMap<>();
    
    private final ChatCurationOrchestrationService chatCurationOrchestrationService;
    private final ChatQueueService chatQueueService;
    private final UserSessionService userSessionService;
    private final ChatRoomManagementService chatRoomManagementService;
    private final ChatOrchestrationService chatOrchestrationService;
    private final ChatMessageService chatMessageService;
    private final S3Service s3Service;
    private final ProductSearchService productSearchService;
    private final ProductCacheService productCacheService;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    
    // 메트릭 관련 의존성
    private final Counter sseConnectionCounter;
    private final Counter sseDisconnectionCounter;
    private final Timer sseConnectionDurationTimer;
    private final Counter llmApiCallCounter;
    private final Counter llmApiSuccessCounter;
    private final Counter llmApiFailureCounter;
    private final Timer llmApiResponseTimer;
    private final Counter productSearchApiCallCounter;
    private final Counter productSearchApiSuccessCounter;
    private final Counter productSearchApiFailureCounter;
    private final Timer productSearchApiResponseTimer;
    
    // 새로운 전문가별 메트릭
    private final MeterRegistry meterRegistry;
    private final Counter llmApiCallCounterByExpert;
    
    // SSE API 전체 응답 시간 메트릭
    private final Timer sseApiTotalResponseTimer;
    private final Counter sseApiTotalCounter;
    private final Counter sseApiSuccessCounter;
    private final Counter sseApiFailureCounter;
    
    // Stream API 메모리 모니터링 메트릭
    private final DistributionSummary sseApiMemoryUsageSummary;
    private final Counter sseApiMemoryPeakCounter;
    private final Timer sseApiGcDurationTimer;
    
    // SSE 커넥션 풀 최적화 메트릭
    private final Counter sseConnectionsTotalCounter;
    private final io.micrometer.core.instrument.Gauge sseConnectionsActiveGauge;
    private final Timer sseConnectionLifetimeTimer;
    
    // 활성 연결 수를 추적하기 위한 AtomicInteger (Gauge에서 사용)
    private final java.util.concurrent.atomic.AtomicInteger activeConnectionsCount;
    
    // 연결별 종료 상태 추적 (중복 감소 방지)
    private final java.util.concurrent.ConcurrentHashMap<String, Boolean> connectionEndedMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final DistributionSummary sseConnectionMemoryUsageSummary;
    private final Counter sseConnectionPoolHitsCounter;
    private final Counter sseConnectionPoolMissesCounter;
    private final Timer sseConnectionCreationTimer;
    private final Counter sseConnectionTimeoutCounter;
    private final Counter sseConnectionErrorCounter;
    
    public ChatController(ChatCurationOrchestrationService chatCurationOrchestrationService,
                         ChatQueueService chatQueueService,
                         UserSessionService userSessionService,
                         ChatRoomManagementService chatRoomManagementService,
                         ChatOrchestrationService chatOrchestrationService,
                         ChatMessageService chatMessageService,
                         S3Service s3Service,
                         ProductSearchService productSearchService,
                         ProductCacheService productCacheService,
                         RestTemplate restTemplate,
                         RedisTemplate<String, String> redisTemplate,
                         Counter sseConnectionCounter,
                         Counter sseDisconnectionCounter,
                         Timer sseConnectionDurationTimer,
                         Counter llmApiCallCounter,
                         Counter llmApiSuccessCounter,
                         Counter llmApiFailureCounter,
                         Timer llmApiResponseTimer,
                         Counter productSearchApiCallCounter,
                         Counter productSearchApiSuccessCounter,
                         Counter productSearchApiFailureCounter,
                         Timer productSearchApiResponseTimer,
                         MeterRegistry meterRegistry,
                         Counter llmApiCallCounterByExpert,
                         Timer sseApiTotalResponseTimer,
                         Counter sseApiTotalCounter,
                         Counter sseApiSuccessCounter,
                         Counter sseApiFailureCounter,
                         DistributionSummary sseApiMemoryUsageSummary,
                         Counter sseApiMemoryPeakCounter,
                         Timer sseApiGcDurationTimer,
                         Counter sseConnectionsTotalCounter,
                         io.micrometer.core.instrument.Gauge sseConnectionsActiveGauge,
                         Timer sseConnectionLifetimeTimer,
                         DistributionSummary sseConnectionMemoryUsageSummary,
                         Counter sseConnectionPoolHitsCounter,
                         Counter sseConnectionPoolMissesCounter,
                         Timer sseConnectionCreationTimer,
                         Counter sseConnectionTimeoutCounter,
                         Counter sseConnectionErrorCounter) {
        this.chatCurationOrchestrationService = chatCurationOrchestrationService;
        this.chatQueueService = chatQueueService;
        this.userSessionService = userSessionService;
        this.chatRoomManagementService = chatRoomManagementService;
        this.chatOrchestrationService = chatOrchestrationService;
        this.chatMessageService = chatMessageService;
        this.s3Service = s3Service;
        this.productSearchService = productSearchService;
        this.productCacheService = productCacheService;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.sseConnectionCounter = sseConnectionCounter;
        this.sseDisconnectionCounter = sseDisconnectionCounter;
        this.sseConnectionDurationTimer = sseConnectionDurationTimer;
        this.llmApiCallCounter = llmApiCallCounter;
        this.llmApiSuccessCounter = llmApiSuccessCounter;
        this.llmApiFailureCounter = llmApiFailureCounter;
        this.llmApiResponseTimer = llmApiResponseTimer;
        this.productSearchApiCallCounter = productSearchApiCallCounter;
        this.productSearchApiSuccessCounter = productSearchApiSuccessCounter;
        this.productSearchApiFailureCounter = productSearchApiFailureCounter;
        this.productSearchApiResponseTimer = productSearchApiResponseTimer;
        this.meterRegistry = meterRegistry;
        this.llmApiCallCounterByExpert = llmApiCallCounterByExpert;
        this.sseApiTotalResponseTimer = sseApiTotalResponseTimer;
        this.sseApiTotalCounter = sseApiTotalCounter;
        this.sseApiSuccessCounter = sseApiSuccessCounter;
        this.sseApiFailureCounter = sseApiFailureCounter;
        this.sseApiMemoryUsageSummary = sseApiMemoryUsageSummary;
        this.sseApiMemoryPeakCounter = sseApiMemoryPeakCounter;
        this.sseApiGcDurationTimer = sseApiGcDurationTimer;
        this.sseConnectionsTotalCounter = sseConnectionsTotalCounter;
        this.sseConnectionsActiveGauge = sseConnectionsActiveGauge;
        this.activeConnectionsCount = com.thefirsttake.app.config.MetricsConfig.getGlobalActiveConnections();
        this.sseConnectionLifetimeTimer = sseConnectionLifetimeTimer;
        this.sseConnectionMemoryUsageSummary = sseConnectionMemoryUsageSummary;
        this.sseConnectionPoolHitsCounter = sseConnectionPoolHitsCounter;
        this.sseConnectionPoolMissesCounter = sseConnectionPoolMissesCounter;
        this.sseConnectionCreationTimer = sseConnectionCreationTimer;
        this.sseConnectionTimeoutCounter = sseConnectionTimeoutCounter;
        this.sseConnectionErrorCounter = sseConnectionErrorCounter;
    }
    
    @Value("${llm.server.expert-stream-url}")
    private String llmExpertStreamUrl;
    
    /**
     * 현재 JVM 메모리 사용량 측정 및 메트릭 기록
     */
    private void recordMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            // 메모리 사용량 기록
            sseApiMemoryUsageSummary.record(usedMemory);
            
            // 메모리 사용률이 80% 이상이면 피크 카운터 증가
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            if (memoryUsagePercent > 80.0) {
                sseApiMemoryPeakCounter.increment();
                log.warn("High memory usage detected: {}% ({}MB/{}MB)", 
                    String.format("%.1f", memoryUsagePercent),
                    usedMemory / 1024 / 1024,
                    maxMemory / 1024 / 1024);
            }
        } catch (Exception e) {
            log.warn("Failed to record memory usage: {}", e.getMessage());
        }
    }
    
    /**
     * SSE 커넥션 생성 시 메트릭 기록
     */
    private Timer.Sample startSSEConnectionTracking(String connectionId) {
        // SSE 커넥션 생성 시작
        Timer.Sample connectionCreationTimer = Timer.start();
        sseConnectionsTotalCounter.increment();
        activeConnectionsCount.incrementAndGet();
        sseConnectionPoolMissesCounter.increment(); // 현재는 풀 없으므로 항상 miss
        
        // 연결 시작 시점의 메모리 기록
        Runtime runtime = Runtime.getRuntime();
        long startMemory = runtime.totalMemory() - runtime.freeMemory();
        connectionMemoryMap.put(connectionId, startMemory);
        
        log.info("SSE connection created. Total: {}, Active: {}, Start Memory: {}MB", 
            sseConnectionsTotalCounter.count(), 
            activeConnectionsCount.get(),
            startMemory / 1024 / 1024);
        
        return connectionCreationTimer;
    }
    
    /**
     * SSE 커넥션 종료 시 메트릭 기록
     */
    private void endSSEConnectionTracking(Timer.Sample connectionCreationTimer, Timer.Sample lifetimeTimer, String reason, String connectionId) {
        try {
            // 커넥션 생성 시간 기록
            connectionCreationTimer.stop(sseConnectionCreationTimer);
            
            // 커넥션 생명주기 시간 기록
            lifetimeTimer.stop(sseConnectionLifetimeTimer);
            
            // 연결별 실제 메모리 사용량 계산
            Runtime runtime = Runtime.getRuntime();
            long endMemory = runtime.totalMemory() - runtime.freeMemory();
            Long startMemory = connectionMemoryMap.remove(connectionId);
            
            // 활성 커넥션 수 감소 (중복 감소 방지)
            int activeConnections;
            if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
                // 첫 번째 종료만 처리
                activeConnections = activeConnectionsCount.decrementAndGet();
            } else {
                // 이미 종료된 연결이므로 현재 값만 가져옴
                activeConnections = activeConnectionsCount.get();
            }
            
            if (startMemory != null) {
                // 연결 시작과 종료 시점의 메모리 차이로 실제 사용량 계산
                long actualMemoryUsed = Math.max(0, endMemory - startMemory);
                sseConnectionMemoryUsageSummary.record(actualMemoryUsed);
                
                log.info("SSE connection ended. Reason: {}, Connection Memory Used: {}KB, Active: {}", 
                    reason, actualMemoryUsed / 1024, activeConnections);
            } else {
                log.warn("SSE connection start memory not found for connection: {}, Active: {}", 
                    connectionId, activeConnections);
            }
            
            // 종료 이유별 카운터 증가
            switch (reason) {
                case "timeout":
                    sseConnectionTimeoutCounter.increment();
                    break;
                case "error":
                    sseConnectionErrorCounter.increment();
                    break;
                case "completion":
                default:
                    // 정상 완료는 별도 카운터 없음
                    break;
            }
                
        } catch (Exception e) {
            log.warn("Failed to record SSE connection end metrics: {}", e.getMessage());
        }
    }
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
                                                      "message": "소개팅에 어울리는 스타일을 분석해보겠습니다. 체형과 핏감을 중심으로 추천해드려요!",
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


    @Operation(
            summary = "채팅 메시지 스트리밍 (SSE) - 통합 스트림 API",
            description = """
                    사용자 입력을 기반으로 채팅방에서 실시간 AI 응답을 스트리밍으로 전달합니다.
                    room_id가 있으면 기존 방을 사용하고, 없으면 새 방을 자동 생성합니다.
                    
                    **주요 특징:**
                    - 유연한 방 관리: room_id 파라미터로 기존 방 사용 또는 새 방 생성
                    - 실시간 스트리밍: AI 응답이 생성되는 대로 즉시 전송
                    - 다중 전문가: 3명의 전문가가 순차적으로 응답 생성
                    - 상품 추천: 완료 시 관련 상품 이미지와 정보 제공
                    - 자동 저장: 사용자 메시지와 AI 응답을 PostgreSQL에 자동 저장
                    - 명시적 종료: 모든 전문가 완료 후 SSE 연결 자동 종료
                    
                    **이벤트 타입:**
                    - `connect`: SSE 연결 성공
                    - `content`: AI 응답 실시간 스트리밍 (각 전문가별)
                    - `complete`: 개별 전문가 완료 (각 전문가별)
                    - `final_complete`: 모든 전문가 완료 및 연결 종료
                    - `error`: 에러 발생 시
                    
                    **사용 예시:**
                    ```javascript
                    // 기존 방 사용
                    const eventSource1 = new EventSource('/api/chat/rooms/messages/stream?room_id=123&user_input=안녕하세요');
                    
                    // 새 방 생성
                    const eventSource2 = new EventSource('/api/chat/rooms/messages/stream?user_input=안녕하세요');
                    
                    eventSource.addEventListener('final_complete', (event) => {
                        console.log('모든 전문가 완료, 연결 종료');
                        eventSource.close();
                    });
                    ```
                    """,
            parameters = {
                    @Parameter(
                            name = "room_id", 
                            description = "기존 방 ID (선택사항 - 없으면 자동 생성)", 
                            required = false, 
                            schema = @Schema(type = "integer", format = "int64"),
                            example = "259"
                    ),
                    @Parameter(
                            name = "user_input", 
                            description = "사용자 입력 텍스트 (패션 상담 요청)", 
                            required = true, 
                            schema = @Schema(type = "string"),
                            example = "소개팅 가는데 입을 옷 추천해줘"
                    ),
                    @Parameter(
                            name = "user_profile", 
                            description = "사용자 프로필 정보 (선택사항)", 
                            required = false, 
                            schema = @Schema(type = "string"),
                            example = "20대 남성, 키 175cm, 체형 보통"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200", 
                            description = "SSE 연결 성공 - 실시간 스트림 시작", 
                            content = @Content(
                                    mediaType = "text/event-stream",
                                    examples = {
                                            @ExampleObject(
                                                    name = "방 정보 전송",
                                                    summary = "자동 생성된 방 정보",
                                                    value = """
                                                    event: room
                                                    data: {"status":"success","message":"요청 성공","data":{"room_id":259,"type":"room","timestamp":1757045016039}}
                                                    
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "연결 성공",
                                                    summary = "SSE 연결이 성공적으로 설정됨",
                                                    value = """
                                                    event: connect
                                                    data: {"status":"success","message":"요청 성공","data":{"message":"SSE 연결 성공","type":"connect","timestamp":1757045016039}}
                                                    
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "AI 응답 스트리밍",
                                                    summary = "실시간 AI 응답 메시지",
                                                    value = """
                                                    event: content
                                                    data: {"status":"success","message":"요청 성공","data":{"agent_id":"style_analyst","agent_name":"스타일 분석가","message":"소개팅에 어울리는 스타일을 분석해보겠습니다...","type":"content","timestamp":1757045028619}}
                                                    
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "완료 및 상품 추천",
                                                    summary = "최종 완료 메시지와 추천 상품",
                                                    value = """
                                                    event: complete
                                                    data: {"status":"success","message":"요청 성공","data":{"agent_id":"style_analyst","agent_name":"스타일 분석가","message":"브라운 린넨 반팔 셔츠에 그레이 와이드 슬랙스가 잘 어울려...","products":[{"product_url":"https://sw-fashion-image-data.s3.amazonaws.com/TOP/1002/4989731/segment/4989731_seg_001.jpg","product_id":"4989731"}]}}
                                                    
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "에러 발생",
                                                    summary = "처리 중 오류 발생",
                                                    value = """
                                                    event: error
                                                    data: {"status":"fail","message":"스트림 처리 오류: [에러 메시지]","data":null}
                                                    
                                                    """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400", 
                            description = "요청 파라미터 오류 (user_input 누락 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "파라미터 오류",
                                            summary = "필수 파라미터가 누락된 경우",
                                            value = """
                                            {
                                              "status": "fail",
                                              "message": "user_input은 필수 파라미터입니다.",
                                              "data": null
                                            }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500", 
                            description = "서버 내부 오류 (방 생성 실패, 외부 API 호출 실패, DB 저장 실패 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "서버 오류",
                                            summary = "예상치 못한 서버 오류 발생",
                                            value = """
                                            {
                                              "status": "fail",
                                              "message": "스트림 처리 중 오류가 발생했습니다: [오류 메시지]",
                                              "data": null
                                            }
                                            """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/rooms/messages/stream")
    public SseEmitter streamChatMessage(
            @RequestParam(value = "room_id", required = false) Long roomId,
            @RequestParam("user_input") String userInput,
            @RequestParam(value = "user_profile", required = false) String userProfile,
            HttpServletRequest httpRequest
    ) {
    // 1. 세션 처리
    HttpSession session = httpRequest.getSession(false);
    if (session == null) {
        session = httpRequest.getSession(true);
    }
    final HttpSession finalSession = session;

    // 2. 방 ID 결정 (roomId가 없으면 새 방 생성)
    final Long finalRoomId;
    if (roomId == null) {
        try {
            finalRoomId = chatRoomManagementService.createChatRoom(finalSession.getId());
            log.info("새 채팅방 생성됨: roomId={}, sessionId={}", finalRoomId, finalSession.getId());
        } catch (Exception e) {
            log.error("채팅방 생성 실패: sessionId={}, error={}", finalSession.getId(), e.getMessage(), e);
            // 에러 응답 반환
            SseEmitter errorEmitter = new SseEmitter(1000L);
            try {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("message", "채팅방 생성에 실패했습니다.");
                errorData.put("error", e.getMessage());
                CommonResponse errorResponse = CommonResponse.fail("채팅방 생성에 실패했습니다: " + e.getMessage());
                String errorJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
                errorEmitter.send(SseEmitter.event().name("error").data(errorJson));
                errorEmitter.complete();
            } catch (Exception ex) {
                log.error("에러 응답 전송 실패", ex);
            }
            return errorEmitter;
        }
    } else {
        finalRoomId = roomId;
    }

    final SseEmitter emitter = new SseEmitter(120000L);
    final AtomicBoolean cancelled = new AtomicBoolean(false);
    
    // SSE 커넥션 추적 시작
    String connectionId = "sse_" + System.currentTimeMillis() + "_" + Thread.currentThread().hashCode();
    Timer.Sample connectionCreationTimer = startSSEConnectionTracking(connectionId);
    Timer.Sample connectionLifetimeTimer = Timer.start();
    
    // SSE API 전체 응답 시간 측정 시작
    sseApiTotalCounter.increment();
    Timer.Sample totalResponseTimer = Timer.start();
    
    // SSE 연결 메트릭
    sseConnectionCounter.increment();
    Timer.Sample connectionTimer = Timer.start();
    
    // 타임아웃 기반 강제 종료를 위한 타이머
    final AtomicBoolean forceCompleted = new AtomicBoolean(false);
    CompletableFuture.delayedExecutor(120, TimeUnit.SECONDS).execute(() -> {
        if (!forceCompleted.get()) {
            log.warn("⏰ SSE 연결 강제 타임아웃: connectionId={}", connectionId);
            cancelled.set(true);
            
            // 강제 종료 시 메트릭 업데이트 (중복 방지)
            if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
                connectionTimer.stop(sseConnectionDurationTimer);
                totalResponseTimer.stop(sseApiTotalResponseTimer);
                sseApiFailureCounter.increment(); // 타임아웃은 실패로 간주
                sseDisconnectionCounter.increment();
                endSSEConnectionTracking(connectionCreationTimer, connectionLifetimeTimer, "force_timeout", connectionId);
            }
            
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("강제 종료 중 오류: {}", e.getMessage());
            }
        }
    });

    // SSE 수명주기 훅: 연결 종료/타임아웃/에러 시 취소 플래그 설정
    emitter.onCompletion(() -> {
        cancelled.set(true);
        // 중복 처리 방지
        if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
            connectionTimer.stop(sseConnectionDurationTimer);
            totalResponseTimer.stop(sseApiTotalResponseTimer);
            sseApiSuccessCounter.increment();
            sseDisconnectionCounter.increment();
            endSSEConnectionTracking(connectionCreationTimer, connectionLifetimeTimer, "completion", connectionId);
        }
    });
    emitter.onTimeout(() -> {
        cancelled.set(true);
        // 중복 처리 방지
        if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
            connectionTimer.stop(sseConnectionDurationTimer);
            totalResponseTimer.stop(sseApiTotalResponseTimer);
            sseApiFailureCounter.increment();
            sseDisconnectionCounter.increment();
            endSSEConnectionTracking(connectionCreationTimer, connectionLifetimeTimer, "timeout", connectionId);
        }
    });
    emitter.onError(e -> {
        cancelled.set(true);
        // 중복 처리 방지
        if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
            connectionTimer.stop(sseConnectionDurationTimer);
            totalResponseTimer.stop(sseApiTotalResponseTimer);
            sseApiFailureCounter.increment();
            sseDisconnectionCounter.increment();
            endSSEConnectionTracking(connectionCreationTimer, connectionLifetimeTimer, "error", connectionId);
        }
    });

    // 통합된 스트림 처리 로직

    try {
        // connect 이벤트를 CommonResponse 형식으로 변경
        Map<String, Object> connectData = new HashMap<>();
        connectData.put("message", "SSE 연결 성공");
        connectData.put("type", "connect");
        connectData.put("timestamp", System.currentTimeMillis());
        
        CommonResponse connectResponse = CommonResponse.success(connectData);
        String json = OBJECT_MAPPER.writeValueAsString(connectResponse);
        emitter.send(SseEmitter.event().name("connect").data(json));
    } catch (IOException e) {
        log.warn("초기 SSE 메시지 전송 실패", e);
    }

    java.util.concurrent.CompletableFuture.runAsync(() -> {
        try {
            // 메모리 사용량 측정 시작
            recordMemoryUsage();
            
            // 사용자 메시지를 먼저 DB에 저장 (세션 ID 기반으로 사용자 생성/조회)
            try {
                log.info("사용자 메시지 저장 시작: roomId={}, userInput='{}', sessionId='{}'", finalRoomId, userInput, finalSession.getId());
                
                // 세션 ID 기반으로 사용자 생성/조회 (send API와 동일한 로직)
                UserEntity userEntity = userSessionService.getOrCreateGuestUser(finalSession.getId());
                log.info("세션 기반 사용자 엔티티 조회/생성 완료: userEntity={}, userId={}", userEntity, userEntity != null ? userEntity.getId() : "null");
                
                if (userEntity == null) {
                    log.error("사용자 엔티티가 null입니다. sessionId={}", finalSession.getId());
                    return;
                }
                
                ChatMessageRequest userMessageRequest = new ChatMessageRequest();
                userMessageRequest.setContent(userInput);
                userMessageRequest.setImageUrl(null); // 스트림 API에서는 이미지 없음
                
                chatMessageService.saveUserMessage(userEntity, userMessageRequest, finalRoomId);
                log.info("스트림 API 사용자 메시지를 데이터베이스에 저장했습니다. roomId={}, message='{}'", finalRoomId, userInput);
            } catch (Exception e) {
                log.error("스트림 API 사용자 메시지 저장 실패: roomId={}, sessionId={}, error={}", finalRoomId, finalSession.getId(), e.getMessage(), e);
            }
            
            final String externalApiUrl = llmExpertStreamUrl;

            java.util.List<String> expertList = new java.util.ArrayList<>();
            expertList.add("style_analyst");
            expertList.add("color_expert");
            expertList.add("fitting_coordinator");

            // 전문가 완료 상태 추적을 위한 Map
            java.util.Map<String, Boolean> expertCompleted = new java.util.concurrent.ConcurrentHashMap<>();
            for (String expert : expertList) {
                expertCompleted.put(expert, false);
            }

            for (String curExpert : expertList) {
                if (cancelled.get()) break;
                
                // 각 전문가 처리 전 메모리 측정
                recordMemoryUsage();
                
                Map<String, Object> expertRequest = new HashMap<>();
                expertRequest.put("user_input", userInput);
                expertRequest.put("expert_type", curExpert);
                expertRequest.put("room_id", finalRoomId);

                // user_profile 포함 (있는 경우)
                if (userProfile != null && !userProfile.trim().isEmpty()) {
                    Map<String, Object> userProfileMap = new HashMap<>();
                    userProfileMap.put("profile_text", userProfile);
                    expertRequest.put("user_profile", userProfileMap);
                }

                expertRequest.put("context_info", null);
                expertRequest.put("json_data", null);

                if (cancelled.get()) break;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(expertRequest, headers);

                if (cancelled.get()) break;
                
                // LLM API 호출 메트릭 (전문가별)
                llmApiCallCounter.increment();
                llmApiCallCounterByExpert.increment();
                Timer.Sample llmTimer = Timer.start();
                
                ResponseEntity<String> response = restTemplate.exchange(
                        externalApiUrl,
                        HttpMethod.POST,
                        entity,
                        String.class
                );
                
                llmTimer.stop(llmApiResponseTimer);
                
                // 상태 코드별 메트릭 추가
                Counter.builder("llm_api_status_code_total")
                        .description("LLM API calls by HTTP status code")
                        .tag("expert_type", curExpert)
                        .tag("status_code", String.valueOf(response.getStatusCode().value()))
                        .register(meterRegistry)
                        .increment();
                
                // 응답 크기 메트릭 추가
                if (response.getBody() != null) {
                    DistributionSummary.builder("llm_api_response_size")
                            .description("LLM API response size in bytes")
                            .baseUnit("bytes")
                            .tag("expert_type", curExpert)
                            .register(meterRegistry)
                            .record(response.getBody().length());
                }

                StringBuilder finalText = new StringBuilder();

                if (response.getStatusCode() == HttpStatus.OK) {
                    llmApiSuccessCounter.increment();
                    String body = response.getBody();
                    if (body != null && body.contains("data:")) {
                        String[] lines = body.split("\n");
                        for (String line : lines) {
                            if (cancelled.get()) break;
                            if (!line.startsWith("data:")) continue;
                            String jsonData = line.substring(5).trim();
                            if (jsonData.isEmpty()) continue;
                            try {
                                ObjectMapper mapper = OBJECT_MAPPER;
                                @SuppressWarnings("unchecked")
                                Map<String, Object> parsed = mapper.readValue(jsonData, Map.class);
                                Object type = parsed.get("type");
                                if ("content".equals(type) && parsed.containsKey("chunk")) {
                                    String chunk = String.valueOf(parsed.get("chunk"));
                                    finalText.append(chunk);
                                    // 청크를 즉시 전송 (CommonResponse 형식으로)
                                    if (cancelled.get()) break;
                                    Map<String, Object> contentPayload = new HashMap<>();
                                    contentPayload.put("message", chunk);
                                    contentPayload.put("agent_id", curExpert);
                                    contentPayload.put("agent_name", getAgentName(curExpert));
                                    contentPayload.put("type", "content");
                                    contentPayload.put("timestamp", System.currentTimeMillis());
                                    
                                    CommonResponse contentResponse = CommonResponse.success(contentPayload);
                                    String json = OBJECT_MAPPER.writeValueAsString(contentResponse);
                                    emitter.send(SseEmitter.event().name("content").data(json));
                                    try { Thread.sleep(100); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                                }
                            } catch (Exception ignore) {
                            }
                        }
                    }
                } else {
                    llmApiFailureCounter.increment();
                    if (!cancelled.get()) {
                        sendEvent(emitter, "error", "외부 API 호출 실패: " + response.getStatusCode(), 3, curExpert);
                    }
                }

                if (cancelled.get()) break;
                
                Map<String, Object> completePayload = new HashMap<>();
                String finalMessage = finalText.toString();
                completePayload.put("message", finalMessage);
                completePayload.put("agent_id", curExpert);
                completePayload.put("agent_name", getAgentName(curExpert));

                // 변수를 미리 선언 (스코프 문제 해결)
                java.util.List<String> productImageUrls = new java.util.ArrayList<>();
                java.util.List<String> productIds = new java.util.ArrayList<>();
                java.util.List<com.thefirsttake.app.chat.dto.response.ProductInfo> products = new java.util.ArrayList<>();

                try {
                    if (!cancelled.get()) {
                        // 상품 검색 API 메트릭
                        productSearchApiCallCounter.increment();
                        Timer.Sample productTimer = Timer.start();
                        
                        Map<String, Object> searchResult = productSearchService.searchProducts(finalMessage);
                        productTimer.stop(productSearchApiResponseTimer);
                        
                        if (searchResult != null) {
                            productSearchApiSuccessCounter.increment();
                            try {
                                productCacheService.cacheProductsFromSearchResult(searchResult);
                            } catch (Exception cacheErr) {
                                log.warn("상품 캐싱 오류: {}", cacheErr.getMessage());
                            }
                            productImageUrls = productSearchService.extractProductImageUrls(searchResult);
                            productIds = productCacheService.extractProductIds(searchResult);
                            
                            // product URL을 Redis에 저장 (600분 만료)
                            // 인코딩/디코딩 없이 원본 presigned URL 그대로 저장
                            for (int i = 0; i < productIds.size() && i < productImageUrls.size(); i++) {
                                try {
                                    String productId = productIds.get(i);
                                    String productUrl = productImageUrls.get(i);
                                    if (productId != null && productUrl != null && !productId.trim().isEmpty() && !productUrl.trim().isEmpty()) {
                                        String redisKey = "product_url_" + productId.trim();
                                        try {
                                            redisTemplate.opsForValue().set(redisKey, productUrl.trim(), 36000, java.util.concurrent.TimeUnit.SECONDS);
                                            log.info("Product URL saved to Redis (raw): key={}, length(original)={}", 
                                                redisKey, productUrl.trim().length());
                                        } catch (Exception encodingException) {
                                            // 실패 시 원본 URL 직접 저장 시도
                                            redisTemplate.opsForValue().set(redisKey, productUrl.trim(), 36000, java.util.concurrent.TimeUnit.SECONDS);
                                            log.warn("Saving raw URL with fallback: key={}, error={}", redisKey, encodingException.getMessage());
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("Failed to save product URL to Redis from search result: productId={}, productUrl={}, error={}", 
                                        productIds.get(i), productImageUrls.get(i), e.getMessage());
                                }
                            }
                            
                            int minSize = Math.min(productImageUrls.size(), productIds.size());
                            for (int i = 0; i < minSize; i++) {
                                com.thefirsttake.app.chat.dto.response.ProductInfo productInfo =
                                        com.thefirsttake.app.chat.dto.response.ProductInfo.builder()
                                                .productUrl(productImageUrls.get(i))
                                                .productId(productIds.get(i))
                                                .build();
                                products.add(productInfo);
                            }
                            completePayload.put("products", products);
                        } else {
                            productSearchApiFailureCounter.increment();
                        }
                        
                        // 상품 정보가 있는 경우 DB에 저장
                        if (!productImageUrls.isEmpty() || !productIds.isEmpty()) {
                            try {
                                log.info("AI 응답 저장 시작: agent={}, roomId={}", curExpert, finalRoomId);
                                UserEntity userEntity = userSessionService.getOrCreateGuestUser(finalSession.getId());
                                ChatRoom chatRoom = chatRoomManagementService.getRoomById(finalRoomId);
                                log.info("AI 응답 저장용 엔티티 조회 완료: userEntity={}, chatRoom={}", 
                                        userEntity != null ? userEntity.getId() : "null", 
                                        chatRoom != null ? chatRoom.getId() : "null");
                                
                                if (userEntity == null || chatRoom == null) {
                                    log.error("필수 엔티티가 null입니다. userEntity={}, chatRoom={}", userEntity, chatRoom);
                                    return;
                                }
                                
                                // ChatAgentResponse 객체 생성
                                ChatAgentResponse agentResponse = new ChatAgentResponse();
                                agentResponse.setAgentId(curExpert);
                                agentResponse.setMessage(finalMessage);
                                agentResponse.setProducts(products);
                                
                                // DB에 저장
                                chatMessageService.saveAIResponse(userEntity, chatRoom, agentResponse);
                                log.info("스트림 완료 시 AI 응답을 데이터베이스에 저장했습니다. agent={}, roomId={}", curExpert, finalRoomId);
                            } catch (Exception dbErr) {
                                log.error("스트림 완료 시 DB 저장 실패: agent={}, roomId={}, error={}", curExpert, finalRoomId, dbErr.getMessage(), dbErr);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("상품 검색 처리 중 오류: {}", e.getMessage());
                }

                if (!cancelled.get()) {
                    // 전문가 완료 상태 업데이트
                    expertCompleted.put(curExpert, true);
                    
                    // complete 이벤트를 CommonResponse 형식으로 변경 (SSE 이벤트 자체를 CommonResponse로)
                    CommonResponse completeResponse = CommonResponse.success(completePayload);
                    String json = OBJECT_MAPPER.writeValueAsString(completeResponse);
                    emitter.send(SseEmitter.event().name("complete").data(json));
                    
                    // 모든 전문가 완료 확인
                    boolean allExpertsCompleted = expertCompleted.values().stream().allMatch(Boolean::booleanValue);
                    if (allExpertsCompleted) {
                        log.info("🎉 모든 전문가 응답 완료 - SSE 연결 종료: roomId={}", finalRoomId);
                        try {
                            // 최종 완료 메시지 전송
                            Map<String, Object> finalCompleteMessage2 = new HashMap<>();
                            finalCompleteMessage2.put("message", "모든 전문가 응답이 완료되었습니다.");
                            finalCompleteMessage2.put("total_experts", expertList.size());
                            finalCompleteMessage2.put("timestamp", System.currentTimeMillis());
                            
                            CommonResponse finalResponse = CommonResponse.success(finalCompleteMessage2);
                            String finalJson = OBJECT_MAPPER.writeValueAsString(finalResponse);
                            emitter.send(SseEmitter.event().name("final_complete").data(finalJson));
                            
                            // 명시적 SSE 연결 종료
                            forceCompleted.set(true);
                            emitter.complete();
                            return; // 루프 종료
                        } catch (Exception e) {
                            log.error("SSE 최종 완료 처리 실패: roomId={}, error={}", finalRoomId, e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            try {
                if (!cancelled.get()) {
                    sendEvent(emitter, "error", "스트림 처리 오류: " + e.getMessage(), -1, null);
                }
            } catch (IOException ignored) {}
        } finally {
            try { 
                forceCompleted.set(true);
                // 중복 완료 방지
                if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
                    // 이미 다른 곳에서 처리되지 않은 경우에만 처리
                    log.warn("SSE 연결이 finally 블록에서 종료됨: connectionId={}", connectionId);
                }
                emitter.complete(); 
            } catch (Exception ignore) {}
        }
    });

    return emitter;
}

private void sendEvent(SseEmitter emitter, String type, Object data, int step, String agentId) throws IOException {
    Map<String, Object> message = new HashMap<>();
    message.put("type", type);
    message.put("data", data);
    message.put("timestamp", System.currentTimeMillis());
    if (agentId != null) {
        message.put("agent_id", agentId);
    }
    String json = OBJECT_MAPPER.writeValueAsString(message);
    emitter.send(SseEmitter.event().name(type).data(json));
}

private String getAgentName(String agentId) {
    switch (agentId) {
        case "style_analyst":
            return "스타일 분석가";
        case "color_expert":
            return "컬러 전문가";
        case "fitting_coordinator":
            return "피팅 코디네이터";
        default:
            return agentId != null ? agentId : "알 수 없음";
    }
}

    @Operation(
        summary = "새 채팅방 생성",
        description = """
            새로운 채팅방을 생성합니다.
            
            **인증 방식:**
            - 세션 기반 사용자 식별 (다른 채팅 API들과 동일)
            - 세션이 없으면 자동으로 새 세션 생성
            - 게스트 사용자로 채팅방 생성
            
            **프론트엔드에서 사용하는 방법:**
            ```javascript
            async function createChatRoom(title = "새로운 채팅방") {
                const response = await fetch('/api/chat/rooms', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    credentials: 'include', // 세션 쿠키 포함 필수
                    body: JSON.stringify({ title: title })
                });
                
                const result = await response.json();
                if (response.ok && result.status === 'success') {
                    const roomData = result.data;
                    console.log('새 채팅방 생성됨:', roomData);
                    return roomData;
                } else {
                    console.error('채팅방 생성 실패:', result.message);
                    return null;
                }
            }
            ```
            """,
        tags = {"채팅 관리"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "채팅방 생성 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    description = "새로 생성된 채팅방 정보를 반환합니다.",
                    value = """
                    {
                        "status": "success",
                        "message": "채팅방이 성공적으로 생성되었습니다",
                        "data": {
                            "id": 3,
                            "title": "새로운 채팅방",
                            "createdAt": "2024-01-15T14:30:00"
                        }
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
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "서버 오류 응답",
                    description = "채팅방 생성 중 서버 오류가 발생했습니다.",
                    value = """
                    {
                        "status": "fail",
                        "message": "채팅방 생성 중 오류가 발생했습니다",
                        "data": null
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/rooms")
    public ResponseEntity<CommonResponse> createChatRoom(
        HttpServletRequest httpRequest
    ) {
        try {
            // 세션 확인 (다른 API들과 동일한 패턴)
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                System.out.println("createChatRoom: 세션 새로 생성");
                session = httpRequest.getSession(true);
            }
            
            String sessionId = session.getId();
            log.info("채팅방 생성 요청: sessionId={}", sessionId);
            
            // 사용자 엔티티 조회 또는 생성 (게스트 사용자)
            UserEntity userEntity = userSessionService.getOrCreateGuestUser(sessionId);
            
            // 새 채팅방 생성
            ChatRoom newRoom = new ChatRoom();
            newRoom.setUser(userEntity);
            newRoom.setTitle("새로운 채팅방");
            newRoom.setCreatedAt(LocalDateTime.now());
            
            ChatRoom savedRoom = chatRoomManagementService.getChatRoomRepository().save(newRoom);
            
            // DTO로 변환하여 응답
            ChatRoomDto roomDto = new ChatRoomDto(savedRoom);
            
            log.info("새 채팅방 생성 완료: roomId={}, sessionId={}, title={}", 
                savedRoom.getId(), sessionId, savedRoom.getTitle());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(roomDto));
                
        } catch (Exception e) {
            log.error("채팅방 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.fail("채팅방 생성 중 오류가 발생했습니다"));
        }
    }
    
}

