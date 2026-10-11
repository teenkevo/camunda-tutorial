package com.example.governance.service.dto;

import java.util.List;

public record GovernanceSyncRequest(String serviceName, List<GovernanceAssetDto> assets) {
}
