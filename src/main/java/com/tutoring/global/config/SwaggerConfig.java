package com.tutoring.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Configuration
public class SwaggerConfig {

    private static final String SCHEME_NAME = "bearer-jwt";

    static {
        // @AuthenticationPrincipal 파라미터는 API 문서에 노출하지 않는다 (전역 무시)
        SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthenticationPrincipal.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Tutoring API")
                .description("과외 워크스페이스 백엔드 API")
                .version("v1"))
            .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
            .components(new Components().addSecuritySchemes(SCHEME_NAME,
                new SecurityScheme()
                    .name(SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
