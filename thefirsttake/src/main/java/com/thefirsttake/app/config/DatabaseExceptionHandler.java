package com.thefirsttake.app.config;

import com.thefirsttake.app.service.ConnectionPoolMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/**
 * DB 관련 예외를 전역적으로 처리하고 메트릭을 업데이트
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class DatabaseExceptionHandler {
    
    private final ConnectionPoolMonitoringService connectionPoolMonitoringService;
    
    /**
     * SQL 타임아웃 예외 처리
     */
    @ExceptionHandler(SQLTimeoutException.class)
    public void handleSQLTimeoutException(SQLTimeoutException e) {
        log.error("❌ SQL 타임아웃 발생: {}", e.getMessage());
        connectionPoolMonitoringService.recordConnectionTimeout();
    }
    
    /**
     * 쿼리 타임아웃 예외 처리
     */
    @ExceptionHandler(QueryTimeoutException.class)
    public void handleQueryTimeoutException(QueryTimeoutException e) {
        log.error("❌ 쿼리 타임아웃 발생: {}", e.getMessage());
        connectionPoolMonitoringService.recordConnectionTimeout();
    }
    
    /**
     * 일반적인 DB 접근 예외 처리
     */
    @ExceptionHandler(DataAccessException.class)
    public void handleDataAccessException(DataAccessException e) {
        // SQL 타임아웃 관련 예외인지 확인
        Throwable rootCause = e.getRootCause();
        if (rootCause instanceof SQLTimeoutException) {
            log.error("❌ DB 타임아웃 발생: {}", e.getMessage());
            connectionPoolMonitoringService.recordConnectionTimeout();
        } else if (rootCause instanceof SQLException) {
            SQLException sqlEx = (SQLException) rootCause;
            // PostgreSQL 타임아웃 에러 코드들
            if (sqlEx.getSQLState() != null && 
                (sqlEx.getSQLState().equals("08006") || // connection failure
                 sqlEx.getSQLState().equals("08003") || // connection does not exist
                 sqlEx.getSQLState().equals("08001"))) { // unable to connect
                log.error("❌ DB 연결 타임아웃 발생: {}", e.getMessage());
                connectionPoolMonitoringService.recordConnectionTimeout();
            }
        }
    }
}
