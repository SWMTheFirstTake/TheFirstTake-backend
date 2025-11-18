package com.thefirsttake.app.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(properties = {
    "ai.server.host=localhost",
    "ai.server.port=18080"
})
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

    @Test
    @DisplayName("extractProductImageUrls - 정상 응답에서 모든 image_url 추출")
    void extractProductImageUrls_success() {
        Map<String, Object> searchResult = Map.of(
            "success", true,
            "data", Map.of(
                "data", java.util.List.of(
                    Map.of("image_url", "https://a.jpg"),
                    Map.of("image_url", "https://b.jpg")
                )
            )
        );

        var urls = productSearchService.extractProductImageUrls(searchResult);

        assertThat(urls).containsExactly("https://a.jpg", "https://b.jpg");
    }

    @Test
    @DisplayName("extractProductImageUrls - invalid 입력이면 빈 리스트")
    void extractProductImageUrls_invalid() {
        var urls1 = productSearchService.extractProductImageUrls(null);
        var urls2 = productSearchService.extractProductImageUrls(Map.of("success", false));
        var urls3 = productSearchService.extractProductImageUrls(Map.of("success", true, "data", Map.of()));

        assertThat(urls1).isEmpty();
        assertThat(urls2).isEmpty();
        assertThat(urls3).isEmpty();
    }

    @Test
    @DisplayName("ProductSearchService calls /search/ and parses response")
    void callsSearchApiAndParsesResponse() {
        String responseJson = """
        { "success": true, "data": { "data": [ { "image_url": "https://example.com/a.jpg" } ] } }
        """;

        String expectedUrl = String.format("http://%s:%s/search/", host, port);
        server.expect(once(), requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        Map<String, Object> result = productSearchService.searchProducts("하얀 린넨 셔츠 추천");

        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);

        server.verify();
    }
}


