package com.thefirsttake.app.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thefirsttake.app.common.response.CommonResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSE(Server-Sent Events) 연결 관리를 담당하는 서비스
 * - SSE 연결 초기화
 * - 이벤트 전송 (room, connect, content, complete, final_complete)
 * - 연결 상태 관리 및 종료 처리
 */
@Service
@Slf4j
public class SSEConnectionService {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // 메트릭 관련 의존성
    private final Counter sseConnectionCounter;
    private final Counter sseDisconnectionCounter;
    private final Timer sseConnectionDurationTimer;
    
    public SSEConnectionService(@Qualifier("sseConnectionCounter") Counter sseConnectionCounter,
                               @Qualifier("sseDisconnectionCounter") Counter sseDisconnectionCounter,
                               @Qualifier("sseConnectionDurationTimer") Timer sseConnectionDurationTimer) {
        this.sseConnectionCounter = sseConnectionCounter;
        this.sseDisconnectionCounter = sseDisconnectionCounter;
        this.sseConnectionDurationTimer = sseConnectionDurationTimer;
    }
    
    // SSE 연결 상태 추적을 위한 맵
    private final Map<String, Boolean> connectionEndedMap = new ConcurrentHashMap<>();
    
    /**
     * SSE 연결 초기화
     * @param connectionId 연결 식별자
     * @param emitter SSE 에미터
     * @param roomId 방 ID (신규 생성 시 null)
     * @param finalRoomId 최종 방 ID
     * @return 초기화된 SSE 에미터
     */
    public SseEmitter initializeConnection(String connectionId, SseEmitter emitter, String roomId, String finalRoomId) {
        try {
            // 신규 방 생성 시 room 이벤트 먼저 전송
            if (roomId == null) {
                log.info("신규 방 생성 감지 - room 이벤트 전송: roomId={}", finalRoomId);
                sendRoomEvent(emitter, finalRoomId);
            } else {
                log.info("기존 방 사용 - room 이벤트 생략: roomId={}", roomId);
            }
            
            // connect 이벤트 전송
            sendConnectEvent(emitter);
            
            // 연결 메트릭 증가
            sseConnectionCounter.increment();
            
            log.info("SSE 연결 초기화 완료: connectionId={}, roomId={}", connectionId, finalRoomId);
            
        } catch (IOException e) {
            log.warn("초기 SSE 메시지 전송 실패: connectionId={}, error={}", connectionId, e.getMessage(), e);
        }
        
        return emitter;
    }
    
    /**
     * room 이벤트 전송 (신규 방 생성 시)
     */
    private void sendRoomEvent(SseEmitter emitter, String finalRoomId) throws IOException {
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("room_id", finalRoomId);
        roomData.put("type", "room");
        roomData.put("timestamp", System.currentTimeMillis());
        
        CommonResponse roomResponse = CommonResponse.success(roomData);
        String roomJson = OBJECT_MAPPER.writeValueAsString(roomResponse);
        emitter.send(SseEmitter.event().name("room").data(roomJson));
    }
    
    /**
     * connect 이벤트 전송
     */
    private void sendConnectEvent(SseEmitter emitter) throws IOException {
        Map<String, Object> connectData = new HashMap<>();
        connectData.put("message", "SSE 연결 성공");
        connectData.put("type", "connect");
        connectData.put("timestamp", System.currentTimeMillis());
        
        CommonResponse connectResponse = CommonResponse.success(connectData);
        String json = OBJECT_MAPPER.writeValueAsString(connectResponse);
        emitter.send(SseEmitter.event().name("connect").data(json));
    }
    
    /**
     * content 이벤트 전송 (실시간 스트림)
     */
    public void sendContentEvent(SseEmitter emitter, String chunk, String agentId, String agentName) {
        try {
            Map<String, Object> contentPayload = new HashMap<>();
            contentPayload.put("message", chunk);
            contentPayload.put("agent_id", agentId);
            contentPayload.put("agent_name", agentName);
            contentPayload.put("type", "content");
            contentPayload.put("timestamp", System.currentTimeMillis());
            
            CommonResponse contentResponse = CommonResponse.success(contentPayload);
            String json = OBJECT_MAPPER.writeValueAsString(contentResponse);
            emitter.send(SseEmitter.event().name("content").data(json));
            
        } catch (IOException e) {
            log.warn("content 이벤트 전송 실패: agentId={}, chunk={}, error={}", agentId, chunk, e.getMessage());
        }
    }
    
    /**
     * complete 이벤트 전송 (전문가 완료)
     */
    public void sendCompleteEvent(SseEmitter emitter, String message, String agentId, String agentName, Object products) {
        try {
            Map<String, Object> completePayload = new HashMap<>();
            completePayload.put("message", message);
            completePayload.put("agent_id", agentId);
            completePayload.put("agent_name", agentName);
            completePayload.put("products", products);
            completePayload.put("type", "complete");
            completePayload.put("timestamp", System.currentTimeMillis());
            
            CommonResponse completeResponse = CommonResponse.success(completePayload);
            String json = OBJECT_MAPPER.writeValueAsString(completeResponse);
            emitter.send(SseEmitter.event().name("complete").data(json));
            
        } catch (IOException e) {
            log.warn("complete 이벤트 전송 실패: agentId={}, message={}, error={}", agentId, message, e.getMessage());
        }
    }
    
    /**
     * final_complete 이벤트 전송 (모든 전문가 완료)
     */
    public void sendFinalCompleteEvent(SseEmitter emitter, int totalExperts) {
        try {
            Map<String, Object> finalCompleteMessage = new HashMap<>();
            finalCompleteMessage.put("message", "모든 전문가 응답이 완료되었습니다.");
            finalCompleteMessage.put("total_experts", totalExperts);
            finalCompleteMessage.put("type", "final_complete");
            finalCompleteMessage.put("timestamp", System.currentTimeMillis());
            
            CommonResponse finalResponse = CommonResponse.success(finalCompleteMessage);
            String finalJson = OBJECT_MAPPER.writeValueAsString(finalResponse);
            emitter.send(SseEmitter.event().name("final_complete").data(finalJson));
            
        } catch (IOException e) {
            log.warn("final_complete 이벤트 전송 실패: totalExperts={}, error={}", totalExperts, e.getMessage());
        }
    }
    
    /**
     * error 이벤트 전송
     */
    public void sendErrorEvent(SseEmitter emitter, String errorMessage, String agentId) {
        try {
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("message", errorMessage);
            errorPayload.put("agent_id", agentId);
            errorPayload.put("type", "error");
            errorPayload.put("timestamp", System.currentTimeMillis());
            
            CommonResponse errorResponse = CommonResponse.success(errorPayload);
            String json = OBJECT_MAPPER.writeValueAsString(errorResponse);
            emitter.send(SseEmitter.event().name("error").data(json));
            
        } catch (IOException e) {
            log.warn("error 이벤트 전송 실패: agentId={}, errorMessage={}, error={}", agentId, errorMessage, e.getMessage());
        }
    }
    
    /**
     * SSE 연결 완료 처리
     */
    public void completeConnection(String connectionId, SseEmitter emitter, AtomicBoolean forceCompleted) {
        try {
            forceCompleted.set(true);
            
            // 중복 완료 방지
            if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
                log.info("SSE 연결 완료: connectionId={}", connectionId);
                emitter.complete();
                
                // 연결 해제 메트릭 증가
                sseDisconnectionCounter.increment();
            } else {
                log.warn("SSE 연결이 이미 종료됨: connectionId={}", connectionId);
            }
            
        } catch (Exception e) {
            log.error("SSE 연결 완료 처리 실패: connectionId={}, error={}", connectionId, e.getMessage(), e);
        }
    }
    
    /**
     * 연결 상태 확인
     */
    public boolean isConnectionEnded(String connectionId) {
        return connectionEndedMap.getOrDefault(connectionId, false);
    }
    
    /**
     * 연결 상태 설정
     */
    public void setConnectionEnded(String connectionId, boolean ended) {
        connectionEndedMap.put(connectionId, ended);
    }
}
