package com.thefirsttake.app.chat.controller;

import com.thefirsttake.app.common.service.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerStreamTests {

    @Autowired
    private MockMvc mockMvc;

    // Controller constructor dependencies mocked
    // SpringBootTest를 사용하므로 실제 Bean들이 로드됩니다.
    // 외부 의존성만 Mock 처리
    @MockBean private S3Service s3Service;

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


