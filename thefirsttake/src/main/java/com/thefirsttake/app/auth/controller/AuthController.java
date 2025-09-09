package com.thefirsttake.app.auth.controller;

import com.thefirsttake.app.auth.dto.KakaoUserInfo;
import com.thefirsttake.app.auth.dto.UserInfo;
import com.thefirsttake.app.auth.service.JwtService;
import com.thefirsttake.app.auth.service.KakaoAuthService;
import com.thefirsttake.app.common.response.CommonResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final KakaoAuthService kakaoAuthService;
    private final JwtService jwtService;
    
    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(@RequestParam String code, HttpServletResponse response) {
        try {
            log.info("카카오 로그인 콜백 시작. code: {}", code);
            
            // 1. Authorization Code로 Access Token 받기
            String accessToken = kakaoAuthService.getAccessToken(code);
            log.info("카카오 액세스 토큰 획득 성공");
            
            // 2. Access Token으로 사용자 정보 조회
            KakaoUserInfo userInfo = kakaoAuthService.getUserInfo(accessToken);
            log.info("카카오 사용자 정보 조회 성공. userId: {}", userInfo.getId());
            
            // 3. JWT 토큰 생성
            String jwtToken = jwtService.generateToken(userInfo.getId(), userInfo.getNickname());
            
            // 4. HttpOnly 쿠키로 토큰 설정
            Cookie jwtCookie = new Cookie("jwt", jwtToken);
            jwtCookie.setHttpOnly(true);           // XSS 방지
            jwtCookie.setSecure(true);             // HTTPS에서만 전송
            jwtCookie.setPath("/");                // 전체 도메인에서 사용
            jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
            // jwtCookie.setSameSite("Strict");    // CSRF 방지 (Spring Boot 2.6+에서 지원)
            
            response.addCookie(jwtCookie);
            
            // 5. 프론트엔드로 리다이렉트
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("https://the-second-take.com/auth/success"));
            
            log.info("카카오 로그인 성공. 프론트엔드로 리다이렉트");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
            
        } catch (Exception e) {
            log.error("카카오 로그인 실패: {}", e.getMessage());
            
            // 실패 시 에러 페이지로 리다이렉트
            String errorUrl = "https://the-second-take.com/auth/error?message=" + 
                URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(errorUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse> logout(HttpServletResponse response) {
        try {
            // 쿠키 삭제
            Cookie jwtCookie = new Cookie("jwt", null);
            jwtCookie.setMaxAge(0);
            jwtCookie.setPath("/");
            jwtCookie.setHttpOnly(true);
            response.addCookie(jwtCookie);
            
            log.info("로그아웃 성공");
            return ResponseEntity.ok(CommonResponse.success("로그아웃 성공"));
            
        } catch (Exception e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.fail("로그아웃 실패"));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<CommonResponse> getCurrentUser(HttpServletRequest request) {
        try {
            // 쿠키에서 JWT 토큰 추출
            String jwtToken = extractJwtFromCookies(request.getCookies());
            
            if (jwtToken == null || !jwtService.validateToken(jwtToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("인증이 필요합니다"));
            }
            
            // 토큰에서 사용자 정보 추출
            String userId = jwtService.getUserIdFromToken(jwtToken);
            String nickname = jwtService.getNicknameFromToken(jwtToken);
            
            UserInfo userInfo = new UserInfo(userId, nickname);
            
            return ResponseEntity.ok(CommonResponse.success(userInfo));
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.fail("인증 실패"));
        }
    }
    
    private String extractJwtFromCookies(Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
