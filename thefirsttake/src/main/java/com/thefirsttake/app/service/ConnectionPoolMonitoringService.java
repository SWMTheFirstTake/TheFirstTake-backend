package com.thefirsttake.app.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DB 커넥션 풀 모니터링 서비스
 * - 실시간 커넥션 풀 상태 추적
 * - Prometheus 메트릭 업데이트
 * - 경고 및 알림 처리
 */
@Service
@Slf4j
public class ConnectionPoolMonitoringService {
    
    private final DataSource dataSource;
    private final MeterRegistry meterRegistry;
    
    // 메트릭 카운터들
    private final Counter connectionTimeoutCounter;
    private final Counter connectionLeakCounter;
    private final Timer connectionAcquisitionTimer;
    private final Timer transactionDurationTimer;
    
    public ConnectionPoolMonitoringService(
            DataSource dataSource,
            MeterRegistry meterRegistry,
            @Qualifier("dbConnectionTimeoutCounter") Counter connectionTimeoutCounter,
            @Qualifier("dbConnectionLeakCounter") Counter connectionLeakCounter,
            @Qualifier("dbConnectionAcquisitionTimer") Timer connectionAcquisitionTimer,
            @Qualifier("dbTransactionDurationTimer") Timer transactionDurationTimer) {
        this.dataSource = dataSource;
        this.meterRegistry = meterRegistry;
        this.connectionTimeoutCounter = connectionTimeoutCounter;
        this.connectionLeakCounter = connectionLeakCounter;
        this.connectionAcquisitionTimer = connectionAcquisitionTimer;
        this.transactionDurationTimer = transactionDurationTimer;
    }
    
    // 커넥션 풀 상태 추적
    private final AtomicInteger highUtilizationCount = new AtomicInteger(0);
    private final AtomicInteger waitingThreadsCount = new AtomicInteger(0);
    
    /**
     * 10초마다 커넥션 풀 상태 모니터링
     */
    @Scheduled(fixedRate = 10000)
    public void monitorConnectionPool() {
        try {
            HikariDataSource hikariDS = (HikariDataSource) dataSource;
            HikariPoolMXBean poolBean = hikariDS.getHikariPoolMXBean();
            
            int activeConnections = poolBean.getActiveConnections();
            int totalConnections = poolBean.getTotalConnections();
            int waitingThreads = poolBean.getThreadsAwaitingConnection();
            int maxPoolSize = hikariDS.getMaximumPoolSize();
            
            // 커넥션 풀 사용률 계산
            double utilizationRate = totalConnections > 0 ? (double) activeConnections / totalConnections : 0.0;
            
            // 경고 상태 확인
            boolean isHighUtilization = utilizationRate > 0.8; // 80% 이상
            boolean hasWaitingThreads = waitingThreads > 0;
            boolean isCritical = utilizationRate > 0.95 || waitingThreads > 5; // 95% 이상 또는 5개 이상 대기
            
            // 로그 출력
            if (isCritical) {
                log.warn("🚨 CRITICAL: 커넥션 풀 상태 위험 - 활성: {}/{}, 대기: {}, 사용률: {:.1f}%", 
                        activeConnections, maxPoolSize, waitingThreads, utilizationRate * 100);
            } else if (isHighUtilization || hasWaitingThreads) {
                log.info("⚠️ WARNING: 커넥션 풀 사용률 높음 - 활성: {}/{}, 대기: {}, 사용률: {:.1f}%", 
                        activeConnections, maxPoolSize, waitingThreads, utilizationRate * 100);
            } else {
                log.debug("✅ 커넥션 풀 상태 정상 - 활성: {}/{}, 대기: {}, 사용률: {:.1f}%", 
                        activeConnections, maxPoolSize, waitingThreads, utilizationRate * 100);
            }
            
            // 경고 카운터 업데이트
            if (isHighUtilization) {
                highUtilizationCount.incrementAndGet();
            } else {
                highUtilizationCount.set(0);
            }
            
            if (hasWaitingThreads) {
                waitingThreadsCount.incrementAndGet();
            } else {
                waitingThreadsCount.set(0);
            }
            
            // 연속 경고 시 알림
            if (highUtilizationCount.get() >= 3) { // 30초간 연속 경고
                log.error("🔥 연속 3회 고사용률 감지 - 커넥션 풀 확장 필요");
                highUtilizationCount.set(0); // 리셋
            }
            
            if (waitingThreadsCount.get() >= 5) { // 50초간 연속 대기
                log.error("🔥 연속 5회 대기 스레드 감지 - 커넥션 풀 병목 발생");
                waitingThreadsCount.set(0); // 리셋
            }
            
        } catch (Exception e) {
            log.error("커넥션 풀 모니터링 중 오류 발생", e);
        }
    }
    
    /**
     * 커넥션 타임아웃 발생 시 호출
     */
    public void recordConnectionTimeout() {
        connectionTimeoutCounter.increment();
        log.error("❌ DB 커넥션 타임아웃 발생");
    }
    
    /**
     * 커넥션 리크 감지 시 호출
     */
    public void recordConnectionLeak() {
        connectionLeakCounter.increment();
        log.error("❌ DB 커넥션 리크 감지");
    }
    
    /**
     * 커넥션 획득 시간 측정
     */
    public Timer.Sample startConnectionAcquisition() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * 커넥션 획득 완료
     */
    public void recordConnectionAcquisition(Timer.Sample sample) {
        sample.stop(connectionAcquisitionTimer);
    }
    
    /**
     * 트랜잭션 시간 측정
     */
    public Timer.Sample startTransaction() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * 트랜잭션 완료
     */
    public void recordTransaction(Timer.Sample sample) {
        sample.stop(transactionDurationTimer);
    }
    
    /**
     * 현재 커넥션 풀 상태 조회
     */
    public ConnectionPoolStatus getConnectionPoolStatus() {
        try {
            HikariDataSource hikariDS = (HikariDataSource) dataSource;
            HikariPoolMXBean poolBean = hikariDS.getHikariPoolMXBean();
            
            return ConnectionPoolStatus.builder()
                    .activeConnections(poolBean.getActiveConnections())
                    .idleConnections(poolBean.getIdleConnections())
                    .totalConnections(poolBean.getTotalConnections())
                    .waitingThreads(poolBean.getThreadsAwaitingConnection())
                    .maxPoolSize(hikariDS.getMaximumPoolSize())
                    .utilizationRate(poolBean.getTotalConnections() > 0 ? (double) poolBean.getActiveConnections() / poolBean.getTotalConnections() : 0.0)
                    .build();
                    
        } catch (Exception e) {
            log.error("커넥션 풀 상태 조회 실패", e);
            return ConnectionPoolStatus.builder().build();
        }
    }
    
    /**
     * 커넥션 풀 상태 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class ConnectionPoolStatus {
        private int activeConnections;
        private int idleConnections;
        private int totalConnections;
        private int waitingThreads;
        private int maxPoolSize;
        private double utilizationRate;
        
        public boolean isHealthy() {
            return utilizationRate < 0.8 && waitingThreads == 0;
        }
        
        public boolean isWarning() {
            return utilizationRate >= 0.8 || waitingThreads > 0;
        }
        
        public boolean isCritical() {
            return utilizationRate >= 0.95 || waitingThreads > 5;
        }
    }
}
