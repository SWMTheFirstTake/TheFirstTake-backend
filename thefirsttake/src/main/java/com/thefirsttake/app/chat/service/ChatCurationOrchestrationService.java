package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.constant.ChatAgentConstants;
import com.thefirsttake.app.chat.dto.response.ChatAgentResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ChatCurationOrchestrationService {
    private final ChatAIService chatAIService;
    private final ChatPromptService chatPromptService;

    /**
     * 큐레이션 응답 생성 (메인 메서드) - 에이전트 정보 포함
     */
    public List<ChatAgentResponse> generateCurationResponse(String userInput, Long roomId) {
        String promptKey = roomId + ":prompt";
        String currentPromptValue = chatPromptService.getOrCreatePrompt(promptKey, userInput);
        
        try {
            // 전문가 체인 API 호출
            List<Map<String, Object>> expertAnalyses = chatAIService.getExpertChainResult(
                    userInput, roomId, currentPromptValue
            );

            // 전문가 분석 결과 처리
            List<ChatAgentResponse> curationResults = processExpertAnalyses(expertAnalyses);
            
            // 프롬프트 업데이트
            updatePromptWithResults(promptKey, currentPromptValue, curationResults);

            return curationResults;

        } catch (Exception e) {
            log.error("큐레이션 분석 중 오류 발생: {}", e.getMessage(), e);
            return createErrorResponses();
        }
    }

    /**
     * 전문가 분석 결과 처리
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
     * 오류 발생 시 기본 응답 생성
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