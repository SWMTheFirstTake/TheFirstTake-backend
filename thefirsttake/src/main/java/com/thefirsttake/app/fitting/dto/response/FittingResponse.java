package com.thefirsttake.app.fitting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "가상피팅 응답 데이터")
public class FittingResponse {
    @Schema(description = "가상피팅 성공 여부", example = "true")
    private boolean success;
    
    @Schema(description = "응답 메시지", example = "가상피팅이 완료되었습니다.")
    private String message;
    
    @Schema(description = "결과 이미지 다운로드 URL", example = "https://storage.googleapis.com/...")
    private String downloadUrl;  // FitRoom의 임시 다운로드 URL
    
    @Schema(description = "FitRoom 작업 ID", example = "task_12345")
    private String taskId;
}
