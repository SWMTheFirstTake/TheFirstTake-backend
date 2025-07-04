//package com.thefirsttake.app.config;
//
//import org.apache.hc.client5.http.config.RequestConfig;
//import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
//import org.apache.hc.client5.http.impl.classic.HttpClients;
//import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
//import org.apache.hc.core5.util.Timeout;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.ClientHttpRequestFactory;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.concurrent.TimeUnit;
//
//@Configuration
//public class RestTemplateConfig {
//
//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate(clientHttpRequestFactory());
//    }
//
//    @Bean
//    public ClientHttpRequestFactory clientHttpRequestFactory() {
//        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
//        connectionManager.setMaxTotal(100); // 전체 커넥션 풀의 최대 수
//        connectionManager.setDefaultMaxPerRoute(20); // 특정 라우트(호스트:포트)당 최대 커넥션 수
//
//        RequestConfig requestConfig = RequestConfig.custom()
//                .setConnectionRequestTimeout(Timeout.of(2000, TimeUnit.MILLISECONDS)) // 연결 요청 타임아웃
//                .setResponseTimeout(Timeout.of(5000, TimeUnit.MILLISECONDS)) // 응답 타임아웃
//                .setConnectTimeout(Timeout.of(1000, TimeUnit.MILLISECONDS)) // 연결 타임아웃
//                .build();
//
//        CloseableHttpClient httpClient = HttpClients.custom()
//                .setConnectionManager(connectionManager)
//                .setDefaultRequestConfig(requestConfig)
//                .build();
//
//        return new HttpComponentsClientHttpRequestFactory(httpClient);
//    }
//}
