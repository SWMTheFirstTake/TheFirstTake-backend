package com.thefirsttake.app.chat.constant;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 채팅 에이전트 관련 상수
 */
public class ChatAgentConstants {
    
    /**
     * 에이전트 순서 정의
     */
    public static final List<String> AGENT_SEQUENCE = Arrays.asList(
            "style_analyst",
            "color_expert",
            "fitting_coordinator"
    );
    
    /**
     * 에이전트 정보 매핑
     */
    public static final Map<String, AgentInfo> AGENT_INFO_MAP = Map.of(
            "style_analyst", new AgentInfo("style_analyst", "스타일 분석가", "체형분석과 핏감을 중심으로 추천해드려요!"),
            "color_expert", new AgentInfo("color_expert", "컬러 전문가", "피부톤에 어울리는 색상 조합을 바탕으로 추천해드려요!"),
            "fitting_coordinator", new AgentInfo("fitting_coordinator", "핏팅 코디네이터", "종합적으로 딱 하나의 추천을 해드려요!")
    );
    
    /**
     * 에이전트 ID를 DB 저장용으로 변환하는 매핑
     */
    public static final Map<String, String> AGENT_ID_MAPPING = Map.of(
            "style_analyst", "STYLE",
            "color_expert", "COLOR",
            "fitting_coordinator", "FITTING"
    );
    
    /**
     * DB 저장용 에이전트 ID를 에이전트 이름으로 변환하는 매핑
     */
    public static final Map<String, String> DB_AGENT_NAME_MAPPING = Map.of(
            "STYLE", "스타일 분석가",
            "COLOR", "컬러 전문가", 
            "FITTING", "핏팅 코디네이터"
    );

    /**
     * 에이전트 정보를 담는 내부 클래스
     */
    public static class AgentInfo {
        private final String agentId;
        private final String agentName;
        private final String agentRole;

        public AgentInfo(String agentId, String agentName, String agentRole) {
            this.agentId = agentId;
            this.agentName = agentName;
            this.agentRole = agentRole;
        }

        public String getAgentId() { return agentId; }
        public String getAgentName() { return agentName; }
        public String getAgentRole() { return agentRole; }
    }
} 