package com.rodrigogalvao.transaction.controller;

import com.rodrigogalvao.transaction.security.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only convenience endpoint: issues a JWT for the given clientId without any
 * credential check. Not real authentication — for local testing only, until a
 * proper identity provider is integrated.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    public AuthController(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@Valid @RequestBody TokenRequest request) {
        String token = tokenProvider.generateToken(request.clientId());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    public record TokenRequest(@NotBlank(message = "clientId is required") String clientId) {
    }

    public record TokenResponse(String token) {
    }
}
