package com.thefirsttake.app.chat.sse;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseInitializer {
    private final Counter sseConnectionCounter;
    private final Counter sseDisconnectionCounter;
    private final Timer sseConnectionDurationTimer;
    private final Counter sseApiTotalCounter;
    private final Timer sseApiTotalResponseTimer;
    // 내부 관리용 맵 (빈 주입 아님)
    private final ConcurrentHashMap<String, Boolean> connectionEndedMap = new ConcurrentHashMap<>();

    public Result initialize(SseTrackingHooks hooks) {
        final SseEmitter emitter = new SseEmitter(120000L);
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final AtomicBoolean forceCompleted = new AtomicBoolean(false);

        final String connectionId = "sse_" + System.currentTimeMillis() + "_" + Thread.currentThread().hashCode();

        Timer.Sample connectionCreationTimer = hooks.onStart(connectionId);
        Timer.Sample connectionLifetimeTimer = Timer.start();

        sseApiTotalCounter.increment();
        Timer.Sample totalResponseTimer = Timer.start();

        sseConnectionCounter.increment();
        Timer.Sample connectionTimer = Timer.start();

        CompletableFuture.delayedExecutor(120, TimeUnit.SECONDS).execute(() -> {
            if (!forceCompleted.get()) {
                log.warn("⏰ SSE 연결 강제 타임아웃: connectionId={}", connectionId);
                cancelled.set(true);
                if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
                    connectionTimer.stop(sseConnectionDurationTimer);
                    totalResponseTimer.stop(sseApiTotalResponseTimer);
                    sseDisconnectionCounter.increment();
                    hooks.onEnd(connectionCreationTimer, connectionLifetimeTimer, "force_timeout", connectionId);
                }
                try { emitter.complete(); } catch (Exception e) { log.warn("강제 종료 중 오류: {}", e.getMessage()); }
            }
        });

        emitter.onCompletion(() -> {
            cancelled.set(true);
            if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
                connectionTimer.stop(sseConnectionDurationTimer);
                totalResponseTimer.stop(sseApiTotalResponseTimer);
                sseDisconnectionCounter.increment();
                hooks.onEnd(connectionCreationTimer, connectionLifetimeTimer, "completion", connectionId);
            }
        });
        emitter.onTimeout(() -> {
            cancelled.set(true);
            if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
                connectionTimer.stop(sseConnectionDurationTimer);
                totalResponseTimer.stop(sseApiTotalResponseTimer);
                sseDisconnectionCounter.increment();
                hooks.onEnd(connectionCreationTimer, connectionLifetimeTimer, "timeout", connectionId);
            }
        });
        emitter.onError(e -> {
            cancelled.set(true);
            if (connectionEndedMap.putIfAbsent(connectionId, true) == null) {
                connectionTimer.stop(sseConnectionDurationTimer);
                totalResponseTimer.stop(sseApiTotalResponseTimer);
                sseDisconnectionCounter.increment();
                hooks.onEnd(connectionCreationTimer, connectionLifetimeTimer, "error", connectionId);
            }
        });

        return new Result(emitter, cancelled, forceCompleted, connectionId,
                connectionCreationTimer, connectionLifetimeTimer, totalResponseTimer, connectionTimer);
    }

    public record Result(
            SseEmitter emitter,
            AtomicBoolean cancelled,
            AtomicBoolean forceCompleted,
            String connectionId,
            Timer.Sample connectionCreationTimer,
            Timer.Sample connectionLifetimeTimer,
            Timer.Sample totalResponseTimer,
            Timer.Sample connectionTimer
    ) {}
}


