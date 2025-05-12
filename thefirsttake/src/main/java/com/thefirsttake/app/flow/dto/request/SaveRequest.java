package com.thefirsttake.app.flow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Schema(description = "사용자가 입력한 질문과 선택지 정보를 담는 요청 객체")
public class SaveRequest {
    private String question;
    private String option;
}
