package com.thefirsttake.app.security;

import com.thefirsttake.app.common.dto.UserDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {
    //    @Value("${jwt.secret}")
//    private String secretKey;
//    private final byte[] SECRET_KEY = secretKey.getBytes(StandardCharsets.UTF_8);
    private final byte[] SECRET_KEY = "X9p$eTqN7#vF3@LmPz1!tUcRg*YkWqZ0oAsJdLxCvBnMhQeRfTgYhUjIkOlPnMbVcXsZaSdFgHiJkL11111".getBytes(StandardCharsets.UTF_8);
    public String generateAccessToken(UserDto user) {
        return Jwts.builder()
                .setSubject(user.getId().toString()) // 또는 user.getId() 등
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 1일
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
    public String generateRefreshToken(UserDto user) {
        return Jwts.builder()
                .setSubject(user.getId().toString()) // 또는 user.getId() 등
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 30)) // 1일
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
}

