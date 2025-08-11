package com.thefirsttake.app.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 에이전트 응답 DTO
 * - 에이전트 정보와 응답 메시지를 함께 전달
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAgentResponse {
    
    /**
     * 에이전트 ID (예: style_analyst, color_expert 등)
     */
    @JsonProperty("agent_id")
    private String agentId;
    
    /**
     * 에이전트 이름 (한글)
     */
    @JsonProperty("agent_name")
    private String agentName;
    
    /**
     * 에이전트 역할 설명
     */
    @JsonProperty("agent_role")
    private String agentRole;
    
    /**
     * 에이전트 응답 메시지
     */
    private String message;
    
    /**
     * 에이전트 순서 (1, 2, 3, 4)
     */
    private Integer order;
    
    /**
     * 상품 이미지 URL 리스트
     */
    @JsonProperty("product_image_url")
    private java.util.List<String> productImageUrl;
} 