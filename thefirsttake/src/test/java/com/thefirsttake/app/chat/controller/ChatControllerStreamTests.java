package com.thefirsttake.app.chat.controller;

import com.thefirsttake.app.chat.service.*;
import com.thefirsttake.app.common.service.S3Service;
import com.thefirsttake.app.common.user.service.UserSessionService;
import com.thefirsttake.app.chat.sse.SseInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerStreamTests {

    @Autowired
    private MockMvc mockMvc;

    // Controller constructor dependencies mocked
    @MockBean private ChatCurationOrchestrationService chatCurationOrchestrationService;
    @MockBean private ChatQueueService chatQueueService;
    @MockBean private UserSessionService userSessionService;
    @MockBean private ChatRoomManagementService chatRoomManagementService;
    @MockBean private ChatOrchestrationService chatOrchestrationService;
    @MockBean private ChatMessageService chatMessageService;
    @MockBean private S3Service s3Service;
    @MockBean private ProductSearchService productSearchService;
    @MockBean private ProductCacheService productCacheService;
    @MockBean private SseInitializer sseInitializer;
    @MockBean private RestTemplate restTemplate;
    @MockBean private RedisTemplate<String, String> redisTemplate;
    @MockBean private ChatStreamOrchestrationService chatStreamOrchestrationService;
    @MockBean private WebClient.Builder webClientBuilder;
    @MockBean private SSEConnectionService sseConnectionService;
    @MockBean private MessageStorageService messageStorageService;
    @MockBean private ChatAIService chatAIService;
    @MockBean private ChatPromptService chatPromptService;
    @MockBean private StringRedisTemplate stringRedisTemplate;
    @MockBean private ExpertStreamService expertStreamService;
    @MockBean private StreamMetricsService streamMetricsService;
    @MockBean private ProductSearchStreamService productSearchStreamService;
    @MockBean private NewLLMStreamService newLLMStreamService;

    @Test
    @DisplayName("GET /api/chat/rooms/messages/stream returns 200")
    void streamWithoutRoomId_returnsEventStream() throws Exception {
        mockMvc.perform(get("/api/chat/rooms/messages/stream")
                        .param("user_input", "소개팅"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/chat/rooms/messages/stream with room_id returns 200")
    void streamWithRoomId_returnsEventStream() throws Exception {
        mockMvc.perform(get("/api/chat/rooms/messages/stream")
                        .param("room_id", "1")
                        .param("user_input", "소개팅"))
                .andExpect(status().isOk());
    }
}


