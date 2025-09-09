package com.thefirsttake.app.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보")
public class UserInfo {
    
    @Schema(
        description = "카카오 사용자 고유 ID",
        example = "123456789",
        required = true
    )
    private String userId;
    
    @Schema(
        description = "카카오 사용자 닉네임",
        example = "홍길동",
        required = true
    )
    private String nickname;
}
