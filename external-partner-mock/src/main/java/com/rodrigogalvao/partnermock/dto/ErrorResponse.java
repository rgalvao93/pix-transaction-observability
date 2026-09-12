package com.rodrigogalvao.partnermock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ErrorResponse", description = "Erro de validação de payload (HTTP 400)")
public record ErrorResponse(
        @Schema(description = "Momento em que o erro ocorreu", example = "2026-07-11T10:00:00Z") Instant timestamp,
        @Schema(description = "Código HTTP do erro", example = "400") int status,
        @Schema(description = "Lista de mensagens de validação", example = "[\"accountId is required\"]") List<String> errors) {
}