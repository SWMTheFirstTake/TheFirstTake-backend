//package com.thefirsttake.app.auth.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.thefirsttake.app.auth.dto.KakaoUserInfo;
//import com.thefirsttake.app.auth.dto.response.TokenResponse;
//import com.thefirsttake.app.auth.repository.UserRepository;
//import com.thefirsttake.app.common.dto.UserDto;
//import com.thefirsttake.app.common.exception.CustomException;
//import com.thefirsttake.app.security.JwtProvider;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestTemplate;
//
//
//
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class KakaoOauthService {
//
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final UserRepository userRepository;
//    private final RedisTemplate<String, String> redisTemplate;
//    private final JwtProvider jwtProvider; // JWT 생성용 컴포넌트
//
//
//    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
//    private String clientId;
//    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
//    private String redirectUri;
//    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
//    private String clientSecret;
//
//    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
//    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
//
//    /**
//     * 인가코드로 access token 요청
//     */
//    public Map<String,String> getAccessTokenAndRefreshToken(String code) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(new LinkedMultiValueMap<>() {{
//            add("grant_type", "authorization_code");
//            add("client_id", clientId);
//            add("client_secret", clientSecret);
//            add("redirect_uri", redirectUri);
//            add("code", code);
//        }}, headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_URL, request, String.class);
//            JsonNode json = objectMapper.readTree(response.getBody());
//            String accessToken = json.get("access_token").asText();
//            String refreshToken = json.get("refresh_token").asText();
//            // 액세스 토큰과 리프레시 토큰을 Map으로 반환
//            Map<String, String> tokens = new HashMap<>();
//            tokens.put("access_token", accessToken);
//            tokens.put("refresh_token", refreshToken);
//
//            return tokens;  // 액세스 토큰과 리프레시 토큰을 반환
//        } catch (Exception e) {
//            throw new RuntimeException("카카오 액세스 토큰 요청 실패", e);
//        }
//    }
//    /**
//     * access token으로 사용자 정보 요청
//     */
//    public KakaoUserInfo getUserInfo(String accessToken) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(accessToken);
//
//        HttpEntity<Void> request = new HttpEntity<>(headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.exchange(
//                    USER_INFO_URL,
//                    HttpMethod.GET,
//                    request,
//                    String.class
//            );
//
//            JsonNode json = objectMapper.readTree(response.getBody());
//            Long id = json.get("id").asLong();
//
//            JsonNode account = json.path("kakao_account");
//            String email = (account != null && account.has("email")) ? account.get("email").asText(null) : null;
//            return new KakaoUserInfo(id, email);
//        } catch (Exception e) {
//            throw new RuntimeException("카카오 사용자 정보 요청 실패", e);
//        }
//    }
//    public UserDto loginOrRegister(KakaoUserInfo userInfo, String accessToken, String refreshToken) {
//        try {
//            // 1. 이메일 기준으로 사용자 조회
//            Optional<UserDto> optionalUser = userRepository.findByEmail(userInfo.getEmail());
//
//            // 2. 사용자 정보 객체 선언
//            UserDto user;
//
//            if (optionalUser.isEmpty()) {
//                // 사용자 없으면 새로 생성해서 저장
//                UserDto newUser = new UserDto();
////                newUser.setEmail(userInfo.getEmail());
//                user = userRepository.save(newUser); // 저장 후 반환된 user 객체 저장
//            } else {
//                // 이미 있으면 가져옴
//                user = optionalUser.get();
//            }
//
//            // ✅ Redis에 access token 저장
//            redisTemplate.opsForValue().set(
//                    "kakao:access_token:" + user.getId(),
//                    accessToken,
//                    Duration.ofMinutes(60)
//            );
//            redisTemplate.opsForValue().set(
//                    "kakao:refresh_token:" + user.getId(),
//                    refreshToken,
//                    Duration.ofMinutes(60*24*7)
//            );
//            // 3. 사용자 정보로 JWT 생성
////            return jwtProvider.generateToken(user); // 예: 이메일, 사용자 ID 등 포함 가능
//            return user;
//        } catch (Exception e) {
//            // 전체 예외 처리: 문제가 발생한 위치에 따라 구체적인 예외 처리 추가 가능
//            throw new CustomException("로그인 또는 등록 과정에서 오류 발생", e);
//        }
//    }
//    public TokenResponse createTokenPair(UserDto user) {
//        String accessToken = jwtProvider.generateAccessToken(user);
//        redisTemplate.opsForValue().set(
//                "access_token:" + user.getId(),
//                accessToken,
//                Duration.ofHours(1)
//        );
//        String refreshToken = jwtProvider.generateRefreshToken(user);
//
//        redisTemplate.opsForValue().set(
//                "refresh_token:" + user.getId(),
//                refreshToken,
//                Duration.ofDays(7)
//        );
//
//        return new TokenResponse(accessToken, refreshToken);
//    }
//
//}
