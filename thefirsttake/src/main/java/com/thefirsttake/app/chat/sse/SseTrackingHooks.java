package com.thefirsttake.app.chat.sse;

import io.micrometer.core.instrument.Timer;

/**
 * SSE 연결 수명주기 메트릭/추적 훅을 컨트롤러로부터 주입받기 위한 인터페이스.
 */
public interface SseTrackingHooks {
    /** 연결 생성 시점 훅 (메트릭 시작 등) */
    Timer.Sample onStart(String connectionId);

    /** 연결 종료 시점 훅 (메트릭 종료 등) */
    void onEnd(Timer.Sample connectionCreationTimer, Timer.Sample lifetimeTimer, String reason, String connectionId);
}


