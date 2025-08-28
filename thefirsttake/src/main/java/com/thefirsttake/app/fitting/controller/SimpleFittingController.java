package com.thefirsttake.app.fitting.controller;

import com.thefirsttake.app.common.response.CommonResponse;
import com.thefirsttake.app.fitting.client.FitRoomApiClient;
import com.thefirsttake.app.fitting.dto.response.FittingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/fitting")
@Tag(name = "가상피팅", description = "가상피팅 관련 API")
@Slf4j
public class SimpleFittingController {
    
    private final FitRoomApiClient fitRoomClient;
    private final RestTemplate restTemplate;
    
    public SimpleFittingController(FitRoomApiClient fitRoomClient, RestTemplate restTemplate) {
        this.fitRoomClient = fitRoomClient;
        this.restTemplate = restTemplate;
    }
    
    /**
     * 가상피팅 실행 - 완료될 때까지 대기 후 다운로드 링크 반환
     */
    @PostMapping("/try-on")
    @Operation(
        summary = "가상피팅 실행",
        description = "모델 이미지와 장바구니에 있는 옷 이미지를 받아서 가상피팅을 실행하고 결과 이미지의 다운로드 링크를 반환합니다.\n\n" +
                     "**Content-Type**: multipart/form-data\n\n" +
                     "**주의**: model_image와 cloth_image(장바구니에 있는 옷)는 이미지 파일입니다. Swagger UI에서는 string으로 표시되지만 실제로는 파일을 선택해야 합니다.\n\n" +
                     "**Postman 테스트 시**: form-data로 설정하고 각 필드를 File 타입으로 선택하세요."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "가상피팅 성공",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        )
    })
    public ResponseEntity<CommonResponse> tryOn(
            @Parameter(name = "model_image", description = "모델 사진 파일 (MultipartFile)", required = true, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam("model_image") MultipartFile modelImage,
            @Parameter(name = "cloth_image", description = "장바구니에 있는 옷 사진 파일 (MultipartFile)", required = true, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam("cloth_image") MultipartFile clothImage,
            @Parameter(name = "cloth_type", description = "옷 종류 (upper: 상의, lower: 하의)", required = true, example = "upper")
            @RequestParam("cloth_type") String clothType,
            @Parameter(name = "hd_mode", description = "HD 모드 여부", required = false, example = "false")
            @RequestParam(value = "hd_mode", defaultValue = "false") boolean hdMode) {
        
        try {
            log.info("가상피팅 시작: clothType={}, hdMode={}", clothType, hdMode);
            
            // 1. FitRoom API로 작업 생성
            String taskId = fitRoomClient.createTask(modelImage, clothImage, clothType, hdMode);
            log.info("FitRoom 작업 생성 완료: taskId={}", taskId);
            
            // 2. 작업 완료까지 대기 (폴링)
            String downloadUrl = fitRoomClient.waitForCompletion(taskId);
            log.info("가상피팅 완료: taskId={}, downloadUrl={}", taskId, downloadUrl);
            
            // 3. 결과 반환
            FittingResponse response = FittingResponse.builder()
                .success(true)
                .message("가상피팅이 완료되었습니다.")
                .downloadUrl(downloadUrl)
                .taskId(taskId)
                .build();
                
            return ResponseEntity.ok(CommonResponse.success(response));
                
        } catch (Exception e) {
            log.error("가상피팅 실패", e);
            FittingResponse errorResponse = FittingResponse.builder()
                .success(false)
                .message("가상피팅 처리 중 오류가 발생했습니다: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.fail(errorResponse.getMessage()));
        }
    }
    
    /**
     * 콤보 가상피팅 실행 - 상하의를 동시에 입히는 가상피팅
     */
    @PostMapping("/try-on/combo")
    @Operation(
        summary = "콤보 가상피팅 실행",
        description = "모델 이미지와 장바구니에 있는 상의, 하의 이미지를 받아서 동시에 입히는 가상피팅을 실행하고 결과 이미지의 다운로드 링크를 반환합니다.\n\n" +
                     "**Content-Type**: multipart/form-data\n\n" +
                     "**주의**: model_image, cloth_image(상의), lower_cloth_image(하의)는 파일 업로드입니다. Swagger UI에서는 string으로 표시되지만 실제로는 파일을 선택해야 합니다.\n\n" +
                     "**Postman 테스트 시**: form-data로 설정하고 각 필드를 File 타입으로 선택하세요."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "콤보 가상피팅 성공",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        )
    })
    public ResponseEntity<CommonResponse> tryOnCombo(
            @Parameter(name = "model_image", description = "모델 사진 파일 (MultipartFile)", required = true, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam("model_image") MultipartFile modelImage,
            @Parameter(name = "cloth_image", description = "장바구니에 있는 상의 사진 파일 (MultipartFile)", required = true, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam("cloth_image") MultipartFile clothImage,
            @Parameter(name = "lower_cloth_image", description = "장바구니에 있는 하의 사진 파일 (MultipartFile)", required = true, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam("lower_cloth_image") MultipartFile lowerClothImage,
            @Parameter(name = "hd_mode", description = "HD 모드 여부", required = false, example = "false")
            @RequestParam(value = "hd_mode", defaultValue = "false") boolean hdMode) {
        
        try {
            log.info("콤보 가상피팅 시작: hdMode={}", hdMode);
            
            // 1. FitRoom API로 콤보 작업 생성
            String taskId = fitRoomClient.createComboTask(modelImage, clothImage, lowerClothImage, hdMode);
            log.info("FitRoom 콤보 작업 생성 완료: taskId={}", taskId);
            
            // 2. 작업 완료까지 대기 (폴링)
            String downloadUrl = fitRoomClient.waitForCompletion(taskId);
            log.info("콤보 가상피팅 완료: taskId={}, downloadUrl={}", taskId, downloadUrl);
            
            // 3. 결과 반환
            FittingResponse response = FittingResponse.builder()
                .success(true)
                .message("콤보 가상피팅이 완료되었습니다.")
                .downloadUrl(downloadUrl)
                .taskId(taskId)
                .build();
                
            return ResponseEntity.ok(CommonResponse.success(response));
                
        } catch (Exception e) {
            log.error("콤보 가상피팅 실패", e);
            FittingResponse errorResponse = FittingResponse.builder()
                .success(false)
                .message("콤보 가상피팅 처리 중 오류가 발생했습니다: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.fail(errorResponse.getMessage()));
        }
    }
    
    /**
     * 가상피팅 상태 확인 API (선택사항 - 나중에 비동기 처리 시 사용)
     */
    @GetMapping("/status/{taskId}")
    @Operation(
        summary = "가상피팅 상태 확인",
        description = "가상피팅 작업의 현재 상태를 확인합니다. (향후 비동기 처리 시 사용 예정)"
    )
    public ResponseEntity<String> getStatus(
            @Parameter(description = "작업 ID", required = true, example = "task_12345")
            @PathVariable String taskId) {
        // 나중에 비동기 처리가 필요할 때 구현할 수 있음
        return ResponseEntity.ok("Status check endpoint - to be implemented for async processing");
    }
}
