package com.thefirsttake.app.fitting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FittingResponse {
    private boolean success;
    private String message;
    private String downloadUrl;  // FitRoom의 임시 다운로드 URL
    private String taskId;
}
