package com.thefirsttake.app.flow.controller;

import com.thefirsttake.app.common.response.ApiResponse;
import com.thefirsttake.app.flow.dto.request.QARequest;
import com.thefirsttake.app.flow.dto.request.SaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flow")
public class QuestionFlowController {
    private final RedisTemplate<String, String> redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(QuestionFlowController.class);
    private final RestTemplate restTemplate;
    @Operation(
            summary = "시작과 세션 ID값 가져오기",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "시작 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답 예시",
                                            summary = "시작 성공 응답",
                                            value = """
                    {
                      "status": "success",
                      "message": "Start successful",
                      "data": "45F707FAFC34A564E016C71BB39D6A9F"
                    }
                    """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/session-start")
    public ApiResponse getSessionInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(); // 세션이 없으면 새로 생성
        String sessionId = session.getId();         // 세션 ID 가져오기
        return new ApiResponse("success","세션 정보 조회 성공",sessionId);
    }

    @Operation(
            summary = "사용자에게 기본 정보를 얻어오기 위해 질문과 응답 선택지 요청",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "요청 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "요청 응답 예시",
                                            summary = "기본 질문 리스트 응답 예시",
                                            value = """
                    {
                      "status": "success",
                      "message": "기본 질문 리스트 정보 조회 성공",
                      "data": [
                        {
                          "question": "Q1. 나이대는 어떻게 되나요?",
                          "options": ["10대", "20대", "30대", "40대", "50대"]
                        },
                        {
                          "question": "Q2. 키는 어떻게 되나요?",
                          "options": ["165이하", "165~170", "170~175", "175~180", "180이상"]
                        }
                      ]
                    }
                    """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/user-info")
    public ApiResponse getUserInfo(HttpServletRequest request) {
        String currentSession=getSessionId(request);
        List<QARequest> questions = List.of(
                new QARequest("Q1. 나이대는 어떻게 되나요?", List.of("10대", "20대", "30대", "40대", "50대")),
                new QARequest("Q2. 키는 어떻게 되나요?", List.of("165이하", "165~170", "170~175", "175~180", "180이상")),
                new QARequest("Q3. 몸무게는 어떻게 되나요?", List.of("55~60", "60~65", "65~70", "70~75", "75~80", "80~85", "85~90", "90~95", "95~100"))
        );
        return new ApiResponse("success","세션 정보 조회 성공",questions);
    }


    @Operation(
            summary = "사용자의 질문/선택지 저장 및 프롬프트 구성",
            description = """
        사용자가 전달한 질문(question)과 선택지(option) 리스트를 현재 세션 기준 Redis에 저장하여 
        프롬프트를 누적 구성합니다.

        - Redis 키는 [세션ID:prompt] 형식으로 저장됩니다.
        - 최초 요청 시 기본 시나리오 프롬프트가 초기화되어 저장되며,
        - 이후 요청마다 전달된 질문/선택지를 이어 붙여 누적 저장됩니다.
        - 최종 프롬프트 문자열이 ApiResponse.data 필드로 반환됩니다.
    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "SaveRequest 객체 리스트 (질문 + 선택지)",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = SaveRequest.class))
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "프롬프트 누적 저장 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                    name = "요청 응답 예시",
                                    summary = "기본 질문 리스트 응답 예시",
                                    value = """
                            {
                              "status": "success",
                              "message": "사용자의 질문 프롬프트에 저장 완료",
                              "data": [
                                {
                                  "question": "Q1. 나이대는 어떻게 되나요?",
                                  "options": ["20대"]
                                },
                                {
                                  "question": "Q2. 키는 어떻게 되나요?",
                                  "options": ["170~175"]
                                }
                              ]
                            }
                            """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/save-info")
    public ApiResponse setSaveInfo(@RequestBody List<SaveRequest> responses, HttpServletRequest request) {
        String currentSession=getSessionId(request);
        String currentKey = currentSession + ":prompt";
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(currentKey))) {
            String promptValue = "시나리오: 당신은 패션 큐레이터입니다. 사용자가 옷을 잘 모르기 때문에, 옷을 고를 때 최소한의 선택만 하도록 돕는 것이 목적입니다. 그래서 사용자가 원하는 취향을 유도하기 위해 질문과 선택지를 만들어야 합니다. 이전 질의응답과 비슷하지 않은 다음 질문을 자연스럽고 간단한 말투로 만들어주세요. 사용자는 패션을 잘 모르기 때문에, 전문 용어를 피하고 쉬운 말로 설명해주세요. 객관식 3개의 선택지도 함께 제공해주세요. 답변의 형식을 엄격히 지키고 줄로 구분해 주세요. 형식: Q. [질문 내용] A. [선택지1] B. [선택지2] C. [선택지3]의 형식으로 한 개의 질의응답만 제공해 주세요. 다음은 지금까지의 응답입니다.";
            redisTemplate.opsForValue().set(currentKey, promptValue);
        }
        StringBuilder promptBuilder = new StringBuilder(redisTemplate.opsForValue().get(currentSession+ ":prompt"));
        for (SaveRequest sr : responses) {
            promptBuilder.append(sr.getQuestion());
            promptBuilder.append(sr.getOption());
        }
        promptBuilder.append(" ");
        String currentPrompt = promptBuilder.toString();
        redisTemplate.opsForValue().set(currentSession + ":prompt",currentPrompt);

        logger.info(redisTemplate.opsForValue().get(currentSession + ":prompt")+"hehe");
        return new ApiResponse("success","세션 정보 조회 성공",promptBuilder);
    }

    @Operation(
            summary = "사용자에게 클라이언트 정보(상황적 요소)를 얻어오기 위해 질문과 응답 선택지 요청",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "클라이언트 정보(상황적 요소) 가져오기 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "요청 응답 예시",
                                            summary = "상황적 요소 질문 리스트 응답 예시",
                                            value = """
                    {
                      "status": "success",
                      "message": "상황적 요소 질문 리스트 응답 조회 성공",
                      "data": [
                        {
                          "question": "Q4. 사려는 옷은 어떤건가요?",
                          "options": ["상의", "하의"]
                        },
                        {
                          "question": "Q5. 옷을 사려는 목적은 어떻게 되나요?",
                          "options": ["출퇴근", "단순외출", "데이트", "운동", "친구모임"]
                        }
                      ]
                    }
                    """
                                    )
                            )
                    )
            }
    )
    @GetMapping("/client-info")
    public ApiResponse getClientInfo(HttpServletRequest request) {
        String curSession=getSessionId(request);
        List<QARequest> questions = List.of(
                new QARequest("Q4. 사려는 옷은 어떤건가요?", List.of("상의", "하의")),
                new QARequest("Q5. 옷을 사려는 목적은 어떻게 되나요?", List.of("출퇴근", "단순외출", "데이트", "운동", "친구모임")),
                new QARequest("Q6. 언제 입을 옷인가요?", List.of("봄/가을", "여름", "겨울")),
                new QARequest("Q7. 원하는 스타일은 어떤 느낌인가요?", List.of("깔끔한", "편안한", "트렌디한", "화려한")),
                new QARequest("Q8. 선호하는 색상 계열이 있나요?", List.of("밝은 색", "어두운 색", "무채색")),
                new QARequest("Q9. 가격대는 어느 정도를 생각하고 계신가요?", List.of("저렴하게", "중간 가격대", "상관없어요")),
                new QARequest("Q10. 어떤 핏을 선호하시나요?", List.of("슬림핏", "레귤러핏", "오버핏"))
        );
        return new ApiResponse("success","세션 정보 조회 성공",questions);
    }

    @Operation(
            summary = "사용자에게 제공할 다음 질문에 대한 응답을 받습니다.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "새 질문 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QARequest.class),
                            examples = @ExampleObject(
                                    name = "응답 예시",
                                    summary = "새로 생성된 질문 응답",
                                    value = """
                  {
                    "status": "success",
                    "message": "상황적 요소 질문 리스트 응답 조회 성공",
                    "data":{
                        "question": "Q. 어떤 옷 스타일이 마음에 드세요?",
                        "options": ["심플하고 깔끔", "약간은 특별한 포인트가 있는 스타일", "편하고 자유로운 캐주얼 스타일"]
                    }
                    
                  }                 
                """
                            )
                    )
            )
    )
    @GetMapping("/additional-info")
    public ApiResponse getNextInfo(HttpServletRequest request) {
        logger.info("he");
        String currentSession=getSessionId(request);
        logger.info(currentSession);
        StringBuilder promptBuilder = new StringBuilder(redisTemplate.opsForValue().get(currentSession + ":prompt"));
        String fastApiUrl = "http://localhost:5160/api/ask";
        String fullPrompt = promptBuilder.toString();
        logger.info(fullPrompt);
        // 요청 객체 생성
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("prompt", fullPrompt);
        ResponseEntity<ApiResponse> responseEntity = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);
        ApiResponse fastApiResponse = responseEntity.getBody();
        // ✅ 응답 data 꺼내기
        Map<String, Object> rawData = (Map<String, Object>) fastApiResponse.getData();

        // ✅ question과 options 수동으로 추출
        String question = String.valueOf(rawData.get("question"));
        Object options = rawData.get("options"); // options는 List일 것

        Map<String, Object> newData = new HashMap<>();
        newData.put("question", question);
        newData.put("options", options);
        logger.info(newData.toString());
        System.out.println(fastApiResponse);
        return new ApiResponse("success", "응답 수신 성공", newData);
    }

    @Operation(
            summary = "사용자에게 최종적으로 추천해 줄 옷 정보에 대한 응답",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "최종 옷 정보 응답. 각 필드는 다음과 같은 의미를 가집니다:\n" +
                            "- product_detail_images: 이미지 링크 (예: S3)\n" +
                            "- product_description: 해당 옷을 추천한 이유\n" +
                            "- product_link: 해당 옷을 판매하는 링크\n" +
                            "- product_like: 해당 추천(옷)에 대한 좋아요 수",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QARequest.class),
                            examples = @ExampleObject(
                                    name = "응답 예시",
                                    summary = "최종 옷 정보 예시",
                                    value = """
                {
                  "status": "success",
                  "message": "최종 옷 정보 응답",
                  "data":{
                    "product_detail_images": "https://image.msscdn.net/images/prd_img/202504040933220682848423367ef28d210ac3.jpg",
                      "product_description": "편안한 느낌과 무채색 취향을 반영하여 이런 옷으로 추천 드립니다.",
                      "product_link": "https://www.musinsa.com/products/4984372",
                      "product_like": 1018
                  }
                }
                """
                            )
                    )
            )
    )
    @GetMapping("/complete")
    public ApiResponse getFinalRecommendation(HttpServletRequest request){
        Map<String, Object> recommendServerResponse = new HashMap<>();
        recommendServerResponse.put("product_detail_images","https://image.msscdn.net/images/prd_img/202504040933220682848423367ef28d210ac3.jpg");
        recommendServerResponse.put("product_description","편안한 느낌과 무채색 취향을 반영하여 이런 옷으로 추천 드립니다.");
        recommendServerResponse.put("product_link","https://naver.com");
        recommendServerResponse.put("product_id","4946821");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("product_detail_images","https://image.msscdn.net/images/prd_img/202504040933220682848423367ef28d210ac3.jpg");
        responseMap.put("product_description","편안한 느낌과 무채색 취향을 반영하여 이런 옷으로 추천 드립니다.");
        responseMap.put("product_link","https://naver.com");
        return new ApiResponse("success", "응답 수신 성공", null);
    }
    private String getSessionId(HttpServletRequest request) {
        // 쿠키에서 access_token을 찾는 로직
        Cookie[] cookies = request.getCookies();
        System.out.println(cookies);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
