package com.thefirsttake.app.chat.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 스트림 처리 중 메트릭 수집을 담당하는 서비스
 * - LLM API 호출 메트릭
 * - 상품 검색 API 메트릭
 * - 메모리 사용량 측정
 * - 응답 시간 측정
 */
@Service
@Slf4j
public class StreamMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // SSE 관련 메트릭
    private final Counter sseConnectionCounter;
    private final Counter sseDisconnectionCounter;
    private final Timer sseConnectionDurationTimer;
    
    // LLM API 관련 메트릭
    private final Counter llmApiCallCounter;
    private final Counter llmApiSuccessCounter;
    private final Counter llmApiFailureCounter;
    private final Timer llmApiResponseTimer;
    private final Counter llmApiCallCounterByExpert;
    
    // 상품 검색 API 관련 메트릭
    private final Counter productSearchApiCallCounter;
    private final Counter productSearchApiSuccessCounter;
    private final Counter productSearchApiFailureCounter;
    private final Timer productSearchApiResponseTimer;
    
    public StreamMetricsService(MeterRegistry meterRegistry,
                               @Qualifier("sseConnectionCounter") Counter sseConnectionCounter,
                               @Qualifier("sseDisconnectionCounter") Counter sseDisconnectionCounter,
                               @Qualifier("sseConnectionDurationTimer") Timer sseConnectionDurationTimer,
                               @Qualifier("llmApiCallCounter") Counter llmApiCallCounter,
                               @Qualifier("llmApiSuccessCounter") Counter llmApiSuccessCounter,
                               @Qualifier("llmApiFailureCounter") Counter llmApiFailureCounter,
                               @Qualifier("llmApiResponseTimer") Timer llmApiResponseTimer,
                               @Qualifier("llmApiCallCounterByExpert") Counter llmApiCallCounterByExpert,
                               @Qualifier("productSearchApiCallCounter") Counter productSearchApiCallCounter,
                               @Qualifier("productSearchApiSuccessCounter") Counter productSearchApiSuccessCounter,
                               @Qualifier("productSearchApiFailureCounter") Counter productSearchApiFailureCounter,
                               @Qualifier("productSearchApiResponseTimer") Timer productSearchApiResponseTimer) {
        this.meterRegistry = meterRegistry;
        this.sseConnectionCounter = sseConnectionCounter;
        this.sseDisconnectionCounter = sseDisconnectionCounter;
        this.sseConnectionDurationTimer = sseConnectionDurationTimer;
        this.llmApiCallCounter = llmApiCallCounter;
        this.llmApiSuccessCounter = llmApiSuccessCounter;
        this.llmApiFailureCounter = llmApiFailureCounter;
        this.llmApiResponseTimer = llmApiResponseTimer;
        this.llmApiCallCounterByExpert = llmApiCallCounterByExpert;
        this.productSearchApiCallCounter = productSearchApiCallCounter;
        this.productSearchApiSuccessCounter = productSearchApiSuccessCounter;
        this.productSearchApiFailureCounter = productSearchApiFailureCounter;
        this.productSearchApiResponseTimer = productSearchApiResponseTimer;
    }
    
    // 메모리 추적을 위한 맵
    private final ConcurrentHashMap<String, Long> connectionMemoryMap = new ConcurrentHashMap<>();
    
    /**
     * LLM API 호출 시작 - 메트릭 증가 및 타이머 시작
     * @param expertType 전문가 타입
     * @return 시작된 타이머 샘플
     */
    public Timer.Sample startLlmApiCall(String expertType) {
        llmApiCallCounter.increment();
        llmApiCallCounterByExpert.increment();
        
        log.debug("LLM API 호출 시작: expertType={}", expertType);
        return Timer.start();
    }
    
    /**
     * LLM API 호출 완료 - 타이머 종료 및 상태별 메트릭 증가
     * @param timerSample 타이머 샘플
     * @param expertType 전문가 타입
     * @param statusCode HTTP 상태 코드
     * @param responseBody 응답 본문 (크기 측정용)
     * @param success 성공 여부
     */
    public void endLlmApiCall(Timer.Sample timerSample, String expertType, int statusCode, String responseBody, boolean success) {
        // 응답 시간 메트릭
        timerSample.stop(llmApiResponseTimer);
        
        // 상태 코드별 메트릭
        Counter.builder("llm_api_status_code_total")
                .description("LLM API calls by HTTP status code")
                .tag("expert_type", expertType)
                .tag("status_code", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();
        
        // 응답 크기 메트릭
        if (responseBody != null) {
            DistributionSummary.builder("llm_api_response_size")
                    .description("LLM API response size in bytes")
                    .baseUnit("bytes")
                    .tag("expert_type", expertType)
                    .register(meterRegistry)
                    .record(responseBody.length());
        }
        
        // 성공/실패 메트릭
        if (success) {
            llmApiSuccessCounter.increment();
        } else {
            llmApiFailureCounter.increment();
        }
        
        log.debug("LLM API 호출 완료: expertType={}, statusCode={}, success={}, responseSize={}", 
                expertType, statusCode, success, responseBody != null ? responseBody.length() : 0);
    }
    
    /**
     * 상품 검색 API 호출 시작 - 메트릭 증가 및 타이머 시작
     * @return 시작된 타이머 샘플
     */
    public Timer.Sample startProductSearchApiCall() {
        productSearchApiCallCounter.increment();
        log.debug("상품 검색 API 호출 시작");
        return Timer.start();
    }
    
    /**
     * 상품 검색 API 호출 완료 - 타이머 종료 및 성공/실패 메트릭 증가
     * @param timerSample 타이머 샘플
     * @param success 성공 여부
     */
    public void endProductSearchApiCall(Timer.Sample timerSample, boolean success) {
        timerSample.stop(productSearchApiResponseTimer);
        
        if (success) {
            productSearchApiSuccessCounter.increment();
        } else {
            productSearchApiFailureCounter.increment();
        }
        
        log.debug("상품 검색 API 호출 완료: success={}", success);
    }
    
    /**
     * 메모리 사용량 기록
     * @param connectionId 연결 식별자
     */
    public void recordMemoryUsage(String connectionId) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            connectionMemoryMap.put(connectionId, usedMemory);
            
            // 메모리 사용량 메트릭 기록
            DistributionSummary.builder("sse_connection_memory_usage")
                    .description("Memory usage per SSE connection")
                    .baseUnit("bytes")
                    .register(meterRegistry)
                    .record(usedMemory);
            
            log.debug("메모리 사용량 기록: connectionId={}, usedMemory={} bytes", connectionId, usedMemory);
            
        } catch (Exception e) {
            log.warn("메모리 사용량 기록 실패: connectionId={}, error={}", connectionId, e.getMessage());
        }
    }
    
    /**
     * 메모리 사용량 기록 (연결 ID 없이)
     */
    public void recordMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            // 메모리 사용량 메트릭 기록
            DistributionSummary.builder("stream_processing_memory_usage")
                    .description("Memory usage during stream processing")
                    .baseUnit("bytes")
                    .register(meterRegistry)
                    .record(usedMemory);
            
            log.debug("메모리 사용량 기록: usedMemory={} bytes", usedMemory);
            
        } catch (Exception e) {
            log.warn("메모리 사용량 기록 실패: error={}", e.getMessage());
        }
    }
    
    /**
     * 연결별 메모리 사용량 조회
     * @param connectionId 연결 식별자
     * @return 메모리 사용량 (바이트)
     */
    public Long getConnectionMemoryUsage(String connectionId) {
        return connectionMemoryMap.get(connectionId);
    }
    
    /**
     * 연결별 메모리 사용량 제거
     * @param connectionId 연결 식별자
     */
    public void removeConnectionMemoryUsage(String connectionId) {
        connectionMemoryMap.remove(connectionId);
        log.debug("연결별 메모리 사용량 제거: connectionId={}", connectionId);
    }
    
    /**
     * SSE 연결 메트릭 증가
     */
    public void incrementSseConnection() {
        sseConnectionCounter.increment();
        log.debug("SSE 연결 메트릭 증가");
    }
    
    /**
     * SSE 연결 해제 메트릭 증가
     */
    public void incrementSseDisconnection() {
        sseDisconnectionCounter.increment();
        log.debug("SSE 연결 해제 메트릭 증가");
    }
    
    /**
     * 커스텀 카운터 생성
     * @param name 카운터 이름
     * @param description 설명
     * @param tags 태그들 (키-값 쌍)
     * @return 생성된 카운터
     */
    public Counter createCounter(String name, String description, String... tags) {
        Counter.Builder builder = Counter.builder(name).description(description);
        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }
        return builder.register(meterRegistry);
    }
    
    /**
     * 커스텀 타이머 생성
     * @param name 타이머 이름
     * @param description 설명
     * @param tags 태그들 (키-값 쌍)
     * @return 생성된 타이머
     */
    public Timer createTimer(String name, String description, String... tags) {
        Timer.Builder builder = Timer.builder(name).description(description);
        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }
        return builder.register(meterRegistry);
    }
    
    /**
     * 커스텀 분포 요약 생성
     * @param name 분포 요약 이름
     * @param description 설명
     * @param baseUnit 기본 단위
     * @param tags 태그들 (키-값 쌍)
     * @return 생성된 분포 요약
     */
    public DistributionSummary createDistributionSummary(String name, String description, String baseUnit, String... tags) {
        DistributionSummary.Builder builder = DistributionSummary.builder(name)
                .description(description)
                .baseUnit(baseUnit);
        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }
        return builder.register(meterRegistry);
    }
}
