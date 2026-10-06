package com.example.camundatutorial.governance;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Prevents REST controllers from returning JPA {@link Entity} instances (or collections thereof).
 * Controllers must map to DTOs; Jackson remains a final safety net for DTO fields.
 */
@ControllerAdvice
public class EntityApiResponseGuard implements ResponseBodyAdvice<Object> {

    private final SensitivityProtectionProperties properties;

    public EntityApiResponseGuard(SensitivityProtectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(@Nonnull MethodParameter returnType, @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
        return properties.isEnabled() && properties.isBlockEntityResponses();
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        Object payload = body;
        if (body instanceof ResponseEntity<?> entity) {
            payload = entity.getBody();
        }
        if (containsJpaEntity(payload)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JPA @Entity types must not be returned from REST controllers; map to a DTO first"
            );
        }
        return body;
    }

    static boolean containsJpaEntity(Object value) {
        if (value == null) {
            return false;
        }
        if (isJpaEntity(value.getClass())) {
            return true;
        }
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                if (containsJpaEntity(element)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            for (Object element : map.values()) {
                if (containsJpaEntity(element)) {
                    return true;
                }
            }
            return false;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (containsJpaEntity(Array.get(value, i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isJpaEntity(Class<?> type) {
        return type.getAnnotation(Entity.class) != null;
    }
}
