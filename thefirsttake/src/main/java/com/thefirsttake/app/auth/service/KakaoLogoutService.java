package com.thefirsttake.app.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoLogoutService {
    private static final String KAKAO_LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout";  // 카카오 로그아웃 API
    private final RedisTemplate<String, String> redisTemplate;
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;
    public void logout(String userId) {
        // 1. Redis에서 accessToken 가져오기
        String accessToken = redisTemplate.opsForValue().get("kakao:access_token:" + userId);

        if (accessToken == null) {
            System.out.println("Access token이 존재하지 않습니다.");
            String refreshToken = redisTemplate.opsForValue().get("kakao:refresh_token:" + userId);
            if (refreshToken != null) {
                accessToken = refreshKakaoAccessToken(refreshToken);

                if (accessToken == null) {
                    System.out.println("Access token 재발급 실패");
                    return;
                }

                // 재발급한 access token Redis에 저장 (선택)
                redisTemplate.opsForValue().set(
                        "kakao:access_token:" + userId,
                        accessToken,
                        Duration.ofHours(6) // 카카오 기본 access token 유효기간
                );
            } else {
                System.out.println("Refresh token도 존재하지 않음");
                return;
            }
            return;
        }
        System.out.println(accessToken);
        // 2. HTTP 요청 구성
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 3. 로그아웃 API 요청
        ResponseEntity<String> response = restTemplate.exchange(
                KAKAO_LOGOUT_URL,
                HttpMethod.POST,
                entity,
                String.class
        );
        System.out.println(response);

        // 4. 응답 처리
        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("kakao logout success");
            // 5. 로그아웃 성공 시, Redis에서 accessToken과 refreshToken 삭제
            redisTemplate.delete("kakao:access_token:" + userId);
            redisTemplate.delete("kakao:refresh_token:" + userId);
            redisTemplate.delete("access_token:" + userId);
            redisTemplate.delete("refresh_token:" + userId);
        } else {
            System.out.println("kakao logout fail");
            System.out.println("카카오 로그아웃 실패: " + response.getStatusCode());
        }
    }

    public String refreshKakaoAccessToken(String refreshToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", clientId); // 카카오 앱 REST API 키
        params.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://kauth.kakao.com/oauth/token",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("access_token")) {
                    return (String) body.get("access_token");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // 실패 시 null 반환
    }

}
