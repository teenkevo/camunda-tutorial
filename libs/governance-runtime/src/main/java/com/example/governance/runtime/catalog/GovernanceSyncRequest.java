package com.example.governance.runtime.catalog;

import java.util.List;

public record GovernanceSyncRequest(String serviceName, List<GovernanceAssetDto> assets) {
}
