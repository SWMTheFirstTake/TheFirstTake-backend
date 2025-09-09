package com.thefirsttake.app.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KakaoUserInfo {
    private String id;
    private String nickname;
    private String email;
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
