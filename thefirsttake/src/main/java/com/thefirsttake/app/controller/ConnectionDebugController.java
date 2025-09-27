package com.thefirsttake.app.controller;

import com.thefirsttake.app.service.ConnectionPoolMonitoringService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 커넥션 리크 진단 및 모니터링을 위한 디버그 컨트롤러
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class ConnectionDebugController {
    
    @Autowired
    private DataSource dataSource;
    
    private final ConnectionPoolMonitoringService connectionPoolMonitoringService;
    
    /**
     * 현재 커넥션 풀 상태 확인
     */
    @GetMapping("/connections")
    public Map<String, Object> getConnectionInfo() {
        try {
            HikariDataSource hikariDS = (HikariDataSource) dataSource;
            HikariPoolMXBean poolBean = hikariDS.getHikariPoolMXBean();
            
            Map<String, Object> info = new HashMap<>();
            info.put("active_connections", poolBean.getActiveConnections());
            info.put("idle_connections", poolBean.getIdleConnections());
            info.put("total_connections", poolBean.getTotalConnections());
            info.put("threads_awaiting_connection", poolBean.getThreadsAwaitingConnection());
            info.put("maximum_pool_size", hikariDS.getMaximumPoolSize());
            info.put("minimum_idle", hikariDS.getMinimumIdle());
            info.put("connection_timeout", hikariDS.getConnectionTimeout());
            info.put("idle_timeout", hikariDS.getIdleTimeout());
            info.put("max_lifetime", hikariDS.getMaxLifetime());
            
            // 커넥션 사용률 계산
            double utilizationRate = (double) poolBean.getActiveConnections() / poolBean.getTotalConnections() * 100;
            info.put("utilization_rate_percent", String.format("%.2f", utilizationRate));
            
            // 경고 상태 확인
            boolean isHighUtilization = utilizationRate > 80.0;
            boolean hasWaitingThreads = poolBean.getThreadsAwaitingConnection() > 0;
            
            info.put("is_high_utilization", isHighUtilization);
            info.put("has_waiting_threads", hasWaitingThreads);
            info.put("status", (isHighUtilization || hasWaitingThreads) ? "WARNING" : "OK");
            
            log.info("커넥션 풀 상태: 활성={}, 유휴={}, 총={}, 대기중인스레드={}, 사용률={}%", 
                poolBean.getActiveConnections(),
                poolBean.getIdleConnections(), 
                poolBean.getTotalConnections(),
                poolBean.getThreadsAwaitingConnection(),
                String.format("%.2f", utilizationRate));
            
            return info;
            
        } catch (Exception e) {
            log.error("커넥션 정보 조회 실패", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get connection info: " + e.getMessage());
            return error;
        }
    }
    
    /**
     * 커넥션 풀 상태 (모니터링 서비스 사용)
     */
    @GetMapping("/connections/status")
    public Map<String, Object> getConnectionPoolStatus() {
        try {
            ConnectionPoolMonitoringService.ConnectionPoolStatus status = 
                    connectionPoolMonitoringService.getConnectionPoolStatus();
            
            Map<String, Object> result = new HashMap<>();
            result.put("active_connections", status.getActiveConnections());
            result.put("idle_connections", status.getIdleConnections());
            result.put("total_connections", status.getTotalConnections());
            result.put("waiting_threads", status.getWaitingThreads());
            result.put("max_pool_size", status.getMaxPoolSize());
            result.put("utilization_rate", String.format("%.2f", status.getUtilizationRate() * 100) + "%");
            result.put("is_healthy", status.isHealthy());
            result.put("is_warning", status.isWarning());
            result.put("is_critical", status.isCritical());
            result.put("status", status.isCritical() ? "CRITICAL" : 
                              status.isWarning() ? "WARNING" : "OK");
            
            return result;
            
        } catch (Exception e) {
            log.error("커넥션 풀 상태 조회 실패", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get connection pool status: " + e.getMessage());
            return error;
        }
    }
    
    /**
     * 커넥션 강제 해제 (긴급시 사용)
     */
    @PostMapping("/connections/force-close")
    public Map<String, String> forceCloseConnections() {
        try {
            HikariDataSource hikariDS = (HikariDataSource) dataSource;
            
            // 모든 커넥션 강제 종료 (위험하지만 긴급시)
            hikariDS.getHikariPoolMXBean().softEvictConnections();
            
            log.warn("커넥션 강제 해제 실행됨");
            
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Connections evicted successfully");
            return result;
            
        } catch (Exception e) {
            log.error("커넥션 강제 해제 실패", e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to evict connections: " + e.getMessage());
            return error;
        }
    }
    
    /**
     * JVM 메모리 상태 확인
     */
    @GetMapping("/memory")
    public Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        Map<String, Object> info = new HashMap<>();
        info.put("used_memory_mb", usedMemory / 1024 / 1024);
        info.put("total_memory_mb", totalMemory / 1024 / 1024);
        info.put("max_memory_mb", maxMemory / 1024 / 1024);
        info.put("free_memory_mb", freeMemory / 1024 / 1024);
        info.put("memory_usage_percent", String.format("%.2f", (double) usedMemory / maxMemory * 100));
        
        return info;
    }
    
    /**
     * 스레드 상태 확인
     */
    @GetMapping("/threads")
    public Map<String, Object> getThreadInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // 활성 스레드 수
        int activeThreads = Thread.activeCount();
        info.put("active_threads", activeThreads);
        
        // 모든 스레드 정보
        Thread[] threads = new Thread[activeThreads];
        Thread.enumerate(threads);
        
        Map<String, Integer> threadGroups = new HashMap<>();
        for (Thread thread : threads) {
            if (thread != null) {
                String groupName = thread.getThreadGroup().getName();
                threadGroups.put(groupName, threadGroups.getOrDefault(groupName, 0) + 1);
            }
        }
        info.put("thread_groups", threadGroups);
        
        return info;
    }
}
