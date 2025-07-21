//package com.thefirsttake.app.chat.handler;
//
//import com.thefirsttake.app.chat.service.ChatCurationGeneratorService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//@RequiredArgsConstructor
//public class ChatHandler extends TextWebSocketHandler {
////    private final RestTemplate restTemplate;
//    private final ChatCurationGeneratorService chatCurationGeneratorService;
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        System.out.println("Client connected: " + session.getId());
//    }
////    @Override
////    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
////        String currentSession=session.getId();
////        String payload = message.getPayload();
////        String responseMessage= chatMessageProcessorService.generateCurationResponse(payload,currentSession);
////
//////        System.out.println(payload);
//////        String promptValue="시나리오: 당신은 패션 큐레이터입니다. 사용자가 옷을 잘 모르기 때문에, 옷을 고를 때 최소한의 선택만 하도록 돕는 것이 목적입니다. 그래서 사용자가 원하는 취향을 유도하기 위한 답을 주어야 합니다. 다음은 질문에 대한 답을 해주세요";
//////        StringBuilder promptBuilder=new StringBuilder(promptValue);
//////        promptBuilder.append(payload);
//////        String fastApiUrl = "http://localhost:6020/api/ask";
//////        String fullPrompt = promptBuilder.toString();
//////        Map<String, String> requestMap = new HashMap<>();
//////        requestMap.put("prompt", fullPrompt);
//////        ResponseEntity<ApiResponse> responseEntity = restTemplate.postForEntity(fastApiUrl, requestMap, ApiResponse.class);
//////        ApiResponse fastApiResponse = responseEntity.getBody();
//////        String rawData=(String)fastApiResponse.getData();
////
////        session.sendMessage(new TextMessage("서버에서 받은 메시지: " + responseMessage));
////    }
//}
//
