package com.thefirsttake.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor") // 빈 이름 지정
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본으로 유지할 스레드 수 (CPU 코어 수 * 2 정도가 일반적 시작점)
        executor.setMaxPoolSize(10); // 최대 스레드 수 (동시 처리할 수 있는 최대 요청 수 고려)
        executor.setQueueCapacity(50); // 작업 큐 용량 (초과 시 스레드 더 생성)
        executor.setThreadNamePrefix("AsyncCuration-"); // 스레드 이름 접두사 (로그 추적 용이)
        executor.initialize(); // 스레드 풀 초기화
        return executor;
    }
}
