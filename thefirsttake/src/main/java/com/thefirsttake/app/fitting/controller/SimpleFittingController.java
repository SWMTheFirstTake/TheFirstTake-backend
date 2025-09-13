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

import jakarta.servlet.http.HttpServletResponse;

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
            @Parameter(name = "model_image", description = "모델 사진 파일 (MultipartFile) - 파일 또는 URL", required = false, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam(value = "model_image", required = false) MultipartFile modelImage,
            @Parameter(name = "model_image_url", description = "모델 이미지 URL (model_image 대신 사용 가능)", required = false, example = "https://example.com/model.jpg")
            @RequestParam(value = "model_image_url", required = false) String modelImageUrl,
            @Parameter(name = "cloth_image", description = "장바구니에 있는 상의 사진 파일 (MultipartFile) - 파일 또는 URL", required = false, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam(value = "cloth_image", required = false) MultipartFile clothImage,
            @Parameter(name = "cloth_image_url", description = "상의 이미지 URL (cloth_image 대신 사용 가능)", required = false, example = "https://example.com/top.jpg")
            @RequestParam(value = "cloth_image_url", required = false) String clothImageUrl,
            @Parameter(name = "lower_cloth_image", description = "장바구니에 있는 하의 사진 파일 (MultipartFile) - 파일 또는 URL", required = false, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam(value = "lower_cloth_image", required = false) MultipartFile lowerClothImage,
            @Parameter(name = "lower_cloth_image_url", description = "하의 이미지 URL (lower_cloth_image 대신 사용 가능)", required = false, example = "https://example.com/bottom.jpg")
            @RequestParam(value = "lower_cloth_image_url", required = false) String lowerClothImageUrl,
            @Parameter(name = "hd_mode", description = "HD 모드 여부", required = false, example = "false")
            @RequestParam(value = "hd_mode", defaultValue = "false") boolean hdMode) {
        
        try {
            log.info("콤보 가상피팅 시작: hdMode={}, modelImageUrl={}, clothImageUrl={}, lowerClothImageUrl={}", 
                hdMode, modelImageUrl, clothImageUrl, lowerClothImageUrl);
            
            // 파라미터 유효성 검사
            if ((modelImage == null && modelImageUrl == null)) {
                log.warn("모델 이미지가 제공되지 않았습니다.");
                return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("모델 이미지(파일 또는 URL)가 필요합니다."));
            }
            
            if ((clothImage == null && clothImageUrl == null) || 
                (lowerClothImage == null && lowerClothImageUrl == null)) {
                log.warn("상의 또는 하의 이미지가 제공되지 않았습니다.");
                return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("상의와 하의 이미지(파일 또는 URL)가 필요합니다."));
            }
            
            // 1. FitRoom API로 콤보 작업 생성
            String taskId;
            if (modelImageUrl != null || clothImageUrl != null || lowerClothImageUrl != null) {
                // URL 방식 사용 (모델, 상의, 하의 중 하나라도 URL이면)
                taskId = fitRoomClient.createComboTaskWithUrls(modelImage, modelImageUrl, clothImageUrl, lowerClothImageUrl, hdMode);
                log.info("FitRoom 콤보 작업 생성 완료 (URL 방식): taskId={}", taskId);
            } else {
                // 파일 방식 사용 (모든 이미지가 파일인 경우)
                taskId = fitRoomClient.createComboTask(modelImage, clothImage, lowerClothImage, hdMode);
                log.info("FitRoom 콤보 작업 생성 완료 (파일 방식): taskId={}", taskId);
            }
            
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
     * 이미지 프록시 API - CORS 문제 해결을 위한 이미지 프록시
     */
    @GetMapping("/proxy-image")
    @Operation(
        summary = "이미지 프록시",
        description = "CORS 문제 해결을 위한 이미지 프록시 API. 외부 이미지 URL을 받아서 CORS 헤더와 함께 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "이미지 프록시 성공",
            content = @Content(mediaType = "image/jpeg")
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (imageUrl 파라미터 누락)",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류 (이미지 다운로드 실패)",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        )
    })
    public ResponseEntity<?> proxyImage(
            @Parameter(description = "프록시할 이미지 URL", required = true, example = "https://example.com/image.jpg")
            @RequestParam("imageUrl") String imageUrl,
            HttpServletResponse response) {
        
        try {
            // URL 유효성 검사
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                log.warn("이미지 URL이 비어있습니다.");
                return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("이미지 URL이 필요합니다."));
            }
            
            // URL 디코딩 (프론트엔드에서 인코딩된 URL 처리)
            String decodedUrl = java.net.URLDecoder.decode(imageUrl, "UTF-8");
            log.info("이미지 프록시 시작: originalUrl={}, decodedUrl={}", imageUrl, decodedUrl);
            
            // URL 형식 검증
            if (!decodedUrl.startsWith("http://") && !decodedUrl.startsWith("https://")) {
                log.warn("잘못된 URL 형식: {}", decodedUrl);
                return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("잘못된 URL 형식입니다."));
            }
            
            // 외부 이미지 다운로드 (타임아웃 설정)
            log.info("외부 이미지 다운로드 시도: {}", decodedUrl);
            ResponseEntity<byte[]> externalResponse;
            
            try {
                externalResponse = restTemplate.getForEntity(decodedUrl, byte[].class);
                log.info("외부 이미지 다운로드 성공: status={}, contentLength={}", 
                    externalResponse.getStatusCode(), 
                    externalResponse.getBody() != null ? externalResponse.getBody().length : 0);
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                log.error("HTTP 클라이언트 에러: status={}, message={}", e.getStatusCode(), e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.fail("이미지 URL에 접근할 수 없습니다: " + e.getStatusCode()));
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                log.error("HTTP 서버 에러: status={}, message={}", e.getStatusCode(), e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(CommonResponse.fail("외부 서버 오류: " + e.getStatusCode()));
            } catch (org.springframework.web.client.ResourceAccessException e) {
                log.error("네트워크 접근 에러: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(CommonResponse.fail("네트워크 연결 실패: " + e.getMessage()));
            }
            
            if (externalResponse.getBody() == null || externalResponse.getBody().length == 0) {
                log.warn("다운로드된 이미지가 비어있습니다: imageUrl={}", decodedUrl);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("이미지를 다운로드할 수 없습니다."));
            }
            
            // CORS 헤더 설정
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Max-Age", "3600");
            
            // Content-Type 설정 (이미지 타입 감지)
            String contentType = externalResponse.getHeaders().getContentType() != null 
                ? externalResponse.getHeaders().getContentType().toString()
                : "image/jpeg";
            response.setContentType(contentType);
            
            // Cache-Control 설정 (선택사항)
            response.setHeader("Cache-Control", "public, max-age=3600");
            
            log.info("이미지 프록시 완료: imageUrl={}, size={} bytes, contentType={}", 
                decodedUrl, externalResponse.getBody().length, contentType);
            
            return ResponseEntity.ok(externalResponse.getBody());
            
        } catch (java.io.UnsupportedEncodingException e) {
            log.error("URL 디코딩 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.fail("URL 디코딩 실패: " + e.getMessage()));
        } catch (Exception e) {
            log.error("이미지 프록시 실패: imageUrl={}, error={}", imageUrl, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.fail("이미지 다운로드에 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * OPTIONS 요청 처리 (CORS preflight)
     */
    @RequestMapping(value = "/proxy-image", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> proxyImageOptions(HttpServletResponse response) {
        // CORS 헤더 설정
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Max-Age", "3600");
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * 프록시 API 테스트용 엔드포인트
     */
    @GetMapping("/proxy-test")
    @Operation(
        summary = "프록시 API 테스트",
        description = "프록시 API가 정상 작동하는지 테스트하는 엔드포인트"
    )
    public ResponseEntity<CommonResponse> proxyTest() {
        try {
            // 간단한 테스트 이미지 URL (예: 1x1 픽셀 이미지)
            String testUrl = "https://via.placeholder.com/1x1.jpg";
            log.info("프록시 테스트 시작: testUrl={}", testUrl);
            
            ResponseEntity<byte[]> response = restTemplate.getForEntity(testUrl, byte[].class);
            
            if (response.getBody() != null && response.getBody().length > 0) {
                log.info("프록시 테스트 성공: size={} bytes", response.getBody().length);
                return ResponseEntity.ok(CommonResponse.success("프록시 API가 정상 작동합니다."));
            } else {
                log.warn("프록시 테스트 실패: 응답이 비어있음");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.fail("프록시 테스트 실패: 응답이 비어있습니다."));
            }
            
        } catch (Exception e) {
            log.error("프록시 테스트 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.fail("프록시 테스트 실패: " + e.getMessage()));
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
