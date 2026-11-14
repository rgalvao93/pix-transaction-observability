package com.rodrigogalvao.transaction.controller;

import com.rodrigogalvao.transaction.dto.ErrorResponse;
import com.rodrigogalvao.transaction.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only convenience endpoint: issues a JWT for the given clientId without any
 * credential check. Not real authentication — for local testing only, until a
 * proper identity provider is integrated (GAP-4). Can be turned off with
 * {@code security.dev-token-enabled=false} (env {@code SECURITY_DEV_TOKEN_ENABLED});
 * see ADR-010 for the decision to keep it until a real IdP lands.
 */
@RestController
@RequestMapping("/auth")
@ConditionalOnProperty(name = "security.dev-token-enabled", havingValue = "true", matchIfMissing = true)
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    public AuthController(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Operation(
            summary = "Emite um JWT de teste para o clientId informado",
            description = "Endpoint de desenvolvimento/teste, sem validação de credenciais. " +
                    "Serve apenas para obter um token e testar os endpoints protegidos localmente, " +
                    "até que um provedor de identidade seja integrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Token emitido",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Payload inválido (validação)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@Valid @RequestBody TokenRequest request) {
        String token = tokenProvider.generateToken(request.clientId());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    public record TokenRequest(
            @Schema(description = "Identificador do cliente", example = "test-client")
            @NotBlank(message = "clientId is required") String clientId) {
    }

    public record TokenResponse(
            @Schema(description = "JWT de acesso", example = "eyJhbGciOiJIUzI1NiJ9...") String token) {
    }
}