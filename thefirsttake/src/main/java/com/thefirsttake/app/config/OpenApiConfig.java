package com.thefirsttake.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("The First Take API")
                        .version("1.0.0")
                        .description("AI 기반 패션 큐레이션 및 가상 피팅 서비스 API")
                        .contact(new Contact()
                                .name("The First Take Team")
                                .email("support@the-first-take.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8000").description("로컬 개발 서버"),
                        new Server().url("https://the-first-take.com").description("프로덕션 서버")
                ));
    }
}

