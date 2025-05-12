package com.thefirsttake.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Collections;

@WebFilter(urlPatterns = "/api/*")  // 특정 URL 패턴에 대해 필터를 적용
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter{
    private final byte[] SECRET_KEY = "X9p$eTqN7#vF3@LmPz1!tUcRg*YkWqZ0oAsJdLxCvBnMhQeRfTgYhUjIkOlPnMbVcXsZaSdFgHiJkL11111".getBytes(StandardCharsets.UTF_8);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwtToken = getJwtFromRequest(request);

        if (jwtToken != null && validateToken(jwtToken)) {
            // 토큰이 유효하면 인증 객체를 생성하여 SecurityContext에 설정
            Authentication authentication = getAuthentication(jwtToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // "Bearer " 이후의 토큰 부분을 반환
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            // JWT 토큰 유효성 검사 (예: 만료 시간, 서명 등)
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String username = claims.getSubject();  // 사용자 이름

        // 사용자 이름을 바탕으로 UserDetails를 로드하거나 권한을 부여
        // 여기서는 간단히 사용자의 이름만을 인증 정보로 사용
        return new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
    }
}
