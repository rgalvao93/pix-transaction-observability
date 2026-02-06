package com.rodrigogalvao.transaction.controller;

import com.rodrigogalvao.transaction.model.TransactionRequest;
import com.rodrigogalvao.transaction.model.TransactionResponse;
import com.rodrigogalvao.transaction.model.TransactionStatus;
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
                TransactionStatus.PROCESSED.name(),
                null
        );

        return ResponseEntity.ok(response);
    }
}