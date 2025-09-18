package com.thefirsttake.app.chat.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSE 연결 정보를 담는 DTO
 */
@Data
@Builder
public class SseConnection {
    private final SseEmitter emitter;
    private final AtomicBoolean cancelled;
    private final String connectionId;
    private final long startTime;
    
    public SseConnection(SseEmitter emitter, AtomicBoolean cancelled, String connectionId, long startTime) {
        this.emitter = emitter;
        this.cancelled = cancelled;
        this.connectionId = connectionId;
        this.startTime = startTime;
    }
    
    /**
     * 연결이 취소되었는지 확인
     */
    public boolean isCancelled() {
        return cancelled.get();
    }
    
    /**
     * 연결 취소
     */
    public void cancel() {
        cancelled.set(true);
    }
    
    /**
     * 연결 지속 시간 계산 (밀리초)
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
}
