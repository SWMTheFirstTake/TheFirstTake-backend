package com.thefirsttake.app.auth.controller;

import com.thefirsttake.app.auth.dto.KakaoUserInfo;
import com.thefirsttake.app.auth.dto.UserInfo;
import com.thefirsttake.app.auth.service.JwtService;
import com.thefirsttake.app.auth.service.KakaoAuthService;
import com.thefirsttake.app.common.response.CommonResponse;
import com.thefirsttake.app.auth.service.RefreshTokenService;
import com.thefirsttake.app.common.user.entity.UserEntity;
import com.thefirsttake.app.common.user.repository.UserEntityRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Qualifier;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "인증 관리", description = "카카오 OAuth 로그인 및 사용자 인증 관련 API")
public class AuthController {
    
    private final KakaoAuthService kakaoAuthService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserEntityRepository userEntityRepository;
    private final Counter kakaoLoginSuccessCounter;
    private final Counter kakaoLoginFailureCounter;
    private final Counter logoutCounter;
    private final Timer jwtTokenGenerationTimer;
    
    public AuthController(KakaoAuthService kakaoAuthService, 
                         JwtService jwtService, 
                         RefreshTokenService refreshTokenService, 
                         UserEntityRepository userEntityRepository,
                         @Qualifier("kakaoLoginSuccessCounter") Counter kakaoLoginSuccessCounter,
                         @Qualifier("kakaoLoginFailureCounter") Counter kakaoLoginFailureCounter,
                         @Qualifier("logoutCounter") Counter logoutCounter,
                         @Qualifier("jwtTokenGenerationTimer") Timer jwtTokenGenerationTimer) {
        this.kakaoAuthService = kakaoAuthService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userEntityRepository = userEntityRepository;
        this.kakaoLoginSuccessCounter = kakaoLoginSuccessCounter;
        this.kakaoLoginFailureCounter = kakaoLoginFailureCounter;
        this.logoutCounter = logoutCounter;
        this.jwtTokenGenerationTimer = jwtTokenGenerationTimer;
    }
    
    
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
            - JWT 액세스 토큰(15분) + 리프레시 토큰(7일)을 HttpOnly 쿠키에 저장
            - XSS 공격 방지 및 짧은 액세스 토큰 수명으로 보안 강화
            - Secure 플래그로 HTTPS에서만 전송
            - 자동 토큰 갱신으로 사용자 경험 향상
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
                    value = "Redirect to https://the-first-take.com/"
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
                    value = "Redirect to https://the-firsrt-take.com/auth/error?message=에러메시지"
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
            String kakaoAccessToken = kakaoAuthService.getAccessToken(code);
            log.info("카카오 액세스 토큰 획득 성공");
            
            // 2. Access Token으로 사용자 정보 조회
            KakaoUserInfo kakaoUserInfo = kakaoAuthService.getUserInfo(kakaoAccessToken);
            log.info("카카오 사용자 정보 조회 성공. userId: {}", kakaoUserInfo.getId());
            
            // 카카오 사용자 정보 페이로드 로그 출력
            log.info("=== 카카오 사용자 정보 페이로드 ===");
            log.info("카카오 사용자 ID: {}", kakaoUserInfo.getId());
            log.info("카카오 닉네임: {}", kakaoUserInfo.getNickname());
            log.info("카카오 이메일: {}", kakaoUserInfo.getEmail());
            log.info("카카오 프로필 이미지: {}", kakaoUserInfo.getProfileImage());
            log.info("=======================================");
            
            // 3. DB에서 사용자 조회 또는 생성
            UserEntity userEntity = userEntityRepository.findByKakaoUserId(kakaoUserInfo.getId())
                .orElseGet(() -> {
                    // 카카오 사용자가 DB에 없으면 새로 생성
                    UserEntity newUser = new UserEntity();
                    newUser.setKakaoUserId(kakaoUserInfo.getId());
                    newUser.setIsGuest(false); // 카카오 로그인 사용자는 게스트가 아님
                    return userEntityRepository.save(newUser);
                });
            
            log.info("DB 사용자 조회/생성 완료. DB 사용자 ID: {}", userEntity.getId());
            
            // 4. JWT 토큰 생성 (우리 DB의 사용자 ID만 사용)
            String jwtAccessToken = jwtTokenGenerationTimer.recordCallable(() -> 
                jwtService.generateAccessToken(String.valueOf(userEntity.getId())));
            String jwtRefreshToken = jwtService.generateRefreshToken(String.valueOf(userEntity.getId()));
            
            // JWT 토큰 페이로드 로그 출력
            log.info("=== JWT 토큰 생성 정보 ===");
            log.info("JWT 액세스 토큰 길이: {} characters", jwtAccessToken.length());
            log.info("JWT 액세스 토큰 미리보기: {}...", jwtAccessToken.substring(0, Math.min(50, jwtAccessToken.length())));
            log.info("JWT 리프레시 토큰 길이: {} characters", jwtRefreshToken.length());
            log.info("JWT 리프레시 토큰 미리보기: {}...", jwtRefreshToken.substring(0, Math.min(50, jwtRefreshToken.length())));
            log.info("=============================");
            
            // 4. HttpOnly 쿠키로 토큰 설정
            // 액세스 토큰 쿠키 (8시간)
            Cookie accessTokenCookie = new Cookie("access_token", jwtAccessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(60 * 60 * 8); // 1시간
            
            response.addCookie(accessTokenCookie);
            // 요구사항: 일단 refresh 토큰은 쿠키에 저장하지 않음
            // 리프레시 토큰은 Redis에 저장 (TTL 7일)
            refreshTokenService.saveRefreshToken(String.valueOf(userEntity.getId()), jwtRefreshToken, Duration.ofDays(7));
            
            log.info("JWT 토큰 설정 완료. 액세스 토큰 길이: {}, 리프레시 토큰 생성됨(쿠키 미저장)", 
                jwtAccessToken.length());
            
            // 5. 성공 메트릭 증가
            kakaoLoginSuccessCounter.increment();
            
            // 6. 프론트엔드로 리다이렉트
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("https://the-second-take.com/"));
            // headers.setLocation(URI.create("https://the-first-take.com/"));
            // headers.setLocation(URI.create("http://localhost:3000/"));
            
            log.info("카카오 로그인 성공. 프론트엔드로 리다이렉트");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
            
        } catch (Exception e) {
            log.error("카카오 로그인 실패: {}", e.getMessage());
            
            // 실패 메트릭 증가
            kakaoLoginFailureCounter.increment();
            
            // 실패 시 에러 페이지로 리다이렉트
            String errorUrl = "https://the-second-take.com/auth/error?message=" + 
                URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            // String errorUrl = "https://the-first-take.com/auth/error?message=" + 
            //     URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            

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
            1. HttpOnly 쿠키에서 액세스 토큰과 리프레시 토큰을 삭제
            2. 쿠키 만료 시간을 0으로 설정하여 즉시 삭제
            3. 로그아웃 성공 응답 반환
            
            **⚠️ 중요: 완전한 로그아웃을 위해서는 프론트엔드에서 추가 처리 필요**
            
            **프론트엔드에서 사용하는 방법:**
            ```javascript
            async function logout() {
                // 1. 서버 측 로그아웃
                const response = await fetch('/api/auth/logout', {
                    method: 'POST',
                    credentials: 'include'
                });
                
                // 2. 카카오 로그아웃 (선택사항)
                if (window.Kakao && window.Kakao.Auth) {
                    window.Kakao.Auth.logout();
                }
                
                // 3. 페이지 이동
                window.location.href = '/login';
            }
            ```
            
            **카카오 SDK 로그아웃을 위한 HTML 추가:**
            ```html
            <script src="https://developers.kakao.com/sdk/js/kakao.js"></script>
            <script>
                Kakao.init('YOUR_KAKAO_CLIENT_ID');
            </script>
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
    public ResponseEntity<CommonResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 쿠키의 액세스 토큰에서 사용자 ID 추출 (만료 허용)
            String accessToken = extractJwtFromCookies(request.getCookies());
            if (accessToken != null) {
                String userId = null;
                try {
                    userId = jwtService.getUserIdFromToken(accessToken);
                } catch (Exception e) {
                    userId = jwtService.getUserIdFromExpiredToken(accessToken);
                }
                if (userId != null) {
                    // Redis의 리프레시 토큰 삭제
                    refreshTokenService.deleteRefreshToken(userId);
                }
            }
            // 액세스 토큰 쿠키 삭제
            Cookie accessTokenCookie = new Cookie("access_token", null);
            accessTokenCookie.setMaxAge(0);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(true);
            response.addCookie(accessTokenCookie);
            // 로그아웃 메트릭 증가
            logoutCounter.increment();
            
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
            - HttpOnly 쿠키에 저장된 JWT 액세스 토큰을 사용하여 인증
            - 액세스 토큰이 없거나 유효하지 않으면 401 Unauthorized 반환
            - 토큰 만료 시 리프레시 토큰으로 자동 갱신 가능
            
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
    
    @Operation(
        summary = "토큰 갱신",
        description = """
            리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.
            
            **동작 방식:**
            1. 리프레시 토큰 쿠키에서 토큰 추출
            2. 리프레시 토큰 유효성 검증
            3. 새로운 액세스 토큰과 리프레시 토큰 발급
            4. 쿠키에 새로운 토큰들 저장
            
            **프론트엔드에서 사용하는 방법:**
            ```javascript
            async function refreshToken() {
                const response = await fetch('/api/auth/refresh', {
                    method: 'POST',
                    credentials: 'include'
                });
                const result = await response.json();
                if (result.status === 'success') {
                    console.log('토큰 갱신 성공');
                }
            }
            ```
            """,
        tags = {"인증 관리"}
    )
    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1) 액세스 토큰(만료 가능)에서 사용자 ID 추출
            String accessToken = extractJwtFromCookies(request.getCookies());
            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("액세스 토큰이 없습니다"));
            }
            String userId;
            try {
                userId = jwtService.getUserIdFromToken(accessToken);
            } catch (Exception e) {
                userId = jwtService.getUserIdFromExpiredToken(accessToken);
            }
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("사용자 식별 실패"));
            }

            // 2) Redis에서 리프레시 토큰 조회 및 검증
            String refreshToken = refreshTokenService.getRefreshToken(userId);
            if (refreshToken == null || !jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("리프레시 토큰이 유효하지 않습니다"));
            }
            String refreshUserId = jwtService.getUserIdFromToken(refreshToken);
            if (!userId.equals(refreshUserId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.fail("토큰 소유자 불일치"));
            }

            // 3) 새로운 액세스 토큰 발급 (토큰 회전은 추후)
            String newAccessToken = jwtService.generateAccessToken(userId);

            // 4) 액세스 토큰 쿠키 설정
            Cookie accessTokenCookie = new Cookie("access_token", newAccessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(60 * 60); // 1시간
            
            response.addCookie(accessTokenCookie);
            // refresh 토큰은 Redis 보관, 쿠키 미저장
            
            log.info("토큰 갱신 성공. 사용자 ID: {}", userId);
            return ResponseEntity.ok(CommonResponse.success("토큰 갱신 성공"));
            
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.fail("토큰 갱신 실패"));
        }
    }
    
    private String extractJwtFromCookies(Cookie[] cookies) {
        return extractTokenFromCookies(cookies, "access_token");
    }
    
    private String extractTokenFromCookies(Cookie[] cookies, String tokenName) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (tokenName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
