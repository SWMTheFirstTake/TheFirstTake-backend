package com.thefirsttake.app.common.response;

import com.thefirsttake.app.auth.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    @Schema(example = "success")
    private String status;
    @Schema(example = "Login successful")
    private String message;
    @Schema(implementation = TokenResponse.class)
    private Object data;
}
