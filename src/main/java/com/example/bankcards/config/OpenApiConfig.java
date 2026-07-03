package com.example.bankcards.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API управления банковскими картами",
        version = "1.0",
        description = "REST API для управления банковскими картами с JWT-аутентификацией и ролевой моделью ADMIN / USER. " +
                "Номера карт хранятся в зашифрованном виде (AES-256-GCM), в ответах возвращается маска вида **** **** **** 1234."
))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {}