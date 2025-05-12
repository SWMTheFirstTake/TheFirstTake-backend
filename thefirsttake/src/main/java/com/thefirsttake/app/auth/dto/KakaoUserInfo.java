package com.thefirsttake.app.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class KakaoUserInfo {
    private Long id;
    private String email;
}
