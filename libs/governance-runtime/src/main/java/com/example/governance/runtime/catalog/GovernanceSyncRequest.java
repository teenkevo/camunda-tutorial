package com.example.governance.runtime.catalog;

import java.util.List;

public record GovernanceSyncRequest(
        String serviceName,
        List<GovernanceAssetDto> assets,
        List<GovernanceValidationDto> validations
) {
    public GovernanceSyncRequest {
        assets = assets == null ? List.of() : assets;
        validations = validations == null ? List.of() : validations;
    }

    public GovernanceSyncRequest(String serviceName, List<GovernanceAssetDto> assets) {
        this(serviceName, assets, List.of());
    }
}
