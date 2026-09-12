package com.rodrigogalvao.partnermock.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "external-partner-mock — Pix External Partner API",
                version = "1.0.0",
                description = "Simula o parceiro externo (PSP / Banco Central), fonte de verdade do saldo, " +
                        "com latência artificial e taxa de falha configuráveis. " +
                        "Autenticação: JWT Bearer assinado com o mesmo segredo do transaction-service."),
        servers = @Server(url = "http://localhost:8081", description = "local dev"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}