package com.thefirsttake.app.chat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatAgentType {
    STYLE_ANALYST("style_analyst", "스타일 분석 전문가"),
    COLOR_EXPERT("color_expert", "컬러 전문가"),
    FITTING_COORDINATOR("fitting_coordinator", "피팅 코디네이터");

    private final String code;
    private final String description;

    public static ChatAgentType fromCode(String code) {
        for (ChatAgentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown agent code: " + code);
    }
}
