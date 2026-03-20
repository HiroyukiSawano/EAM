package com.eam.assetcenter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置，用于生成接口说明。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 构建系统的 OpenAPI 文档基础信息。
     */
    @Bean
    public OpenAPI assetCenterOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Information Asset Management Center API")
                        .description("Backend APIs for the information asset management center")
                        .version("v1")
                        .license(new License().name("Internal Use")));
    }
}


