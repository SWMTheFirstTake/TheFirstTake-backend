package com.thefirsttake.app.auth.service;

import com.thefirsttake.app.auth.dto.KakaoUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class KakaoAuthService {
    
    @Value("${kakao.client-id}")
    private String clientId;
    
    @Value("${kakao.client-secret}")
    private String clientSecret;
    
    @Value("${kakao.redirect-uri}")
    private String redirectUri;
    
    private final RestTemplate restTemplate;
    
    public KakaoAuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public String getAccessToken(String authorizationCode) throws Exception {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        System.out.println("redirectUri: " + redirectUri);
        params.add("code", authorizationCode);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getBody() == null || !response.getBody().containsKey("access_token")) {
                throw new Exception("카카오 액세스 토큰 요청 실패");
            }
            
            return (String) response.getBody().get("access_token");
            
        } catch (Exception e) {
            log.error("카카오 토큰 요청 실패: {}", e.getMessage());
            throw new Exception("카카오 토큰 요청 중 오류 발생: " + e.getMessage());
        }
    }
    
    public KakaoUserInfo getUserInfo(String accessToken) throws Exception {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/json");
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUrl, HttpMethod.GET, request, Map.class);
            
            if (response.getBody() == null) {
                throw new Exception("카카오 사용자 정보 조회 실패");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();
            return KakaoUserInfo.fromKakaoResponse(responseBody);
            
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new Exception("카카오 사용자 정보 조회 중 오류 발생: " + e.getMessage());
        }
    }
    
    public void logout(String accessToken) throws Exception {
        String logoutUrl = "https://kapi.kakao.com/v1/user/logout";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/json");
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(logoutUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("카카오 로그아웃 성공");
            } else {
                log.warn("카카오 로그아웃 응답: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("카카오 로그아웃 실패: {}", e.getMessage());
            // 카카오 로그아웃 실패해도 서버 측 로그아웃은 진행
        }
    }
}
