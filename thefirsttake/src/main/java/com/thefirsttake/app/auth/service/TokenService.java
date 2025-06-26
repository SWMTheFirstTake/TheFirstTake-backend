//package com.thefirsttake.app.auth.service;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jws;
//import io.jsonwebtoken.JwtException;
//import io.jsonwebtoken.Jwts;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Service;
//
//import java.nio.charset.StandardCharsets;
//
//@Service
//public class TokenService {
//    private final byte[] SECRET_KEY = "X9p$eTqN7#vF3@LmPz1!tUcRg*YkWqZ0oAsJdLxCvBnMhQeRfTgYhUjIkOlPnMbVcXsZaSdFgHiJkL11111".getBytes(StandardCharsets.UTF_8);
//    // 임시로 해놓음
//    public boolean validateUser(String username, String password) {
//        // 하드코딩된 사용자 정보
//        String validUsername = "user";
//        String validPassword = "1234";
//
//        // 사용자 입력값과 비교
//        return username.equals(validUsername) && password.equals(validPassword);
//    }
//    public UserDetails validateAndGetUser(String token) {
//        try {
//            // JWT 토큰을 파싱하고, 토큰에서 사용자 정보를 추출
//            Jws<Claims> claims = Jwts.parser()
//                    .setSigningKey(SECRET_KEY)
//                    .parseClaimsJws(token);
//            String username=claims.getBody().getSubject();
//            // 유저 이름과 비밀번호를 검증
//            if (!validateUser(username, "1234")) {  // 비밀번호가 1234인지를 체크
//                throw new IllegalArgumentException("Invalid username or password");
//            }
//
//            // 사용자 정보를 반환하거나 필요한 작업 수행
//            return User.builder()
//                    .username(username)
//                    .password("1234")  // 비밀번호를 {noop}로 설정하여 암호화하지 않음
//                    .roles("USER")
//                    .build();
//            // 사용자 정보를 DB나 캐시에서 조회
////            return userRepository.findByUsername(username);
//        } catch (JwtException e) {
//            // 토큰이 유효하지 않거나 잘못된 경우
//            return null;
//        }
//    }
//}
//
