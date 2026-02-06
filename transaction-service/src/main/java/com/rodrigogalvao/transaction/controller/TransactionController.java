package com.rodrigogalvao.transaction.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger logger =
            LoggerFactory.getLogger(TransactionController.class);

    @PostMapping
    public ResponseEntity<String> createTransaction(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {

        logger.info("Transaction request received",
                kv("requestId", requestId));

        return ResponseEntity.ok("Transaction received");
    }
}
