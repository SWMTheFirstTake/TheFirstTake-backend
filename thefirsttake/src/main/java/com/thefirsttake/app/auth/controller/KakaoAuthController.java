//package com.thefirsttake.app.auth.controller;
//
//import com.thefirsttake.app.auth.dto.KakaoUserInfo;
//import com.thefirsttake.app.auth.dto.response.TokenResponse;
//import com.thefirsttake.app.auth.repository.UserRepository;
//import com.thefirsttake.app.auth.service.KakaoLogoutService;
//import com.thefirsttake.app.auth.service.KakaoOauthService;
//import com.thefirsttake.app.common.dto.UserDto;
//import com.thefirsttake.app.common.response.ApiResponse;
//import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.JwtException;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.ExampleObject;
//import io.swagger.v3.oas.annotations.media.Schema;
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.data.redis.core.RedisTemplate;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.time.Duration;
//import java.util.Date;
//import java.util.Map;
//import java.util.Optional;
////import io.swagger.v3.oas.annotations.responses.ApiResponse;
//@RestController
//@RequiredArgsConstructor
//public class KakaoAuthController {
//
//    private final KakaoOauthService kakaoOauthService;
//    private final UserRepository userRepository;
//    private final KakaoLogoutService kakaoLogoutService;
//
//    private final byte[] SECRET_KEY = "X9p$eTqN7#vF3@LmPz1!tUcRg*YkWqZ0oAsJdLxCvBnMhQeRfTgYhUjIkOlPnMbVcXsZaSdFgHiJkL11111".getBytes(StandardCharsets.UTF_8);
//
//
//    // ReponseEntity<?>를 썼었음
//    // @RequestParam String code였음
//    @Operation(
//            summary = "카카오 로그인 콜백",
//            responses = {
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                            responseCode = "200",
//                            description = "로그인 성공",
//                            content = @Content(
//                                    mediaType = "application/json",
//                                    schema = @Schema(implementation = ApiResponse.class),
//                                    examples = @ExampleObject(
//                                            name = "성공 응답 예시",
//                                            summary = "로그인 성공 응답",
//                                            value = """
//                    {
//                      "status": "success",
//                      "message": "Login successful",
//                      "data": {
//                        "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
//                        "refresh_token": "eyJhbGciOiJIUzI1NiIfdmsklFDFFDKLDFC1..."
//                      }
//                    }
//                    """
//                                    )
//                            )
//                    )
//            }
//    )
//    @PostMapping("/api/auth/callback/kakao")
//    public ResponseEntity<?> kakaoCallback(@RequestBody Map<String, String> requestBody, HttpServletResponse response) throws IOException {
//        String code=requestBody.get("code");
//        // 1. 인가 코드를 이용해 카카오 액세스 토큰 요청
//        Map<String,String> kakaoToken = kakaoOauthService.getAccessTokenAndRefreshToken(code);
//        String kakaoAccessToken=kakaoToken.get("access_token");
//        String kakaoRefreshToken=kakaoToken.get("refresh_token");
//        // 2. 액세스 토큰으로 사용자 정보 요청
//        KakaoUserInfo userInfo = kakaoOauthService.getUserInfo(kakaoAccessToken);
//        // userInfo는 id와 email
//        // 3. 사용자 정보로 우리 시스템의 JWT를 생성 or 로그인 처리
//        // jwt에 액세스 토큰이 저장된 것은 아님
//        UserDto user = kakaoOauthService.loginOrRegister(userInfo,kakaoAccessToken,kakaoRefreshToken);
//
//        // 4. access token과 refresh 토큰 생성
//        TokenResponse tokenResponse=kakaoOauthService.createTokenPair(user);
//        // 프론트에 JWT 전달 (JSON이나 쿠키 등 방식 선택)
//        String accessToken = tokenResponse.getAccessToken();
//        Cookie accessTokenCookie = new Cookie("access_token", accessToken);
//        accessTokenCookie.setPath("/");       // 전체 경로에서 유효
//        accessTokenCookie.setHttpOnly(true); // 서버에서만 접근 가능하도록 설정 (보안 권장)
//        accessTokenCookie.setMaxAge(3600);   // 1시간 동안 유효
//        response.addCookie(accessTokenCookie);
//
//        // 리프레시 토큰도 발급
//        String refreshToken = tokenResponse.getRefreshToken();
//        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
//
//        refreshTokenCookie.setPath("/"); // 전체 경로에서 유효
//        refreshTokenCookie.setMaxAge(86400);  // 24시간 동안 유효
//        response.addCookie(refreshTokenCookie);
//        System.out.println(accessToken);
//        System.out.println(refreshToken);
//        return ResponseEntity.ok(new ApiResponse("success", "Login successful", tokenResponse));
//    }
//    @PostMapping("/api/auth/logout/kakao")
//    public ResponseEntity<?> logout(HttpServletRequest request) {
//        System.out.println("logout gogo");
//        try {
//            // accesstoken
//            String accessToken=getAccessTokenFromCookies(request);
//            System.out.println(accessToken);
//            String userId = extractUserIdFromToken(accessToken);
////            System.out.println(userId);
//            // 카카오 로그아웃 API 호출
//            kakaoLogoutService.logout(userId);
//
//            // 클라이언트 측에서 세션 종료 후 응답 반환
//            return ResponseEntity.ok("로그아웃 성공");
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("로그아웃 실패");
//        }
//    }
//    public String extractUserIdFromToken(String token) {
//        try {
//            return Jwts.parser()
//                    .setSigningKey(SECRET_KEY)
//                    .parseClaimsJws(token)
//                    .getBody()
//                    .getSubject();
//        } catch (ExpiredJwtException e) {
//            // 만료된 토큰 처리
//            throw new RuntimeException("Access Token expired");
//        } catch (JwtException e) {
//            // 유효하지 않은 토큰
//            throw new RuntimeException("Invalid JWT token");
//        }
//    }
//
//    private String getAccessTokenFromCookies(HttpServletRequest request) {
//        // 쿠키에서 access_token을 찾는 로직
//        Cookie[] cookies = request.getCookies();
//        System.out.println(cookies);
//        if (cookies != null) {
//            for (Cookie cookie : cookies) {
//                if ("access_token".equals(cookie.getName())) {
//                    return cookie.getValue();
//                }
//            }
//        }
//        return null;
//    }
//    private String generateAccessToken(String id) {
//        return Jwts.builder()
//                .setSubject(id)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1시간 유효
//                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
//                .compact();
//    }
//    private String generateRefreshToken(String id) {
//        return Jwts.builder()
//                .setSubject(id)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7)) // 7일 유효
//                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
//                .compact();
//    }
//}
