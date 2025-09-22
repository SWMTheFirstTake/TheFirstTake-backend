package com.thefirsttake.app.chat.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 채팅 스트림 처리를 전체적으로 조율하는 오케스트레이터 서비스
 * - 전체 스트림 프로세스 조율
 * - 서비스 간 의존성 관리
 * - 에러 핸들링 및 복구
 * - 비동기 처리 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatStreamOrchestrationService {
    
    private final SSEConnectionService sseConnectionService;
    private final ExpertStreamService expertStreamService;
    private final MessageStorageService messageStorageService;
    private final StreamMetricsService streamMetricsService;
    
    /**
     * 스트림 채팅 처리 메인 메서드
     * @param userInput 사용자 입력
     * @param userProfile 사용자 프로필
     * @param roomId 방 ID
     * @param isNewRoom 신규 방 생성 여부
     * @param session 세션
     * @return SSE 에미터
     */
    public SseEmitter processStreamChat(String userInput, String userProfile, String roomId, boolean isNewRoom, HttpSession session) {
        
        // 연결 ID 생성
        String connectionId = generateConnectionId(session);
        
        // SSE 에미터 생성 및 초기화
        SseEmitter emitter = new SseEmitter(300000L);
        
        // 연결 상태 추적
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicBoolean forceCompleted = new AtomicBoolean(false);
        
        // 최종 방 ID 결정 (이미 ChatController에서 결정됨)
        String finalRoomId = roomId;
        
        log.info("🚀 스트림 채팅 처리 시작: connectionId={}, roomId={}, finalRoomId={}", 
                connectionId, roomId, finalRoomId);
        
        try {
            // SSE 연결 초기화 (신규 방 생성 여부를 전달)
            sseConnectionService.initializeConnection(connectionId, emitter, isNewRoom ? null : roomId, finalRoomId);
            
            // 비동기 스트림 처리 시작
            CompletableFuture.runAsync(() -> {
                try {
                    // 메모리 사용량 측정 시작
                    streamMetricsService.recordMemoryUsage(connectionId);
                    
                    // 사용자 메시지 저장
                    messageStorageService.saveUserMessage(session.getId(), userInput, finalRoomId);
                    
                    // 전문가 리스트 및 완료 상태 추적 준비
                    List<String> expertList = expertStreamService.getExpertList();
                    Map<String, Boolean> expertCompleted = expertStreamService.createExpertCompletedMap(expertList);
                    
                    // 각 전문가별 순차 처리
                    for (String curExpert : expertList) {
                        if (cancelled.get()) break;
                        
                        log.info("👨‍💼 전문가 처리 시작: expert={}, roomId={}", curExpert, finalRoomId);
                        
                        // 전문가 처리
                        ExpertStreamService.ExpertProcessResult result = expertStreamService.processExpert(
                                curExpert, userInput, userProfile, finalRoomId, session.getId(), emitter, cancelled
                        );
                        
                        if (cancelled.get()) break;
                        
                        // 전문가 완료 상태 업데이트
                        expertStreamService.markExpertCompleted(expertCompleted, curExpert);
                        
                        // 완료 이벤트 전송
                        expertStreamService.sendExpertCompleteEvent(
                                emitter, result.getMessage(), curExpert, result.getProducts()
                        );
                        
                        // 모든 전문가 완료 확인
                        if (expertStreamService.areAllExpertsCompleted(expertCompleted)) {
                            log.info("🎉 모든 전문가 응답 완료 - SSE 연결 종료: roomId={}", finalRoomId);
                            
                            // 최종 완료 이벤트 전송
                            sseConnectionService.sendFinalCompleteEvent(emitter, expertList.size());
                            
                            // SSE 연결 종료
                            forceCompleted.set(true);
                            sseConnectionService.completeConnection(connectionId, emitter, forceCompleted);
                            
                            // 연결 추적 정리
                            cleanupConnection(connectionId);
                            
                            return; // 루프 종료
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("스트림 처리 중 오류 발생: connectionId={}, error={}", connectionId, e.getMessage(), e);
                    
                    if (!cancelled.get()) {
                        sseConnectionService.sendErrorEvent(emitter, "스트림 처리 오류: " + e.getMessage(), null);
                    }
                } finally {
                    // 최종 정리
                    if (!forceCompleted.get()) {
                        forceCompleted.set(true);
                        sseConnectionService.completeConnection(connectionId, emitter, forceCompleted);
                        cleanupConnection(connectionId);
                    }
                }
            });
            
            // 연결 추적 설정
            setupConnectionTracking(connectionId, emitter, cancelled);
            
        } catch (Exception e) {
            log.error("스트림 채팅 초기화 실패: connectionId={}, error={}", connectionId, e.getMessage(), e);
            
            try {
                sseConnectionService.sendErrorEvent(emitter, "스트림 초기화 실패: " + e.getMessage(), null);
                sseConnectionService.completeConnection(connectionId, emitter, forceCompleted);
            } catch (Exception cleanupError) {
                log.error("에러 발생 시 정리 작업 실패: connectionId={}, error={}", connectionId, cleanupError.getMessage());
            }
        }
        
        return emitter;
    }
    
    /**
     * 연결 ID 생성
     */
    private String generateConnectionId(HttpSession session) {
        return "stream_" + session.getId() + "_" + System.currentTimeMillis();
    }
    
    /**
     * 방 ID 생성 (신규 생성 시)
     */
    private String generateRoomId() {
        return "room_" + System.currentTimeMillis() + "_" + Thread.currentThread().threadId();
    }
    
    /**
     * 연결 추적 설정
     */
    private void setupConnectionTracking(String connectionId, SseEmitter emitter, AtomicBoolean cancelled) {
        try {
            // SSE 추적 훅 설정 (현재는 기본 설정만)
            log.info("연결 추적 설정 완료: connectionId={}", connectionId);
            
        } catch (Exception e) {
            log.warn("연결 추적 설정 실패: connectionId={}, error={}", connectionId, e.getMessage());
        }
    }
    
    /**
     * 연결 정리
     */
    private void cleanupConnection(String connectionId) {
        try {
            // 메모리 사용량 추적 제거
            streamMetricsService.removeConnectionMemoryUsage(connectionId);
            
            log.info("연결 정리 완료: connectionId={}", connectionId);
            
        } catch (Exception e) {
            log.warn("연결 정리 실패: connectionId={}, error={}", connectionId, e.getMessage());
        }
    }
    
    /**
     * 스트림 처리 상태 조회
     */
    public boolean isStreamProcessing(String connectionId) {
        return !sseConnectionService.isConnectionEnded(connectionId);
    }
    
    /**
     * 스트림 처리 취소
     */
    public void cancelStreamProcessing(String connectionId) {
        try {
            sseConnectionService.setConnectionEnded(connectionId, true);
            log.info("스트림 처리 취소: connectionId={}", connectionId);
        } catch (Exception e) {
            log.error("스트림 처리 취소 실패: connectionId={}, error={}", connectionId, e.getMessage());
        }
    }
}
