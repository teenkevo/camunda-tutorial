package com.example.governance.service.dto;

public record ConformanceIssue(String entityName, String fieldName, String rule, String message) {
}
