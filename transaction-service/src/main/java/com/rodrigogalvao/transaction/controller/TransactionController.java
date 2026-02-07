package com.rodrigogalvao.transaction.controller;

import com.rodrigogalvao.transaction.dto.TransactionRequest;
import com.rodrigogalvao.transaction.dto.TransactionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestBody TransactionRequest request) {

        TransactionResponse response = new TransactionResponse(
                request.getTransactionId(),
                "PROCESSED",
                null
        );

        return ResponseEntity.ok(response);
    }
}
