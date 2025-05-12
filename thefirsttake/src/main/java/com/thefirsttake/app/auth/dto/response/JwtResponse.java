package com.thefirsttake.app.auth.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class JwtResponse {
    //    private String token;
    private String accessToken;
    private String refreshToken;

}

