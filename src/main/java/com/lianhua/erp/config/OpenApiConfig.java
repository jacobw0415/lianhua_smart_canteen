package com.lianhua.erp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI erpOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Lianhua ERP API")
                        .description("ERP 系統 API 文件")
                        .version("1.0"));
    }
}
