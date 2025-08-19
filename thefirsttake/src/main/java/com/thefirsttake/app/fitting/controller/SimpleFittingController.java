package com.thefirsttake.app.fitting.controller;

import com.thefirsttake.app.fitting.client.FitRoomApiClient;
import com.thefirsttake.app.fitting.dto.response.FittingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/fitting")
@Slf4j
public class SimpleFittingController {
    
    private final FitRoomApiClient fitRoomClient;
    
    public SimpleFittingController(FitRoomApiClient fitRoomClient) {
        this.fitRoomClient = fitRoomClient;
    }
    
    /**
     * 가상피팅 실행 - 완료될 때까지 대기 후 다운로드 링크 반환
     */
    @PostMapping("/try-on")
    public ResponseEntity<FittingResponse> tryOn(
            @RequestParam("modelImage") MultipartFile modelImage,
            @RequestParam("clothImage") MultipartFile clothImage,
            @RequestParam("clothType") String clothType, // "upper" 또는 "lower"
            @RequestParam(value = "hdMode", defaultValue = "false") boolean hdMode) {
        
        try {
            log.info("가상피팅 시작: clothType={}, hdMode={}", clothType, hdMode);
            
            // 1. FitRoom API로 작업 생성
            String taskId = fitRoomClient.createTask(modelImage, clothImage, clothType, hdMode);
            log.info("FitRoom 작업 생성 완료: taskId={}", taskId);
            
            // 2. 작업 완료까지 대기 (폴링)
            String downloadUrl = fitRoomClient.waitForCompletion(taskId);
            log.info("가상피팅 완료: taskId={}, downloadUrl={}", taskId, downloadUrl);
            
            // 3. 결과 반환
            return ResponseEntity.ok(FittingResponse.builder()
                .success(true)
                .message("가상피팅이 완료되었습니다.")
                .downloadUrl(downloadUrl)
                .taskId(taskId)
                .build());
                
        } catch (Exception e) {
            log.error("가상피팅 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FittingResponse.builder()
                    .success(false)
                    .message("가상피팅 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * 가상피팅 상태 확인 API (선택사항 - 나중에 비동기 처리 시 사용)
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<String> getStatus(@PathVariable String taskId) {
        // 나중에 비동기 처리가 필요할 때 구현할 수 있음
        return ResponseEntity.ok("Status check endpoint - to be implemented for async processing");
    }
}
