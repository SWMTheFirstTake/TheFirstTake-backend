package com.thefirsttake.app.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@ActiveProfiles("test")
class ProductSearchServiceTests {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ProductSearchService productSearchService;

    private MockRestServiceServer server;

    @Value("${ai.server.host}")
    private String host;

    @Value("${ai.server.port}")
    private String port;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    // @Test
    // @DisplayName("ProductSearchService calls /api/v1/search and parses response")
    // void callsSearchApiAndParsesResponse() {
    //     String responseJson = "{\n" +
    //             "  \"success\": true,\n" +
    //             "  \"data\": { \"data\": [{ \"image_url\": \"https://example.com/a.jpg\" }] }\n" +
    //             "}";

    //     String expectedUrl = String.format("http://%s:%s/search/", host, port);
    //     server.expect(once(), requestTo(expectedUrl))
    //             .andExpect(method(HttpMethod.POST))
    //             .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
    //             .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

    //     Map<String, Object> result = productSearchService.searchProducts("하얀색 세미오버핏 린넨셔츠에 블랙 데님팬츠가 잘 어울려");

    //     assertThat(result).isNotNull();
    //     assertThat(result.get("success")).isEqualTo(true);

    //     server.verify();
    // }
}


