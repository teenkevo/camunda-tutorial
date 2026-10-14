package com.example.governance.service.openmetadata;

import com.example.governance.service.config.OpenMetadataProperties;
import com.example.governance.service.dto.GovernanceMetadataDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin REST client for OpenMetadata glossary terms used as governed assets.
 */
@Component
public class OpenMetadataClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMetadataClient.class);

    private final RestTemplate restTemplate;
    private final OpenMetadataProperties properties;
    private final ObjectMapper objectMapper;

    private volatile String jwtToken;

    public OpenMetadataClient(
            RestTemplateBuilder builder,
            OpenMetadataProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = builder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isReachable() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            ensureToken();
            exchange("/v1/system/version", HttpMethod.GET, null, JsonNode.class);
            return true;
        } catch (RuntimeException ex) {
            log.debug("OpenMetadata not reachable: {}", ex.getMessage());
            return false;
        }
    }

    public Optional<GovernanceMetadataDto> fetchAsset(String assetId) {
        if (!properties.isEnabled() || assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        String fqn = glossaryTermFqn(assetId.trim());
        try {
            ensureToken();
            JsonNode term = exchange(
                    "/v1/glossaryTerms/name/" + encodePath(fqn) + "?fields=owners,tags,extension",
                    HttpMethod.GET,
                    null,
                    JsonNode.class
            );
            return Optional.ofNullable(mapTerm(term));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("Failed to fetch OpenMetadata asset {}: {}", assetId, ex.getMessage());
            return Optional.empty();
        }
    }

    public void ensureGlossary() {
        ensureToken();
        String name = properties.getGlossaryName();
        try {
            exchange("/v1/glossaries/name/" + encodePath(name), HttpMethod.GET, null, JsonNode.class);
        } catch (HttpClientErrorException.NotFound ex) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", name);
            body.put("displayName", name);
            body.put("description", "Governed logical assets for Camunda tutorial microservices");
            exchange("/v1/glossaries", HttpMethod.POST, body, JsonNode.class);
            log.info("Created OpenMetadata glossary {}", name);
        }
        ensureCustomProperties();
    }

    public void upsertAsset(OpenMetadataProperties.SeedAsset seed) {
        ensureGlossary();
        String assetId = seed.getAssetId().trim();
        String termName = termNameFromAssetId(assetId);
        String fqn = glossaryTermFqn(assetId);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", termName);
        body.put("displayName", assetId);
        body.put("description", nullToEmpty(seed.getDescription()));
        body.put("glossary", properties.getGlossaryName());

        ArrayNode owners = body.putArray("owners");
        if (seed.getOwner() != null && !seed.getOwner().isBlank()) {
            // Store owner as display name via team-like reference using name in description extension;
            // OM requires UUID owners — we put owner into extension and also as display annotation.
        }

        ObjectNode extension = objectMapper.createObjectNode();
        extension.put("steward", nullToEmpty(seed.getSteward()));
        extension.put("retentionPolicy", nullToEmpty(seed.getRetentionPolicy()));
        extension.put("businessOwner", nullToEmpty(seed.getOwner()));
        body.set("extension", extension);

        if (seed.getClassification() != null && !seed.getClassification().isBlank()) {
            ensureClassificationTag(seed.getClassification());
            ArrayNode tags = body.putArray("tags");
            ObjectNode tag = tags.addObject();
            tag.put("tagFQN", "GovernanceClassification." + seed.getClassification().trim());
            tag.put("source", "Classification");
            tag.put("labelType", "Manual");
            tag.put("state", "Confirmed");
        }

        try {
            exchange("/v1/glossaryTerms/name/" + encodePath(fqn), HttpMethod.GET, null, JsonNode.class);
            // Exists — PATCH extension / description
            ArrayNode patch = objectMapper.createArrayNode();
            patch.add(op("add", "/description", nullToEmpty(seed.getDescription())));
            patch.add(op("add", "/extension", extension));
            if (seed.getClassification() != null && !seed.getClassification().isBlank()) {
                ArrayNode tags = objectMapper.createArrayNode();
                ObjectNode tag = tags.addObject();
                tag.put("tagFQN", "GovernanceClassification." + seed.getClassification().trim());
                tag.put("source", "Classification");
                tag.put("labelType", "Manual");
                tag.put("state", "Confirmed");
                patch.add(op("add", "/tags", tags));
            }
            exchangeJsonPatch("/v1/glossaryTerms/name/" + encodePath(fqn), patch);
            log.info("Updated OpenMetadata governed asset {}", assetId);
        } catch (HttpClientErrorException.NotFound ex) {
            exchange("/v1/glossaryTerms", HttpMethod.POST, body, JsonNode.class);
            log.info("Created OpenMetadata governed asset {}", assetId);
        }
    }

    private void ensureCustomProperties() {
        try {
            JsonNode type = exchange(
                    "/v1/metadata/types/name/glossaryTerm?fields=customProperties",
                    HttpMethod.GET,
                    null,
                    JsonNode.class
            );
            String typeId = type.path("id").asText(null);
            if (typeId == null || typeId.isBlank()) {
                return;
            }
            JsonNode stringType = exchange("/v1/metadata/types/name/string", HttpMethod.GET, null, JsonNode.class);
            String stringTypeId = stringType.path("id").asText(null);
            if (stringTypeId == null) {
                return;
            }
            ensureProperty(typeId, stringTypeId, "steward", "Data steward team");
            ensureProperty(typeId, stringTypeId, "retentionPolicy", "Retention policy code");
            ensureProperty(typeId, stringTypeId, "businessOwner", "Business owner / team");
        } catch (RestClientException ex) {
            log.warn("Could not ensure glossaryTerm custom properties: {}", ex.getMessage());
        }
    }

    private void ensureProperty(String typeId, String stringTypeId, String name, String description) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", name);
        body.put("description", description);
        ObjectNode propertyType = body.putObject("propertyType");
        propertyType.put("id", stringTypeId);
        propertyType.put("type", "type");
        try {
            exchange("/v1/metadata/types/" + typeId, HttpMethod.PUT, body, JsonNode.class);
        } catch (HttpClientErrorException ex) {
            // already exists or conflict — ignore
            log.debug("Custom property {} ensure: {}", name, ex.getStatusCode());
        }
    }

    private void ensureClassificationTag(String classification) {
        String name = classification.trim();
        try {
            exchange("/v1/classifications/name/GovernanceClassification", HttpMethod.GET, null, JsonNode.class);
        } catch (HttpClientErrorException.NotFound ex) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", "GovernanceClassification");
            body.put("description", "Sensitivity classifications for governed assets");
            exchange("/v1/classifications", HttpMethod.POST, body, JsonNode.class);
        }
        String tagFqn = "GovernanceClassification." + name;
        try {
            exchange("/v1/tags/name/" + encodePath(tagFqn), HttpMethod.GET, null, JsonNode.class);
        } catch (HttpClientErrorException.NotFound ex) {
            ObjectNode tag = objectMapper.createObjectNode();
            tag.put("name", name);
            tag.put("description", name + " classification");
            tag.put("classification", "GovernanceClassification");
            exchange("/v1/tags", HttpMethod.POST, tag, JsonNode.class);
        }
    }

    private GovernanceMetadataDto mapTerm(JsonNode term) {
        if (term == null || term.isMissingNode()) {
            return null;
        }
        String displayName = text(term, "displayName");
        String name = text(term, "name");
        String fqn = text(term, "fullyQualifiedName");
        String assetId = displayName != null && displayName.contains(".")
                ? displayName
                : assetIdFromFqn(fqn, name);

        JsonNode extension = term.path("extension");
        String steward = text(extension, "steward");
        String retention = text(extension, "retentionPolicy");
        String owner = text(extension, "businessOwner");
        if (owner == null || owner.isBlank()) {
            JsonNode owners = term.path("owners");
            if (owners.isArray() && !owners.isEmpty()) {
                owner = text(owners.get(0), "name");
                if (owner == null || owner.isBlank()) {
                    owner = text(owners.get(0), "displayName");
                }
            }
        }

        String classification = null;
        JsonNode tags = term.path("tags");
        if (tags.isArray()) {
            for (JsonNode tag : tags) {
                String tagFqn = text(tag, "tagFQN");
                if (tagFqn != null && tagFqn.startsWith("GovernanceClassification.")) {
                    classification = tagFqn.substring("GovernanceClassification.".length());
                    break;
                }
            }
        }

        long version = 0L;
        if (term.has("version") && !term.get("version").isNull()) {
            // OM versions are doubles like 0.1, 0.2 — scale to long ticks
            double v = term.get("version").asDouble(0);
            version = Math.round(v * 10);
        }
        String domain = domainFromAssetId(assetId);
        return new GovernanceMetadataDto(
                assetId,
                version,
                owner,
                steward,
                domain,
                classification,
                retention,
                text(term, "description")
        );
    }

    private void ensureToken() {
        if (jwtToken != null && !jwtToken.isBlank()) {
            return;
        }
        synchronized (this) {
            if (jwtToken != null && !jwtToken.isBlank()) {
                return;
            }
            login();
        }
    }

    private void login() {
        String url = trimSlash(properties.getBaseUrl()) + "/v1/users/login";
        // OpenMetadata basic login expects JSON {"email","password"} with password base64-encoded
        ObjectNode body = objectMapper.createObjectNode();
        String email = properties.getUsername().contains("@")
                ? properties.getUsername()
                : properties.getUsername() + "@open-metadata.org";
        body.put("email", email);
        body.put("password", Base64.getEncoder().encodeToString(
                properties.getPassword().getBytes(StandardCharsets.UTF_8)
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );
            JsonNode root = response.getBody();
            if (root == null) {
                throw new IllegalStateException("Empty login response from OpenMetadata");
            }
            String token = text(root, "accessToken");
            if (token == null) {
                token = text(root.path("token"), "accessToken");
            }
            if (token == null && root.has("accessToken")) {
                token = root.get("accessToken").asText();
            }
            // Some versions nest under JWTAuthResponse
            if (token == null) {
                Iterator<String> names = root.fieldNames();
                while (names.hasNext()) {
                    String field = names.next();
                    if (field.toLowerCase(Locale.ROOT).contains("token")) {
                        JsonNode n = root.get(field);
                        if (n != null && n.isTextual()) {
                            token = n.asText();
                            break;
                        }
                        if (n != null && n.has("accessToken")) {
                            token = n.get("accessToken").asText();
                            break;
                        }
                    }
                }
            }
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("OpenMetadata login did not return accessToken: " + root);
            }
            this.jwtToken = token;
            log.info("Authenticated to OpenMetadata as {}", email);
        } catch (RestClientException ex) {
            throw new IllegalStateException("OpenMetadata login failed: " + ex.getMessage(), ex);
        }
    }

    void clearToken() {
        this.jwtToken = null;
    }

    private <T> T exchange(String path, HttpMethod method, Object body, Class<T> type) {
        String url = trimSlash(properties.getBaseUrl()) + path;
        HttpHeaders headers = authHeaders();
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), type);
            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized ex) {
            clearToken();
            ensureToken();
            headers = authHeaders();
            if (body != null) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
            ResponseEntity<T> response = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), type);
            return response.getBody();
        }
    }

    private void exchangeJsonPatch(String path, ArrayNode patch) {
        String url = trimSlash(properties.getBaseUrl()) + path;
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.valueOf("application/json-patch+json"));
        restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(patch, headers), JsonNode.class);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        return headers;
    }

    private ObjectNode op(String op, String path, Object value) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("op", op);
        node.put("path", path);
        if (value instanceof JsonNode jn) {
            node.set("value", jn);
        } else if (value == null) {
            node.putNull("value");
        } else {
            node.put("value", String.valueOf(value));
        }
        return node;
    }

    private String glossaryTermFqn(String assetId) {
        return properties.getGlossaryName() + "." + termNameFromAssetId(assetId);
    }

    static String termNameFromAssetId(String assetId) {
        // Glossary term names cannot contain '.'; use '__'
        return assetId.trim().replace('.', '_');
    }

    static String assetIdFromFqn(String fqn, String name) {
        if (name != null && name.contains("_")) {
            // GovernedAssets.refunds_application → refunds.application (first underscore only for domain)
            int idx = name.indexOf('_');
            if (idx > 0) {
                return name.substring(0, idx) + "." + name.substring(idx + 1).replace('_', '.');
            }
        }
        if (fqn != null && fqn.contains(".")) {
            String term = fqn.substring(fqn.indexOf('.') + 1);
            int idx = term.indexOf('_');
            if (idx > 0) {
                return term.substring(0, idx) + "." + term.substring(idx + 1).replace('_', '.');
            }
            return term;
        }
        return name;
    }

    public static String domainFromAssetId(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        int dot = assetId.indexOf('.');
        return dot <= 0 ? assetId.trim() : assetId.substring(0, dot).trim();
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String v = node.get(field).asText();
        return v == null || v.isBlank() ? null : v;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String trimSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8585/api";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String encodePath(String path) {
        return path.replace(" ", "%20");
    }
}
