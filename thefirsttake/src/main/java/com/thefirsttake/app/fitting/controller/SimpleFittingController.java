package com.thefirsttake.app.fitting.controller;

import com.thefirsttake.app.common.response.CommonResponse;
import com.thefirsttake.app.fitting.client.FitRoomApiClient;
import com.thefirsttake.app.fitting.dto.response.FittingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
    @Qualifier("redisTemplate")
    private final RedisTemplate<String, String> redisTemplate;
    
    public SimpleFittingController(FitRoomApiClient fitRoomClient, RestTemplate restTemplate, @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.fitRoomClient = fitRoomClient;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 가상피팅 실행 - 완료될 때까지 대기 후 다운로드 링크 반환
     */
    @PostMapping("/try-on")
    @Operation(
        summary = "가상피팅 실행",
        description = "모델 이미지와 상의/하의 상품 ID를 받아서 Redis에서 이미지 URL을 조회하고 FitRoom API를 통해 가상피팅을 실행합니다.\n\n" +
                     "**처리 과정**:\n" +
                     "1. Redis에서 상품 ID로 이미지 URL 조회\n" +
                     "2. FitRoom API로 가상피팅 작업 생성\n" +
                     "3. 작업 완료까지 폴링 대기\n" +
                     "4. 결과 이미지 다운로드 URL 반환\n\n" +
                     "**Content-Type**: multipart/form-data\n\n" +
                     "**파라미터**:\n" +
                     "- model_image: 모델 사진 파일 (필수)\n" +
                     "- upper_product_id: 상의 상품 ID (선택)\n" +
                     "- lower_product_id: 하의 상품 ID (선택)\n" +
                     "- hd_mode: HD 모드 여부 (기본값: false)\n\n" +
                     "**파일 업로드 요구사항**:\n" +
                     "• **Content-Type**: multipart/form-data\n" +
                     "• **파라미터명**: model_image (필수)\n" +
                     "• **파일 형식**: JPG, JPEG, PNG (권장: JPG)\n" +
                     "• **최대 파일 크기**: 10MB\n" +
                     "• **권장 파일 크기**: 2-5MB\n" +
                     "• **이미지 해상도**: 512x512 ~ 1024x1024 픽셀\n" +
                     "• **권장 해상도**: 768x768 픽셀\n\n" +
                     "**프론트엔드 구현 가이드**:\n" +
                     "```javascript\n" +
                     "// FormData 사용 예시\n" +
                     "const formData = new FormData();\n" +
                     "formData.append('model_image', fileInput.files[0]);\n" +
                     "formData.append('upper_product_id', '12345');\n" +
                     "formData.append('hd_mode', 'false');\n" +
                     "\n" +
                     "fetch('/api/fitting/try-on', {\n" +
                     "  method: 'POST',\n" +
                     "  body: formData,\n" +
                     "  headers: {\n" +
                     "    'Authorization': 'Bearer ' + token\n" +
                     "  }\n" +
                     "});\n" +
                     "```\n\n" +
                     "**파일 검증 (프론트엔드)**:\n" +
                     "• 파일 크기: 1MB 이상, 10MB 이하\n" +
                     "• 파일 형식: image/jpeg, image/jpg, image/png\n" +
                     "• 이미지 크기: 최소 512x512, 최대 1024x1024\n\n" +
                     "**주의사항**: 상의 또는 하의 중 최소 하나는 필수입니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "가상피팅 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = "{\n" +
                           "  \"status\": \"success\",\n" +
                           "  \"message\": \"요청 성공\",\n" +
                           "  \"data\": {\n" +
                           "    \"success\": true,\n" +
                           "    \"message\": \"콤보 가상피팅이 완료되었습니다.\",\n" +
                           "    \"downloadUrl\": \"https://fitroom-results.s3.amazonaws.com/results/task_12345.jpg\",\n" +
                           "    \"taskId\": \"task_12345\"\n" +
                           "  }\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (상품 ID 누락 또는 Redis에서 URL 조회 실패)",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류 (FitRoom API 호출 실패 또는 처리 중 오류)",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        )
    })
    public ResponseEntity<CommonResponse> tryOn(
            @Parameter(name = "model_image", description = "모델 사진 파일 (MultipartFile)\n\n" +
                     "**파일 업로드 요구사항**:\n" +
                     "• Content-Type: multipart/form-data\n" +
                     "• 파라미터명: model_image\n" +
                     "• 파일 형식: JPG, JPEG, PNG\n" +
                     "• 최대 파일 크기: 10MB\n" +
                     "• 이미지 해상도: 512x512 ~ 1024x1024 픽셀\n\n" +
                     "**프론트엔드 예시**:\n" +
                     "```javascript\n" +
                     "const formData = new FormData();\n" +
                     "formData.append('model_image', fileInput.files[0]);\n" +
                     "```", required = true, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam("model_image") MultipartFile modelImage,
            @Parameter(name = "upper_product_id", description = "상의 상품 ID (Redis에서 URL 조회)", required = false, example = "12345")
            @RequestParam(value = "upper_product_id", required = false) String upperProductId,
            @Parameter(name = "lower_product_id", description = "하의 상품 ID (Redis에서 URL 조회)", required = false, example = "67890")
            @RequestParam(value = "lower_product_id", required = false) String lowerProductId,
            @Parameter(name = "hd_mode", description = "HD 모드 여부", required = false, example = "false")
            @RequestParam(value = "hd_mode", defaultValue = "false") boolean hdMode) {
        
        try {
            log.info("=== tryOn 메서드 시작 ===");
            log.info("가상피팅 시작: upperProductId={}, lowerProductId={}, hdMode={}", upperProductId, lowerProductId, hdMode);
            
            // 파라미터 유효성 검사
            if ((upperProductId == null || upperProductId.trim().isEmpty()) && 
                (lowerProductId == null || lowerProductId.trim().isEmpty())) {
                log.warn("상의 또는 하의 product_id가 제공되지 않았습니다.");
                return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("상의 또는 하의 product_id가 필요합니다."));
            }
            
            // Redis에서 product_id로 URL 조회
            String upperClothImageUrl = null;
            String lowerClothImageUrl = null;
            
            if (upperProductId != null && !upperProductId.trim().isEmpty()) {
                try {
                    String redisKey = "product_url_" + upperProductId.trim();
                    String encodedUrl = redisTemplate.opsForValue().get(redisKey);
                    if (encodedUrl != null) {
                        log.info("Redis에서 상의 encoded URL 조회: productId={}, encodedUrl={}", upperProductId, encodedUrl);
                        try {
                            // Base64 디코딩만 수행 (ChatController에서 Base64 인코딩만 사용)
                            upperClothImageUrl = new String(java.util.Base64.getDecoder().decode(encodedUrl), "UTF-8");
                            log.info("Redis에서 상의 URL 조회 성공 (Base64 디코딩만): productId={}, decodedUrl={}", upperProductId, upperClothImageUrl);
                        } catch (Exception e) {
                            log.warn("Base64 디코딩 실패, 원본 URL 사용: productId={}, error={}", upperProductId, e.getMessage());
                            // Base64 디코딩 실패 시 원본 URL 사용
                            upperClothImageUrl = encodedUrl;
                        }
                    } else {
                        log.warn("Redis에서 상의 URL을 찾을 수 없음: productId={}", upperProductId);
                    }
                } catch (Exception e) {
                    log.warn("Redis에서 상의 URL 조회 실패: productId={}, error={}", upperProductId, e.getMessage());
                }
            }
            
            if (lowerProductId != null && !lowerProductId.trim().isEmpty()) {
                try {
                    String redisKey = "product_url_" + lowerProductId.trim();
                    String encodedUrl = redisTemplate.opsForValue().get(redisKey);
                    if (encodedUrl != null) {
                        log.info("Redis에서 하의 encoded URL 조회: productId={}, encodedUrl={}", lowerProductId, encodedUrl);
                        try {
                            // Base64 디코딩만 수행 (ChatController에서 Base64 인코딩만 사용)
                            lowerClothImageUrl = new String(java.util.Base64.getDecoder().decode(encodedUrl), "UTF-8");
                            log.info("Redis에서 하의 URL 조회 성공 (Base64 디코딩만): productId={}, decodedUrl={}", lowerProductId, lowerClothImageUrl);
                        } catch (Exception e) {
                            log.warn("Base64 디코딩 실패, 원본 URL 사용: productId={}, error={}", lowerProductId, e.getMessage());
                            // Base64 디코딩 실패 시 원본 URL 사용
                            lowerClothImageUrl = encodedUrl;
                        }
                    } else {
                        log.warn("Redis에서 하의 URL을 찾을 수 없음: productId={}", lowerProductId);
                    }
                } catch (Exception e) {
                    log.warn("Redis에서 하의 URL 조회 실패: productId={}, error={}", lowerProductId, e.getMessage());
                }
            }
            
            // URL이 조회되지 않은 경우 에러 반환
            if ((upperProductId != null && upperClothImageUrl == null) || 
                (lowerProductId != null && lowerClothImageUrl == null)) {
                log.warn("일부 상품 URL을 Redis에서 찾을 수 없습니다.");
                return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("일부 상품 URL을 찾을 수 없습니다. product_id를 확인해주세요."));
            }
            
            // 1. FitRoom API로 콤보 작업 생성 (상의와 하의 모두 URL 방식)
            log.info("FitRoom API 호출 전 URL 확인: upperClothImageUrl={}, lowerClothImageUrl={}", upperClothImageUrl, lowerClothImageUrl);
            String taskId = fitRoomClient.createComboTaskWithUrls(modelImage, null, upperClothImageUrl, lowerClothImageUrl, hdMode);
            log.info("FitRoom 콤보 작업 생성 완료: taskId={}", taskId);
            
            // 2. 작업 완료까지 대기 (폴링)
            String downloadUrl = fitRoomClient.waitForCompletion(taskId);
            log.info("가상피팅 완료: taskId={}, downloadUrl={}", taskId, downloadUrl);
            
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
     * 콤보 가상피팅 실행 - 상하의를 동시에 입히는 가상피팅
     */
    @PostMapping("/try-on/combo")
    @Operation(
        summary = "콤보 가상피팅 실행",
        description = "모델 이미지와 상의/하의 상품 ID를 받아서 상의와 하의를 동시에 입히는 고급 가상피팅을 실행합니다.\n\n" +
                     "**처리 과정**:\n" +
                     "1. Redis에서 상의/하의 상품 ID로 이미지 URL 조회\n" +
                     "2. FitRoom API로 콤보 가상피팅 작업 생성\n" +
                     "3. 작업 완료까지 폴링 대기\n" +
                     "4. 결과 이미지 다운로드 URL 반환\n\n" +
                     "**Content-Type**: multipart/form-data\n\n" +
                     "**필수 파라미터**:\n" +
                     "- model_image: 모델 사진 파일\n" +
                     "- upper_product_id: 상의 상품 ID\n" +
                     "- lower_product_id: 하의 상품 ID\n\n" +
                     "**파일 업로드 요구사항**:\n" +
                     "• **Content-Type**: multipart/form-data\n" +
                     "• **파라미터명**: model_image (필수)\n" +
                     "• **파일 형식**: JPG, JPEG, PNG (권장: JPG)\n" +
                     "• **최대 파일 크기**: 10MB\n" +
                     "• **권장 파일 크기**: 2-5MB\n" +
                     "• **이미지 해상도**: 512x512 ~ 1024x1024 픽셀\n" +
                     "• **권장 해상도**: 768x768 픽셀\n\n"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "콤보 가상피팅 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = "{\n" +
                           "  \"status\": \"success\",\n" +
                           "  \"message\": \"요청 성공\",\n" +
                           "  \"data\": {\n" +
                           "    \"success\": true,\n" +
                           "    \"message\": \"콤보 가상피팅이 완료되었습니다.\",\n" +
                           "    \"downloadUrl\": \"https://fitroom-results.s3.amazonaws.com/results/combo_task_67890.jpg\",\n" +
                           "    \"taskId\": \"combo_task_67890\"\n" +
                           "  }\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (필수 파라미터 누락 또는 Redis에서 URL 조회 실패)",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류 (FitRoom API 호출 실패 또는 처리 중 오류)",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))
        )
    })
    public ResponseEntity<CommonResponse> tryOnCombo(
            @Parameter(name = "model_image", description = "모델 사진 파일 (MultipartFile)\n\n" +
                     "**파일 업로드 요구사항**:\n" +
                     "• Content-Type: multipart/form-data\n" +
                     "• 파라미터명: model_image\n" +
                     "• 파일 형식: JPG, JPEG, PNG\n" +
                     "• 최대 파일 크기: 10MB\n" +
                     "• 이미지 해상도: 512x512 ~ 1024x1024 픽셀\n\n" +
                     "**프론트엔드 예시**:\n" +
                     "```javascript\n" +
                     "const formData = new FormData();\n" +
                     "formData.append('model_image', fileInput.files[0]);\n" +
                     "```", required = true, content = @Content(mediaType = "multipart/form-data"))
            @RequestParam(value = "model_image", required = true) MultipartFile modelImage,
            @Parameter(name = "upper_product_id", description = "상의 상품 ID (Redis에서 URL 조회)", required = true, example = "12345")
            @RequestParam(value = "upper_product_id", required = true) String upperProductId,
            @Parameter(name = "lower_product_id", description = "하의 상품 ID (Redis에서 URL 조회)", required = true, example = "67890")
            @RequestParam(value = "lower_product_id", required = true) String lowerProductId) {
        System.out.println(upperProductId);
        System.out.println(lowerProductId);
        try {
            log.info("=== tryOnCombo 메서드 시작 ===");
            log.info("콤보 가상피팅 시작(원본): upperProductId={}, lowerProductId={}", upperProductId, lowerProductId);

            // 입력값 정규화: 양끝의 따옴표(" or ') 제거 및 trim
            upperProductId = normalizeProductId(upperProductId);
            lowerProductId = normalizeProductId(lowerProductId);
            log.info("콤보 가상피팅 시작(정규화): upperProductId={}, lowerProductId={}", upperProductId, lowerProductId);
            
            // Redis에서 product_id로 URL 조회
            String redisClothImageUrl = null;
            String redisLowerClothImageUrl = null;
            
            if (upperProductId != null && !upperProductId.trim().isEmpty()) {
                try {
                    String redisKey = "product_url_" + upperProductId.trim();
                    String encodedUrl = redisTemplate.opsForValue().get(redisKey);
                    if (encodedUrl != null) {
                        // 인코딩 없이 원본 값을 그대로 사용 + 앞뒤 공백 제거
                        redisClothImageUrl = encodedUrl.trim();
                    } else {
                        // log.warn("Redis에서 상의 URL을 찾을 수 없음: productId={}", upperProductId);
                    }
                } catch (Exception e) {
                    // log.warn("Redis에서 상의 URL 조회 실패: productId={}, error={}", upperProductId, e.getMessage());
                }
            }
            
            if (lowerProductId != null && !lowerProductId.trim().isEmpty()) {
                try {
                    String redisKey = "product_url_" + lowerProductId.trim();
                    String encodedUrl = redisTemplate.opsForValue().get(redisKey);
                    if (encodedUrl != null) {
                        // 인코딩 없이 원본 값을 그대로 사용 + 앞뒤 공백 제거
                        redisLowerClothImageUrl = encodedUrl.trim();
                    } else {
                        // log.warn("Redis에서 하의 URL을 찾을 수 없음: productId={}", lowerProductId);
                    }
                } catch (Exception e) {
                    // log.warn("Redis에서 하의 URL 조회 실패: productId={}, error={}", lowerProductId, e.getMessage());
                }
            }
            
            // 파라미터 유효성 검사
            if (modelImage == null || modelImage.isEmpty()) {
                log.warn("모델 이미지가 제공되지 않았습니다.");
                return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("모델 이미지 파일이 필요합니다."));
            }

            if (upperProductId == null || upperProductId.trim().isEmpty() ||
                lowerProductId == null || lowerProductId.trim().isEmpty()) {
                log.warn("상품 ID가 제공되지 않았습니다. upperProductId={}, lowerProductId={}", upperProductId, lowerProductId);
                return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("상의/하의 product_id가 필요합니다."));
            }

            if (redisClothImageUrl == null || redisLowerClothImageUrl == null) {
                log.warn("Redis에서 상품 URL을 찾을 수 없습니다. upperUrl={}, lowerUrl={}", redisClothImageUrl, redisLowerClothImageUrl);
                String diag = buildUrlDiagnostics("upper", redisClothImageUrl) + "\n" + buildUrlDiagnostics("lower", redisLowerClothImageUrl);
                return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("일부 상품 URL을 찾을 수 없습니다. product_id를 확인해주세요.\n" + diag));
            }

            // 1. FitRoom API로 콤보 작업 생성 (항상 URL 방식 사용)
            String taskId;
            log.info("FitRoom API 호출 전 URL 방식: modelImage={}, upperUrl={}, lowerUrl={}", 
                modelImage != null ? "EXISTS" : "null", redisClothImageUrl, redisLowerClothImageUrl);
            taskId = fitRoomClient.createComboTaskWithUrls(modelImage, null, redisClothImageUrl, redisLowerClothImageUrl, false);
            log.info("FitRoom 콤보 작업 생성 완료 (URL 방식): taskId={}", taskId);
            
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
        description = "CORS 문제 해결을 위한 이미지 프록시 API입니다. 외부 이미지 URL을 받아서 CORS 헤더와 함께 이미지 바이너리를 반환합니다.\n\n" +
                     "**사용 목적**:\n" +
                     "- 외부 이미지 URL의 CORS 정책으로 인한 브라우저 차단 해결\n" +
                     "- 상품 이미지나 가상피팅 결과 이미지의 안전한 로딩\n" +
                     "- 캐시 제어를 통한 성능 최적화\n\n" +
                     "**처리 과정**:\n" +
                     "1. URL 디코딩 및 유효성 검사\n" +
                     "2. 외부 이미지 다운로드\n" +
                     "3. CORS 헤더 설정\n" +
                     "4. 이미지 바이너리 반환\n\n" +
                     "**응답 헤더**:\n" +
                     "- Access-Control-Allow-Origin: *\n" +
                     "- Cache-Control: public, max-age=3600\n" +
                     "- Content-Type: image/jpeg (또는 원본 타입)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "이미지 프록시 성공 - 이미지 바이너리 데이터 반환",
            content = @Content(
                mediaType = "image/jpeg",
                schema = @Schema(type = "string", format = "binary"),
                examples = @ExampleObject(
                    name = "이미지 바이너리",
                    value = "이미지 바이너리 데이터 (Content-Type: image/jpeg)"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (imageUrl 파라미터 누락 또는 잘못된 URL 형식)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "에러 응답",
                    value = "{\n" +
                           "  \"status\": \"fail\",\n" +
                           "  \"message\": \"이미지 URL이 필요합니다.\",\n" +
                           "  \"data\": null\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류 (이미지 다운로드 실패, 네트워크 오류 등)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class)
            )
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
        description = "이미지 프록시 API가 정상 작동하는지 테스트하는 엔드포인트입니다.\n\n" +
                     "**테스트 과정**:\n" +
                     "1. 테스트용 이미지 URL로 외부 이미지 다운로드 시도\n" +
                     "2. 다운로드 성공 여부 확인\n" +
                     "3. API 정상 작동 상태 반환\n\n" +
                     "**사용 목적**:\n" +
                     "- 프록시 API 서비스 상태 확인\n" +
                     "- 외부 이미지 다운로드 기능 검증\n" +
                     "- 네트워크 연결 상태 테스트"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "프록시 API 정상 작동",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = "{\n" +
                           "  \"status\": \"success\",\n" +
                           "  \"message\": \"프록시 API가 정상 작동합니다.\",\n" +
                           "  \"data\": null\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "프록시 API 오류 (외부 이미지 다운로드 실패)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class)
            )
        )
    })
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
        description = "가상피팅 작업의 현재 상태를 확인합니다.\n\n" +
                     "**현재 상태**: 향후 비동기 처리 시 사용 예정\n" +
                     "**예정 기능**:\n" +
                     "- 작업 진행률 조회\n" +
                     "- 완료/실패 상태 확인\n" +
                     "- 예상 완료 시간 제공\n" +
                     "- 에러 메시지 조회\n\n" +
                     "**현재 응답**: 개발 중 메시지 반환"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "상태 확인 응답 (현재는 개발 중 메시지)",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string"),
                examples = @ExampleObject(
                    name = "개발 중 응답",
                    value = "Status check endpoint - to be implemented for async processing"
                )
            )
        )
    })
    public ResponseEntity<String> getStatus(
            @Parameter(description = "작업 ID", required = true, example = "task_12345")
            @PathVariable String taskId) {
        // 나중에 비동기 처리가 필요할 때 구현할 수 있음
        return ResponseEntity.ok("Status check endpoint - to be implemented for async processing");
    }
    
    /**
     * URL에서 이미지 다운로드
     */
    private byte[] downloadImageFromUrl(String imageUrl) {
        try {
            log.info("=== 이미지 다운로드 시작 ===");
            log.info("원본 URL: {}", imageUrl);
            log.info("URL 길이: {} characters", imageUrl.length());
            log.info("URL에 & 포함 여부: {}", imageUrl.contains("&"));
            log.info("URL에 %26 포함 여부: {}", imageUrl.contains("%26"));
            
            // presigned URL은 이미 완전한 형태이므로 추가 디코딩 없이 그대로 사용
            ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);
            
            if (response.getBody() == null || response.getBody().length == 0) {
                throw new RuntimeException("다운로드된 이미지가 비어있습니다: " + imageUrl);
            }
            
            log.info("이미지 다운로드 완료: {} ({} bytes)", imageUrl, response.getBody().length);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("이미지 다운로드 실패: {}", imageUrl, e);
            throw new RuntimeException("이미지 다운로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * product_id 입력값 정규화: 앞뒤 공백 제거 후 양끝의 따옴표(' 또는 ") 제거
     */
    private String normalizeProductId(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                s = s.substring(1, s.length() - 1).trim();
            }
        }
        return s;
    }

    // 로그에 민감한 쿼리 파라미터가 있는 presigned URL을 출력하지 않도록 마스킹
    private String maskUrlForLog(String url) {
        try {
            if (url == null) return null;
            int q = url.indexOf('?');
            if (q < 0) return url; // 쿼리 없음
            String base = url.substring(0, q);
            return base + "?[masked]";
        } catch (Exception e) {
            return "[mask-failed]";
        }
    }

    private String buildUrlDiagnostics(String label, String url) {
        try {
            if (url == null) return label + ": url=null";
            String head = url.substring(0, Math.min(50, url.length()));
            String tail = url.substring(Math.max(0, url.length() - 50));
            return String.format(
                "%s: len=%d, http=%s, xamz=%s, head='%s', tail='%s'",
                label,
                url.length(),
                String.valueOf(url.startsWith("http")),
                String.valueOf(url.contains("X-Amz-")),
                head,
                tail
            );
        } catch (Exception e) {
            return label + ": diag-failed=" + e.getMessage();
        }
    }
}
