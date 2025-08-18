package com.thefirsttake.app.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse {
    @Schema(example = "success")
    private String status;
    @Schema(example = "Login successful")
    private String message;
    @Schema(description = "응답 데이터")
    private Object data;
    // ✅ 정적 팩토리 메서드
    public static CommonResponse success(Object data) {
        return new CommonResponse("success", "요청 성공", data);
    }

    public static CommonResponse fail(String message) {
        return new CommonResponse("fail", message, null);
    }
}
