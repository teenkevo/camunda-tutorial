package com.example.governance.service.dto;

import java.util.List;

public record ConformanceReport(String serviceName, boolean compliant, List<ConformanceIssue> issues) {
}
