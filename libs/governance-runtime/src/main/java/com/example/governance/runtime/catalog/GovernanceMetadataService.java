package com.example.governance.runtime.catalog;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Local cache of registry governance metadata. Refresh is version-monotonic so
 * stale webhook/poll results never overwrite newer data.
 */
@Service
public class GovernanceMetadataService {

    private static final Logger log = LoggerFactory.getLogger(GovernanceMetadataService.class);

    private final GovernanceRegistryClient client;
    private final ConcurrentMap<String, GovernanceMetadata> cache = new ConcurrentHashMap<>();

    public GovernanceMetadataService(GovernanceRegistryClient client) {
        this.client = client;
    }

    public GovernanceMetadata get(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        return cache.computeIfAbsent(assetId.trim(), id -> {
            GovernanceMetadata fetched = client.fetchSafe(id);
            return fetched != null ? fetched : placeholder(id);
        });
    }

    public void refresh(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return;
        }
        String id = assetId.trim();
        GovernanceMetadata latest = client.fetchSafe(id);
        if (latest == null) {
            return;
        }
        cache.compute(id, (key, current) ->
                current == null || latest.version() >= current.version() ? latest : current
        );
        log.info("Refreshed governance metadata for {} (version {})", id, latest.version());
    }

    public void warm(Collection<String> assetIds) {
        if (assetIds == null) {
            return;
        }
        for (String assetId : assetIds) {
            if (assetId != null && !assetId.isBlank()) {
                get(assetId.trim());
            }
        }
    }

    public ConcurrentMap<String, GovernanceMetadata> snapshot() {
        return cache;
    }

    private static GovernanceMetadata placeholder(String assetId) {
        return new GovernanceMetadata(
                assetId,
                0L,
                null,
                null,
                GovernanceMetadataScanner.domainFromAssetId(assetId),
                null,
                null,
                null
        );
    }
}
