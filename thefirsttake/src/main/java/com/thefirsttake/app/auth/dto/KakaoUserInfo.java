package com.thefirsttake.app.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카카오 사용자 정보 (내부 처리용)")
public class KakaoUserInfo {
    
    @Schema(description = "카카오 사용자 고유 ID", example = "123456789")
    private String id;
    
    @Schema(description = "카카오 사용자 닉네임", example = "홍길동")
    private String nickname;
    
    @Schema(description = "카카오 사용자 이메일", example = "user@example.com")
    private String email;
    
    @Schema(description = "카카오 사용자 프로필 이미지 URL", example = "https://k.kakaocdn.net/dn/profile.jpg")
    private String profileImage;
    
    public static KakaoUserInfo fromKakaoResponse(Map<String, Object> response) {
        KakaoUserInfo userInfo = new KakaoUserInfo();
        userInfo.setId(String.valueOf(response.get("id")));
        
        // properties에서 nickname, profile_image 추출
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) response.get("properties");
        if (properties != null) {
            userInfo.setNickname((String) properties.get("nickname"));
            userInfo.setProfileImage((String) properties.get("profile_image"));
        }
        
        // kakao_account에서 email 추출
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
        if (kakaoAccount != null) {
            userInfo.setEmail((String) kakaoAccount.get("email"));
        }
        
        return userInfo;
    }
}
