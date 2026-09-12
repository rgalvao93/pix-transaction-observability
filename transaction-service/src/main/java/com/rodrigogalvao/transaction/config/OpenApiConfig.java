package com.rodrigogalvao.transaction.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "transaction-service — Pix Transaction API",
                version = "1.0.0",
                description = "Serviço transacional Pix (cash-in/cash-out) com observabilidade." +
                        "Autenticação: JWT Bearer emitido por POST /auth/token (demo, sem validação de credenciais)."),
        servers = @Server(url = "http://localhost:8080", description = "local dev"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}