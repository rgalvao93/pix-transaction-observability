package com.rodrigogalvao.transaction.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(Instant timestamp, int status, List<String> errors) {
}
