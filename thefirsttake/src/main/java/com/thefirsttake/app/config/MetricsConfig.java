package com.thefirsttake.app.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter kakaoLoginSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("kakao_login_success_total")
                .description("Total number of successful Kakao logins")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter kakaoLoginFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("kakao_login_failure_total")
                .description("Total number of failed Kakao logins")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter logoutCounter(MeterRegistry meterRegistry) {
        return Counter.builder("logout_total")
                .description("Total number of logouts")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer jwtTokenGenerationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("jwt_token_generation_duration")
                .description("Time taken to generate JWT tokens")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter apiRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("api_requests_total")
                .description("Total number of API requests")
                .tag("type", "all")
                .register(meterRegistry);
    }
    
    // ===== 채팅 관련 메트릭 =====
    
    @Bean
    public Counter sseConnectionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("sse_connections_total")
                .description("Total number of SSE connections")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter sseDisconnectionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("sse_disconnections_total")
                .description("Total number of SSE disconnections")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer sseConnectionDurationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("sse_connection_duration")
                .description("Duration of SSE connections")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter llmApiCallCounter(MeterRegistry meterRegistry) {
        return Counter.builder("llm_api_calls_total")
                .description("Total number of LLM API calls")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter llmApiSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("llm_api_success_total")
                .description("Total number of successful LLM API calls")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter llmApiFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("llm_api_failure_total")
                .description("Total number of failed LLM API calls")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer llmApiResponseTimer(MeterRegistry meterRegistry) {
        return Timer.builder("llm_api_response_duration")
                .description("LLM API response time")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter productSearchApiCallCounter(MeterRegistry meterRegistry) {
        return Counter.builder("product_search_api_calls_total")
                .description("Total number of product search API calls")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter productSearchApiSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("product_search_api_success_total")
                .description("Total number of successful product search API calls")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter productSearchApiFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("product_search_api_failure_total")
                .description("Total number of failed product search API calls")
                .register(meterRegistry);
    }
    
    @Bean
    public Timer productSearchApiResponseTimer(MeterRegistry meterRegistry) {
        return Timer.builder("product_search_api_response_duration")
                .description("Product search API response time")
                .register(meterRegistry);
    }
    
    // ===== LLM API 전문가별 메트릭 =====
    
    @Bean
    public Counter llmApiCallCounterByExpert(MeterRegistry meterRegistry) {
        return Counter.builder("llm_api_calls_by_expert_total")
                .description("Total number of LLM API calls by expert")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter llmApiStatusCodeCounter(MeterRegistry meterRegistry) {
        return Counter.builder("llm_api_status_code_total")
                .description("LLM API calls by HTTP status code")
                .register(meterRegistry);
    }
    
    @Bean
    public DistributionSummary llmApiResponseSizeSummary(MeterRegistry meterRegistry) {
        return DistributionSummary.builder("llm_api_response_size")
                .description("LLM API response size in bytes")
                .baseUnit("bytes")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter llmApiRetryCounter(MeterRegistry meterRegistry) {
        return Counter.builder("llm_api_retries_total")
                .description("Total number of LLM API retries")
                .register(meterRegistry);
    }
    
    // ===== SSE API 전체 응답 시간 메트릭 =====
    
    @Bean
    public Timer sseApiTotalResponseTimer(MeterRegistry meterRegistry) {
        return Timer.builder("sse_api_total_response_duration")
                .description("Total SSE API response time from start to complete")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter sseApiTotalCounter(MeterRegistry meterRegistry) {
        return Counter.builder("sse_api_total_requests")
                .description("Total number of SSE API requests")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter sseApiSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("sse_api_success_total")
                .description("Total number of successful SSE API completions")
                .register(meterRegistry);
    }
    
    @Bean
    public Counter sseApiFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("sse_api_failure_total")
                .description("Total number of failed SSE API requests")
                .register(meterRegistry);
    }
    
}
