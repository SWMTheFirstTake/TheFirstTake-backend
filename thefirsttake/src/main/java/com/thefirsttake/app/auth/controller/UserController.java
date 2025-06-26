//package com.thefirsttake.app.auth.controller;
//
//import com.thefirsttake.app.auth.service.TokenService;
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class UserController {
//    private final TokenService tokenService;
//
//    // Constructor-based injection for better testability
//    @Autowired
//    public UserController(TokenService tokenService) {
//        this.tokenService = tokenService;
//    }
//    @GetMapping("/user-info")
//    public ResponseEntity<?> getUserInfo(HttpServletRequest request) {
//        // 쿠키에서 access_token 찾기
//        String accessToken = getAccessTokenFromCookies(request);
//
//        if (accessToken == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
//        }
//
//        // 토큰 검증 (실제 검증 로직은 사용 중인 인증 방법에 따라 다름)
//        UserDetails user = validateAccessToken(accessToken);
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
//        }
//        // 사용자 정보 반환
//        return ResponseEntity.ok(user);
//    }
//
//    private String getAccessTokenFromCookies(HttpServletRequest request) {
//        // 쿠키에서 access_token을 찾는 로직
//        Cookie[] cookies = request.getCookies();
//        if (cookies != null) {
//            for (Cookie cookie : cookies) {
//                if ("access_token".equals(cookie.getName())) {
//                    return cookie.getValue();
//                }
//            }
//        }
//        return null;
//    }
//
//    private UserDetails validateAccessToken(String accessToken) {
//        // 여기에 실제 토큰 검증 로직을 구현
//        // 예: JWT 검증, DB에서 사용자 조회 등
//        return tokenService.validateAndGetUser(accessToken);
//    }
//}
