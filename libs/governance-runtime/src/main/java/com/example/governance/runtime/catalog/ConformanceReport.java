package com.example.governance.runtime.catalog;

import java.util.List;

public record ConformanceReport(String serviceName, boolean compliant, List<ConformanceIssue> issues) {
}
