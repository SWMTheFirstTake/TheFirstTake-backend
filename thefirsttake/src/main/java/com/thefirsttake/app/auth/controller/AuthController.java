package com.thefirsttake.app.auth.controller;

import com.thefirsttake.app.auth.dto.KakaoUserInfo;
import com.thefirsttake.app.auth.dto.UserInfo;
import com.thefirsttake.app.auth.service.JwtService;
import com.thefirsttake.app.auth.service.KakaoAuthService;
import com.thefirsttake.app.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증 관리", description = "카카오 OAuth 로그인 및 사용자 인증 관련 API")
public class AuthController {
    
    private final KakaoAuthService kakaoAuthService;
    private final JwtService jwtService;
    
    @Operation(
        summary = "카카오 로그인 콜백 처리",
        description = """
            카카오 OAuth 인증 완료 후 호출되는 콜백 엔드포인트입니다.
            
            **프론트엔드에서 사용하는 방법:**
            1. 사용자가 카카오 로그인 버튼 클릭
            2. 카카오 인증 페이지로 리다이렉트: `https://kauth.kakao.com/oauth/authorize?client_id={CLIENT_ID}&redirect_uri={REDIRECT_URI}&response_type=code`
            3. 카카오에서 이 엔드포인트로 authorization code와 함께 리다이렉트
            4. 서버에서 JWT 토큰을 HttpOnly 쿠키에 저장
            5. 메인 페이지로 리다이렉트
            
            **보안 특징:**
            - JWT 토큰이 HttpOnly 쿠키에 저장되어 XSS 공격 방지
            - Secure 플래그로 HTTPS에서만 전송
            - 7일 자동 만료
            """,
        tags = {"인증 관리"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "302",
            description = "로그인 성공 - 메인 페이지로 리다이렉트",
            content = @Content(
                mediaType = "text/html",
                examples = @ExampleObject(
                    name = "성공 예시",
                    description = "로그인 성공 시 메인 페이지로 리다이렉트됩니다.",
                    value = "Redirect to https://the-second-take.com/"
                )
            )
        ),
        @ApiResponse(
            responseCode = "302", 
            description = "로그인 실패 - 에러 페이지로 리다이렉트",
            content = @Content(
                mediaType = "text/html",
                examples = @ExampleObject(
                    name = "실패 예시",
                    description = "로그인 실패 시 에러 페이지로 리다이렉트됩니다.",
                    value = "Redirect to https://the-second-take.com/auth/error?message=에러메시지"
                )
            )
        )
    })
    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(
        @Parameter(
            description = "카카오에서 전달받은 authorization code",
            example = "abc123def456",
            required = true
        )
        @RequestParam String code, 
        HttpServletResponse response
    ) {
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
            headers.setLocation(URI.create("https://the-second-take.com/"));
            
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
    
    @Operation(
        summary = "로그아웃",
        description = """
            사용자 로그아웃을 처리합니다.
            
            **동작 방식:**
            1. HttpOnly 쿠키에서 JWT 토큰을 삭제
            2. 쿠키 만료 시간을 0으로 설정하여 즉시 삭제
            3. 로그아웃 성공 응답 반환
            
            **프론트엔드에서 사용하는 방법:**
            ```javascript
            const response = await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'include'  // 쿠키 포함
            });
            const result = await response.json();
            if (result.status === 'success') {
                // 로그아웃 성공 처리
                window.location.href = '/login';
            }
            ```
            """,
        tags = {"인증 관리"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    description = "로그아웃이 성공적으로 처리되었습니다.",
                    value = """
                    {
                        "status": "success",
                        "message": "로그아웃 성공",
                        "data": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "로그아웃 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "실패 응답",
                    description = "로그아웃 처리 중 오류가 발생했습니다.",
                    value = """
                    {
                        "status": "fail",
                        "message": "로그아웃 실패",
                        "data": null
                    }
                    """
                )
            )
        )
    })
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
    
    @Operation(
        summary = "현재 사용자 정보 조회",
        description = """
            현재 로그인된 사용자의 정보를 조회합니다.
            
            **인증 방식:**
            - HttpOnly 쿠키에 저장된 JWT 토큰을 사용하여 인증
            - 쿠키가 없거나 토큰이 유효하지 않으면 401 Unauthorized 반환
            
            **프론트엔드에서 사용하는 방법:**
            ```javascript
            const response = await fetch('/api/auth/me', {
                method: 'GET',
                credentials: 'include'  // 쿠키 포함
            });
            const result = await response.json();
            
            if (response.ok && result.status === 'success') {
                const user = result.data;
                console.log('사용자 ID:', user.userId);
                console.log('닉네임:', user.nickname);
            } else {
                // 로그인되지 않은 사용자
                console.log('로그인이 필요합니다.');
            }
            ```
            
            **사용자 정보 확인 시점:**
            - 페이지 로드 시 사용자 로그인 상태 확인
            - 로그인 후 사용자 정보 표시
            - API 호출 전 인증 상태 확인
            """,
        tags = {"인증 관리"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "사용자 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    description = "로그인된 사용자의 정보를 반환합니다.",
                    value = """
                    {
                        "status": "success",
                        "message": "요청 성공",
                        "data": {
                            "userId": "123456789",
                            "nickname": "홍길동"
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 로그인이 필요합니다",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                    name = "인증 실패 응답",
                    description = "JWT 토큰이 없거나 유효하지 않습니다.",
                    value = """
                    {
                        "status": "fail",
                        "message": "인증이 필요합니다",
                        "data": null
                    }
                    """
                )
            )
        )
    })
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
