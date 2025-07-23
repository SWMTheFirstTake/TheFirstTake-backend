//package com.thefirsttake.app.chat.service;
//
//import com.thefirsttake.app.chat.constant.ChatAgentConstants;
//import com.thefirsttake.app.common.response.ApiResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//
///**
// * AI 응답 생성 전담 서비스
// * - 전문가 체인 API 호출
// * - 단일 AI 응답 생성
// * - 외부 LLM 서버와의 통신
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ChatAIService {
//    private final RestTemplate restTemplate;
//
//    @Value("${llm.server.host}")
//    private String llmServerHost;
//
//    @Value("${llm.server.port}")
//    private String llmServerPort;
//
//    public Map<String, Object> getExpertSingleResult(String userInput, Long roomId, String agent) {
//        try {
//            Map<String, Object> requestMap = buildExpertChainRequest(userInput, roomId);
////            String fastApiUrl = "http://" + llmServerHost + ":" + llmServerPort + "/api/expert/single";
////            String fastApiUrl = "http://" + "localhost" + ":" + "6020" + "/api/expert/chain";
//            String fastApiUrl = "http://" + "localhost" + ":" + "6020" + "/api/expert/single";
//
//            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);
//            ApiResponse body = response.getBody();
//
//            if (body == null || body.getData() == null) {
//                throw new RuntimeException("FastAPI 서버로부터 유효한 응답을 받지 못했습니다.");
//            }
//
//            Map<String, Object> responseData = (Map<String, Object>) body.getData();
//            Map<String, Object> expertAnalyses = (Map<String, Object>) responseData.get("expert_analyses");
//
//            if (expertAnalyses == null || expertAnalyses.isEmpty()) {
//                throw new RuntimeException("전문가 분석 결과가 비어있습니다.");
//            }
//
//            return expertAnalyses;
//
//        } catch (Exception e) {
//            log.error("전문가 체인 API 호출 실패: {}", e.getMessage(), e);
//            throw new RuntimeException("전문가 체인 분석 실패", e);
//        }
//    }
//
//    /**
//     * 전문가 체인 API 호출하여 다중 전문가 분석 결과 반환
//     */
//    public List<Map<String, Object>> getExpertChainResult(String userInput, Long roomId) {
//        try {
//            Map<String, Object> requestMap = buildExpertChainRequest(userInput, roomId);
////            String fastApiUrl = "http://" + llmServerHost + ":" + llmServerPort + "/api/expert/single";
////            String fastApiUrl = "http://" + "localhost" + ":" + "6020" + "/api/expert/chain";
//            String fastApiUrl = "http://" + "localhost" + ":" + "6020" + "/api/expert/single";
//
//            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);
//            ApiResponse body = response.getBody();
//
//            if (body == null || body.getData() == null) {
//                throw new RuntimeException("FastAPI 서버로부터 유효한 응답을 받지 못했습니다.");
//            }
//
//            Map<String, Object> responseData = (Map<String, Object>) body.getData();
//            List<Map<String, Object>> expertAnalyses = (List<Map<String, Object>>) responseData.get("expert_analyses");
//
//            if (expertAnalyses == null || expertAnalyses.isEmpty()) {
//                throw new RuntimeException("전문가 분석 결과가 비어있습니다.");
//            }
//
//            return expertAnalyses;
//
//        } catch (Exception e) {
//            log.error("전문가 체인 API 호출 실패: {}", e.getMessage(), e);
//            throw new RuntimeException("전문가 체인 분석 실패", e);
//        }
//    }
//
//    private Map<String, Object> buildExpertSingleRequest(String userInput, Long roomId, String agent) {
//        Map<String, Object> requestMap = new HashMap<>();
//        requestMap.put("user_input", userInput);
//        requestMap.put("room_id", roomId);
//        requestMap.put("expert_type",agent);
//
//        // 상수에서 에이전트 순서 가져오기
////        requestMap.put("expert_sequence", ChatAgentConstants.AGENT_SEQUENCE);
//
//        Map<String, Object> userProfile = new HashMap<>();
//        requestMap.put("user_profile", userProfile);
//
////        Map<String, Object> contextInfo = new HashMap<>();
////        contextInfo.put("previous_conversation", promptValue);
////        requestMap.put("context_info", contextInfo);
//
//        return requestMap;
//    }
//
//    /**
//     * 전문가 체인 요청 데이터 구성
//     */
//    private Map<String, Object> buildExpertChainRequest(String userInput, Long roomId) {
//        Map<String, Object> requestMap = new HashMap<>();
//        requestMap.put("user_input", userInput);
//        requestMap.put("room_id", roomId);
//
//        // 상수에서 에이전트 순서 가져오기
//        requestMap.put("expert_sequence", ChatAgentConstants.AGENT_SEQUENCE);
//
//        Map<String, Object> userProfile = new HashMap<>();
//        requestMap.put("user_profile", userProfile);
//
////        Map<String, Object> contextInfo = new HashMap<>();
////        contextInfo.put("previous_conversation", promptValue);
////        requestMap.put("context_info", contextInfo);
//
//        return requestMap;
//    }
//}
package com.thefirsttake.app.chat.service;

import com.thefirsttake.app.chat.constant.ChatAgentConstants;
import com.thefirsttake.app.chat.enums.ChatAgentType;
import com.thefirsttake.app.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 응답 생성 전담 서비스
 * - 전문가 체인 API 호출
 * - 단일 AI 응답 생성
 * - 외부 LLM 서버와의 통신
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatAIService {
    private final RestTemplate restTemplate;

    @Value("${llm.server.host}")
    private String llmServerHost;

    @Value("${llm.server.port}")
    private String llmServerPort;

    /**
     * 단일 전문가 분석 결과 반환
     */
    public Map<String, Object> getExpertSingleResult(String userInput, Long roomId, ChatAgentType agent) {
        try {
            Map<String, Object> requestMap = buildExpertSingleRequest(userInput, roomId, agent);
            // String fastApiUrl = "http://"+"localhost"+":"+llmServerPort+"/api/expert/single";
            String fastApiUrl = "http://"+llmServerHost+":"+llmServerPort+"/api/expert/single";

            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);
            ApiResponse body = response.getBody();

            if (body == null || body.getData() == null) {
                throw new RuntimeException("FastAPI 서버로부터 유효한 응답을 받지 못했습니다.");
            }

            Map<String, Object> responseData = (Map<String, Object>) body.getData();

            if (responseData == null || responseData.isEmpty()) {
                throw new RuntimeException("전문가 분석 결과가 비어있습니다.");
            }

            return responseData;

        } catch (Exception e) {
            log.error("단일 전문가 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("단일 전문가 분석 실패", e);
        }
    }

    /**
     * 전문가 체인 API 호출하여 다중 전문가 분석 결과 반환 (기존 메소드 유지)
     */
    public List<Map<String, Object>> getExpertChainResult(String userInput, Long roomId) {
        try {
            Map<String, Object> requestMap = buildExpertChainRequest(userInput, roomId);
            String fastApiUrl = "http://localhost:6020/api/expert/chain";

            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);
            ApiResponse body = response.getBody();

            if (body == null || body.getData() == null) {
                throw new RuntimeException("FastAPI 서버로부터 유효한 응답을 받지 못했습니다.");
            }

            Map<String, Object> responseData = (Map<String, Object>) body.getData();
            List<Map<String, Object>> expertAnalyses = (List<Map<String, Object>>) responseData.get("expert_analyses");

            if (expertAnalyses == null || expertAnalyses.isEmpty()) {
                throw new RuntimeException("전문가 분석 결과가 비어있습니다.");
            }

            return expertAnalyses;

        } catch (Exception e) {
            log.error("전문가 체인 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("전문가 체인 분석 실패", e);
        }
    }

    /**
     * 단일 전문가 요청 데이터 구성
     */
    private Map<String, Object> buildExpertSingleRequest(String userInput, Long roomId, ChatAgentType agent) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("user_input", userInput);
        requestMap.put("room_id", roomId);
        requestMap.put("expert_type", agent.getCode()); // enum의 code 값 사용

        Map<String, Object> userProfile = new HashMap<>();
        requestMap.put("user_profile", userProfile);

        return requestMap;
    }

    /**
     * 전문가 체인 요청 데이터 구성 (기존 메소드 유지)
     */
    private Map<String, Object> buildExpertChainRequest(String userInput, Long roomId) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("user_input", userInput);
        requestMap.put("room_id", roomId);

        // 상수에서 에이전트 순서 가져오기
        requestMap.put("expert_sequence", ChatAgentConstants.AGENT_SEQUENCE);

        Map<String, Object> userProfile = new HashMap<>();
        requestMap.put("user_profile", userProfile);

        return requestMap;
    }
}