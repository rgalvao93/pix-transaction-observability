# Pix Transaction Observability

This repository presents an MVP design focused on observability and operational reliability
for a Pix transactional service.

## Context
A Pix transactional service responsible for cash-in and cash-out operations,
integrated with an external partner and operating in a regulated environment.

## Goal
Design a minimal but realistic architecture that is observable from day one,
allowing early detection of issues and efficient incident response.
The MVP consists of a single transactional microservice, an external partner mock,
and a basic observability stack to simulate real production scenarios.

## Scope
- Cash-in and cash-out flows
- External partner integration (mock)
- Transaction state management
- Observability-first approach

## High-Level Architecture

- Transaction Service  
  Responsible for handling Pix cash-in and cash-out transactions.
  Exposes REST APIs and manages transaction state.

- External Partner (Mock)  
  Simulates a regulated external partner for transaction processing,
  including latency and error scenarios.

- Observability Layer  
  Provides logs, metrics and traces for all transactional flows,
  enabling incident detection and root cause analysis.