package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.constant.ChatAgentConstants;
import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
import com.thefirsttake.app.chat.enums.ChatAgentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 채팅 큐레이션 조율 전담 서비스
 * - AI 응답 생성 조율
 * - 전문가 분석 결과 처리
 * - 프롬프트 관리와 AI 서비스 연결
 */
@Service
@Slf4j
public class ChatCurationOrchestrationService {
    private final ChatAIService chatAIService;
    private final ChatPromptService chatPromptService;
    
    public ChatCurationOrchestrationService(ChatAIService chatAIService,
                                          ChatPromptService chatPromptService) {
        this.chatAIService = chatAIService;
        this.chatPromptService = chatPromptService;
    }

    /**
     * 큐레이션 응답 생성 (메인 메서드) - 단일 에이전트 처리
     */
    public List<ChatAgentResponse> generateCurationResponse(String userInput, Long roomId, ChatAgentType agent) {
        try {
            // 단일 전문가 분석 결과 생성
            ChatAgentResponse singleResult = generateSingleAgentResponse(userInput, roomId, agent);

            // DB 저장을 위해 List로 감싸서 반환 (기존 구조 호환성)
            List<ChatAgentResponse> resultList = new ArrayList<>();
            resultList.add(singleResult);

            return resultList;

        } catch (Exception e) {
            log.error("큐레이션 분석 중 오류 발생: {}", e.getMessage(), e);
            return createErrorResponse(agent);
        }
    }

    /**
     * 단일 에이전트 응답 생성
     */
    public ChatAgentResponse generateSingleAgentResponse(String userInput, Long roomId, ChatAgentType agent) {
        try {
            // 단일 전문가 API 호출
            Map<String, Object> expertAnalysis = chatAIService.getExpertSingleResult(
                    userInput, roomId, agent
            );

            // 전문가 분석 결과 처리
            return processExpertAnalysis(expertAnalysis, agent);

        } catch (Exception e) {
            log.error("단일 에이전트 분석 중 오류 발생: {}", e.getMessage(), e);
            return createSingleErrorResponse(agent);
        }
    }

    /**
     * 단일 전문가 분석 결과 처리
     */
    private ChatAgentResponse processExpertAnalysis(Map<String, Object> expertAnalysis, ChatAgentType agent) {
        String analysis = (String) expertAnalysis.get("analysis");
        if (analysis == null) {
            analysis = "분석 결과를 가져올 수 없습니다.";
            log.warn("분석 결과가 null입니다. agent={}", agent.getCode());
        }

        // 에이전트 정보 가져오기
        ChatAgentConstants.AgentInfo agentInfo = ChatAgentConstants.AGENT_INFO_MAP.get(agent.getCode());
        if (agentInfo == null) {
            log.warn("알 수 없는 에이전트 타입: {}", agent.getCode());
            // 기본값 설정
            return ChatAgentResponse.builder()
                    .agentId(agent.getCode())
                    .agentName("알 수 없는 전문가")
                    .agentRole("전문가")
                    .message(analysis)
                    .order(1)
                    .build();
        }

        return ChatAgentResponse.builder()
                .agentId(agentInfo.getAgentId())
                .agentName(agentInfo.getAgentName())
                .agentRole(agentInfo.getAgentRole())
                .message(analysis)
                .order(1)
                .build();
    }

    /**
     * 전문가 분석 결과 처리 (다중 전문가용 - 기존 메소드 유지)
     */
    private List<ChatAgentResponse> processExpertAnalyses(List<Map<String, Object>> expertAnalyses) {
        List<ChatAgentResponse> curationResults = new ArrayList<>();

        for (int i = 0; i < expertAnalyses.size(); i++) {
            Map<String, Object> expertResult = expertAnalyses.get(i);
            String expertType = (String) expertResult.get("expert_type");
            String analysis = (String) expertResult.get("analysis");

            // 에이전트 정보 가져오기
            ChatAgentConstants.AgentInfo agentInfo = ChatAgentConstants.AGENT_INFO_MAP.get(expertType);
            if (agentInfo == null) {
                log.warn("알 수 없는 에이전트 타입: {}", expertType);
                continue;
            }

            ChatAgentResponse response = ChatAgentResponse.builder()
                    .agentId(agentInfo.getAgentId())
                    .agentName(agentInfo.getAgentName())
                    .agentRole(agentInfo.getAgentRole())
                    .message(analysis)
                    .order(i + 1)
                    .build();

            curationResults.add(response);
        }

        return curationResults;
    }

    /**
     * 프롬프트를 큐레이션 결과로 업데이트
     */
    private void updatePromptWithResults(String promptKey, String currentPromptValue, List<ChatAgentResponse> curationResults) {
        StringBuilder promptUpdateBuilder = new StringBuilder(currentPromptValue);
        for (ChatAgentResponse result : curationResults) {
            String formattedResult = result.getAgentRole() + "\n" + result.getMessage();
            promptUpdateBuilder.append(formattedResult);
        }
        chatPromptService.savePrompt(promptKey, promptUpdateBuilder.toString());
    }

    /**
     * 오류 발생 시 기본 응답 생성 (단일 에이전트용)
     */
    private ChatAgentResponse createSingleErrorResponse(ChatAgentType agent) {
        String errorMessage = "큐레이션 결과를 가져오는 중 오류가 발생했습니다.";

        // 해당 에이전트에 대한 오류 응답 생성
        ChatAgentConstants.AgentInfo agentInfo = ChatAgentConstants.AGENT_INFO_MAP.get(agent.getCode());

        if (agentInfo != null) {
            return ChatAgentResponse.builder()
                    .agentId(agentInfo.getAgentId())
                    .agentName(agentInfo.getAgentName())
                    .agentRole(agentInfo.getAgentRole())
                    .message(errorMessage)
                    .order(1)
                    .build();
        } else {
            // agentInfo가 없을 경우 기본 응답
            return ChatAgentResponse.builder()
                    .agentId(agent.getCode())
                    .agentName("전문가")
                    .agentRole("패션 전문가")
                    .message(errorMessage)
                    .order(1)
                    .build();
        }
    }

    /**
     * 오류 발생 시 기본 응답 생성 (List 형태, DB 저장 호환성용)
     */
    private List<ChatAgentResponse> createErrorResponse(ChatAgentType agent) {
        List<ChatAgentResponse> errorResults = new ArrayList<>();
        errorResults.add(createSingleErrorResponse(agent));
        return errorResults;
    }

    /**
     * 오류 발생 시 기본 응답 생성 (다중 에이전트용 - 기존 메소드 유지)
     */
    private List<ChatAgentResponse> createErrorResponses() {
        List<ChatAgentResponse> errorResults = new ArrayList<>();
        String errorMessage = "큐레이션 결과를 가져오는 중 오류가 발생했습니다.";

        // 각 에이전트별로 오류 응답 생성
        for (int i = 0; i < ChatAgentConstants.AGENT_SEQUENCE.size(); i++) {
            String agentId = ChatAgentConstants.AGENT_SEQUENCE.get(i);
            ChatAgentConstants.AgentInfo agentInfo = ChatAgentConstants.AGENT_INFO_MAP.get(agentId);

            ChatAgentResponse errorResponse = ChatAgentResponse.builder()
                    .agentId(agentInfo.getAgentId())
                    .agentName(agentInfo.getAgentName())
                    .agentRole(agentInfo.getAgentRole())
                    .message(errorMessage)
                    .order(i + 1)
                    .build();

            errorResults.add(errorResponse);
        }

        return errorResults;
    }
}