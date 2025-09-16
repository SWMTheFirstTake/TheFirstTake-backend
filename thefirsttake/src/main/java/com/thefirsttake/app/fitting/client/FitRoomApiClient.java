package com.thefirsttake.app.fitting.client;

import com.thefirsttake.app.fitting.dto.response.FitRoomTaskResponse;
import com.thefirsttake.app.fitting.dto.response.FitRoomTaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
@Slf4j
public class FitRoomApiClient {
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl = "https://platform.fitroom.app";
    
    public FitRoomApiClient(RestTemplate restTemplate, @Value("${fitroom.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }
    
    /**
     * FitRoom에 가상피팅 작업 생성
     */
    public String createTask(MultipartFile modelImage, MultipartFile clothImage, 
                           String clothType, boolean hdMode) {
        try {
            // Multipart 데이터 구성
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            
            // 파일을 ByteArrayResource로 변환
            formData.add("model_image", new ByteArrayResource(modelImage.getBytes()) {
                @Override
                public String getFilename() {
                    return modelImage.getOriginalFilename();
                }
            });
            formData.add("cloth_image", new ByteArrayResource(clothImage.getBytes()) {
                @Override
                public String getFilename() {
                    return clothImage.getOriginalFilename();
                }
            });
            formData.add("cloth_type", clothType);
            
            if (hdMode) {
                formData.add("hd_mode", "true");
            }
            
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-API-KEY", apiKey);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);
            
            // API 호출
            String url = baseUrl + "/api/tryon/v2/tasks";
            ResponseEntity<FitRoomTaskResponse> response = restTemplate.postForEntity(url, requestEntity, FitRoomTaskResponse.class);
            
            if (response.getBody() == null || response.getBody().getTaskId() == null) {
                throw new RuntimeException("FitRoom 작업 생성 실패: 응답이 null입니다.");
            }
            
            return response.getBody().getTaskId();
            
        } catch (IOException e) {
            log.error("파일 읽기 실패", e);
            throw new RuntimeException("파일 읽기 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("FitRoom 작업 생성 실패", e);
            throw new RuntimeException("FitRoom 작업 생성 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * FitRoom에 콤보 가상피팅 작업 생성 (상하의 동시)
     */
    public String createComboTask(MultipartFile modelImage, MultipartFile clothImage, 
                                MultipartFile lowerClothImage, boolean hdMode) {
        try {
            // Multipart 데이터 구성
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            
            // 파일을 ByteArrayResource로 변환
            formData.add("model_image", new ByteArrayResource(modelImage.getBytes()) {
                @Override
                public String getFilename() {
                    return modelImage.getOriginalFilename();
                }
            });
            formData.add("cloth_image", new ByteArrayResource(clothImage.getBytes()) {
                @Override
                public String getFilename() {
                    return clothImage.getOriginalFilename();
                }
            });
            formData.add("lower_cloth_image", new ByteArrayResource(lowerClothImage.getBytes()) {
                @Override
                public String getFilename() {
                    return lowerClothImage.getOriginalFilename();
                }
            });
            formData.add("cloth_type", "combo");
            
            if (hdMode) {
                formData.add("hd_mode", "true");
            }
            
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-API-KEY", apiKey);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);
            
            // API 호출
            String url = baseUrl + "/api/tryon/v2/tasks";
            ResponseEntity<FitRoomTaskResponse> response = restTemplate.postForEntity(url, requestEntity, FitRoomTaskResponse.class);
            
            if (response.getBody() == null || response.getBody().getTaskId() == null) {
                throw new RuntimeException("FitRoom 콤보 작업 생성 실패: 응답이 null입니다.");
            }
            
            return response.getBody().getTaskId();
            
        } catch (IOException e) {
            log.error("파일 읽기 실패", e);
            throw new RuntimeException("파일 읽기 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("FitRoom 콤보 작업 생성 실패", e);
            throw new RuntimeException("FitRoom 콤보 작업 생성 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * URL에서 이미지 다운로드 (presigned URL 직접 사용)
     */
    private byte[] downloadImageFromUrl(String imageUrl) {
        try {
            log.info("=== 이미지 다운로드 시작 ===");
            log.info("받은 URL: {}", imageUrl);
            log.info("URL 길이: {} characters", imageUrl.length());
            log.info("URL에 X-Amz 포함 여부: {}", imageUrl.contains("X-Amz"));
            
            // presigned URL을 그대로 사용 (추가 디코딩 없이)
            String finalUrl = imageUrl;
            log.info("최종 다운로드 URL: {}", finalUrl);
            
        // GET 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // GET 요청으로 다운로드 시도 (이미 인코딩된 presigned URL을 그대로 사용)
        log.info("RestTemplate GET 요청 시작: {}", finalUrl);
        java.net.URI uri = org.springframework.web.util.UriComponentsBuilder
            .fromUriString(finalUrl)
            .build(true) // encoded=true: 이미 인코딩된 URL 그대로 사용
            .toUri();
        ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, byte[].class);
        log.info("RestTemplate 응답 받음: status={}, contentLength={}",
            response.getStatusCode(), response.getBody() != null ? response.getBody().length : 0);
            
            if (response.getBody() == null || response.getBody().length == 0) {
                throw new RuntimeException("다운로드된 이미지가 비어있습니다: " + imageUrl);
            }
            
            log.info("이미지 다운로드 완료: {} ({} bytes)", finalUrl, response.getBody().length);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("=== 이미지 다운로드 실패 상세 정보 ===");
            log.error("실패한 URL: {}", imageUrl);
            log.error("에러 타입: {}", e.getClass().getSimpleName());
            log.error("에러 메시지: {}", e.getMessage());
            log.error("에러 스택 트레이스:", e);
            throw new RuntimeException("이미지 다운로드 실패: " + e.getMessage(), e);
        }
    }

    private String buildUrlDiagnostics(String label, String url) {
        try {
            if (url == null) return label + ": url=null";
            String head = url.substring(0, Math.min(80, url.length()));
            String tail = url.substring(Math.max(0, url.length() - 80));
            return String.format(
                "%sUrlDiag[len=%d,http=%s,xamz=%s,head='%s',tail='%s']",
                label,
                url.length(),
                String.valueOf(url.startsWith("http")),
                String.valueOf(url.contains("X-Amz-")),
                head,
                tail
            );
        } catch (Exception e) {
            return label + ":diag-failed=" + e.getMessage();
        }
    }
    
    /**
     * FitRoom에 콤보 가상피팅 작업 생성 (URL 방식 - 모델, 상하의 모두 URL 지원)
     * URL을 받으면 다운로드해서 파일로 변환하여 전송
     */
    public String createComboTaskWithUrls(MultipartFile modelImage, String modelImageUrl, 
                                        String clothImageUrl, String lowerClothImageUrl, boolean hdMode) {
        try {
            log.info("=== createComboTaskWithUrls 호출 ===");
            log.info("modelImageUrl: {}", modelImageUrl);
            log.info("clothImageUrl: {}", clothImageUrl);
            log.info("lowerClothImageUrl: {}", lowerClothImageUrl);

            // 앞뒤 공백 제거(개행/스페이스)만 수행. 내용은 절대 변경하지 않음
            if (modelImageUrl != null) modelImageUrl = modelImageUrl.trim();
            if (clothImageUrl != null) clothImageUrl = clothImageUrl.trim();
            if (lowerClothImageUrl != null) lowerClothImageUrl = lowerClothImageUrl.trim();
            
            // Multipart 데이터 구성
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            
            // 모델 이미지 처리 (파일 또는 URL)
            if (modelImage != null) {
                // 파일로 처리
                formData.add("model_image", new ByteArrayResource(modelImage.getBytes()) {
                    @Override
                    public String getFilename() {
                        return modelImage.getOriginalFilename();
                    }
                });
            } else if (modelImageUrl != null) {
                // URL에서 다운로드해서 파일로 처리
                byte[] imageBytes = downloadImageFromUrl(modelImageUrl);
                formData.add("model_image", new ByteArrayResource(imageBytes) {
                    @Override
                    public String getFilename() {
                        return "model.jpg";
                    }
                });
            }
            
            // 상의 이미지 처리 (URL에서 다운로드)
            if (clothImageUrl != null) {
                byte[] imageBytes;
                try {
                    imageBytes = downloadImageFromUrl(clothImageUrl);
                } catch (Exception ex) {
                    String diag = buildUrlDiagnostics("upper", clothImageUrl);
                    throw new RuntimeException("상의 이미지 다운로드 실패: " + ex.getMessage() + " | " + diag, ex);
                }
                formData.add("cloth_image", new ByteArrayResource(imageBytes) {
                    @Override
                    public String getFilename() {
                        return "cloth.jpg";
                    }
                });
            }
            
            // 하의 이미지 처리 (URL에서 다운로드)
            if (lowerClothImageUrl != null) {
                byte[] imageBytes;
                try {
                    imageBytes = downloadImageFromUrl(lowerClothImageUrl);
                } catch (Exception ex) {
                    String diag = buildUrlDiagnostics("lower", lowerClothImageUrl);
                    throw new RuntimeException("하의 이미지 다운로드 실패: " + ex.getMessage() + " | " + diag, ex);
                }
                formData.add("lower_cloth_image", new ByteArrayResource(imageBytes) {
                    @Override
                    public String getFilename() {
                        return "lower_cloth.jpg";
                    }
                });
            }
            
            formData.add("cloth_type", "combo");
            
            if (hdMode) {
                formData.add("hd_mode", "true");
            }
            
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-API-KEY", apiKey);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);
            
            // API 호출
            String url = baseUrl + "/api/tryon/v2/tasks";
            ResponseEntity<FitRoomTaskResponse> response = restTemplate.postForEntity(url, requestEntity, FitRoomTaskResponse.class);
            
            if (response.getBody() == null || response.getBody().getTaskId() == null) {
                throw new RuntimeException("FitRoom 콤보 작업 생성 실패 (URL 방식): 응답이 null입니다.");
            }
            
            return response.getBody().getTaskId();
            
        } catch (IOException e) {
            log.error("모델 이미지 파일 읽기 실패", e);
            throw new RuntimeException("모델 이미지 파일 읽기 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("FitRoom 콤보 작업 생성 실패 (URL 방식)", e);
            throw new RuntimeException("FitRoom 콤보 작업 생성 실패 (URL 방식): " + e.getMessage(), e);
        }
    }
    
    /**
     * 작업 완료까지 대기 (폴링)
     */
    public String waitForCompletion(String taskId) {
        int maxAttempts = 60; // 최대 5분 대기 (5초 간격)
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                Thread.sleep(5000); // 5초 대기
                
                FitRoomTaskStatus status = getTaskStatus(taskId);
                log.info("작업 상태 확인: taskId={}, status={}, progress={}", 
                        taskId, status.getStatus(), status.getProgress());
                
                if ("COMPLETED".equals(status.getStatus())) {
                    if (status.getDownloadSignedUrl() == null) {
                        String diag = String.format("status=%s, progress=%s, downloadUrl=null", status.getStatus(), status.getProgress());
                        throw new RuntimeException("완료되었지만 다운로드 URL이 없습니다. | " + diag);
                    }
                    return status.getDownloadSignedUrl();
                    
                } else if ("FAILED".equals(status.getStatus())) {
                    String diag = String.format("status=%s, progress=%s, error=%s", status.getStatus(), status.getProgress(), status.getError());
                    throw new RuntimeException("FitRoom 작업 실패: " + (status.getError() != null ? status.getError() : "(no message)") + " | " + diag);
                }
                
                attempt++;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("작업 대기 중 인터럽트 발생", e);
            } catch (Exception e) {
                log.warn("상태 확인 실패 (재시도): taskId={}, attempt={}, error={}", 
                        taskId, attempt, e.getMessage());
                attempt++;
                
                if (attempt >= maxAttempts) {
                    throw new RuntimeException("작업 상태 확인 최대 재시도 초과", e);
                }
            }
        }
        
        throw new RuntimeException("작업 완료 대기 시간 초과 (5분): taskId=" + taskId);
    }
    
    /**
     * 작업 상태 조회
     */
    private FitRoomTaskStatus getTaskStatus(String taskId) {
        try {
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = baseUrl + "/api/tryon/v2/tasks/" + taskId;
            ResponseEntity<FitRoomTaskStatus> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, FitRoomTaskStatus.class);
            
            if (response.getBody() == null) {
                throw new RuntimeException("상태 조회 응답이 null입니다.");
            }
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("작업 상태 조회 실패: taskId={}", taskId, e);
            throw new RuntimeException("작업 상태 조회 실패: " + e.getMessage(), e);
        }
    }
}
