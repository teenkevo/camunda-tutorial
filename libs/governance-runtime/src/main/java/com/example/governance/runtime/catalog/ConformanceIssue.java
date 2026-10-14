package com.example.governance.runtime.catalog;

public record ConformanceIssue(String entityName, String fieldName, String rule, String message) {
}
