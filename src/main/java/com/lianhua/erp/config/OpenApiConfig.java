package com.lianhua.erp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Primary
public class OpenApiConfig {
    
    @Bean
    public OpenAPI erpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lianhua ERP API")
                        .description("ERP 系統 API 文件")
                        .version("1.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org/")
                        )
                );
    }
}
