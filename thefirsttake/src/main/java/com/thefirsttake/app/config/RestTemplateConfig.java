package com.thefirsttake.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 연결 타임아웃 30초 (이미지 다운로드용)
        factory.setReadTimeout(60000);    // 읽기 타임아웃 60초 (대용량 이미지 다운로드용)
        
        return new RestTemplate(factory);
    }
}
