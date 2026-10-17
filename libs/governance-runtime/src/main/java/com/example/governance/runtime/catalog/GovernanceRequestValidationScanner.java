package com.example.governance.runtime.catalog;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Discovers Jakarta Bean Validation constraints on {@code @Valid @RequestBody}
 * DTOs used by REST controllers. Stable ids:
 * {@code {service}.{DtoSimpleName}.{propertyPath}.{ConstraintSimpleName}}.
 */
@Component
public class GovernanceRequestValidationScanner {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public List<GovernanceValidationDto> scan(String serviceName, Set<String> basePackages) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Map<String, GovernanceValidationDto> byId = new LinkedHashMap<>();
        for (String basePackage : basePackages) {
            for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
                String className = definition.getBeanClassName();
                if (className == null) {
                    continue;
                }
                try {
                    Class<?> type = Class.forName(className);
                    for (GovernanceValidationDto dto : describeController(serviceName, type)) {
                        byId.putIfAbsent(dto.getValidationId(), dto);
                    }
                } catch (ClassNotFoundException ex) {
                    throw new IllegalStateException("Failed to load type " + className, ex);
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    private List<GovernanceValidationDto> describeController(String serviceName, Class<?> controllerType) {
        List<GovernanceValidationDto> found = new ArrayList<>();
        String typePath = firstPath(classLevelPaths(controllerType));

        for (Method method : controllerType.getDeclaredMethods()) {
            if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                continue;
            }
            String httpMethod = resolveHttpMethod(method);
            String requestPath = joinPaths(typePath, firstPath(methodLevelPaths(method)));

            for (Parameter parameter : method.getParameters()) {
                if (!isValidatedRequestBody(parameter)) {
                    continue;
                }
                found.addAll(describeDtoConstraints(
                        serviceName,
                        parameter.getType(),
                        controllerType.getName(),
                        method.getName(),
                        httpMethod,
                        requestPath
                ));
            }
        }
        return found;
    }

    private List<GovernanceValidationDto> describeDtoConstraints(
            String serviceName,
            Class<?> dtoType,
            String controllerType,
            String controllerMethod,
            String httpMethod,
            String requestPath
    ) {
        List<GovernanceValidationDto> found = new ArrayList<>();
        Set<Class<?>> visited = new LinkedHashSet<>();
        collectConstraints(
                serviceName,
                dtoType,
                dtoType.getSimpleName(),
                "",
                controllerType,
                controllerMethod,
                httpMethod,
                requestPath,
                visited,
                found
        );
        return found;
    }

    private void collectConstraints(
            String serviceName,
            Class<?> type,
            String rootDtoSimpleName,
            String propertyPrefix,
            String controllerType,
            String controllerMethod,
            String httpMethod,
            String requestPath,
            Set<Class<?>> visited,
            List<GovernanceValidationDto> out
    ) {
        if (type == null || type.isPrimitive() || type.getName().startsWith("java.") || !visited.add(type)) {
            return;
        }

        BeanDescriptor bean = validator.getConstraintsForClass(type);
        if (!bean.isBeanConstrained()) {
            return;
        }

        for (PropertyDescriptor property : bean.getConstrainedProperties()) {
            String propertyPath = propertyPrefix.isEmpty()
                    ? property.getPropertyName()
                    : propertyPrefix + "." + property.getPropertyName();

            for (ConstraintDescriptor<?> constraint : property.getConstraintDescriptors()) {
                Annotation annotation = constraint.getAnnotation();
                if (annotation == null) {
                    continue;
                }
                Class<? extends Annotation> annType = annotation.annotationType();
                if (Valid.class.equals(annType)) {
                    continue;
                }

                // Catalog the annotation as declared (e.g. NotBlank), not composing NotNull/Size.
                String constraintName = annType.getSimpleName();
                String validationId =
                        serviceName + "." + rootDtoSimpleName + "." + propertyPath + "." + constraintName;

                GovernanceValidationDto dto = new GovernanceValidationDto();
                dto.setServiceName(serviceName);
                dto.setValidationId(validationId);
                dto.setDtoType(type.getName());
                dto.setFieldName(propertyPath);
                dto.setConstraintName(constraintName);
                dto.setConstraintType(annType.getName());
                Object message = constraint.getAttributes().get("message");
                dto.setMessage(message == null ? null : String.valueOf(message));
                dto.setControllerType(controllerType);
                dto.setControllerMethod(controllerMethod);
                dto.setHttpMethod(httpMethod);
                dto.setRequestPath(requestPath);
                out.add(dto);
            }

            if (property.isCascaded()) {
                collectConstraints(
                        serviceName,
                        property.getElementClass(),
                        rootDtoSimpleName,
                        propertyPath,
                        controllerType,
                        controllerMethod,
                        httpMethod,
                        requestPath,
                        visited,
                        out
                );
            }
        }
    }

    private static boolean isValidatedRequestBody(Parameter parameter) {
        if (!parameter.isAnnotationPresent(RequestBody.class)) {
            return false;
        }
        return parameter.isAnnotationPresent(Valid.class)
                || parameter.isAnnotationPresent(Validated.class);
    }

    private static String resolveHttpMethod(Method method) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping == null || mapping.method().length == 0) {
            return "";
        }
        return mapping.method()[0].name();
    }

    private static String[] classLevelPaths(Class<?> type) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
        if (mapping == null) {
            return new String[0];
        }
        return mapping.path().length > 0 ? mapping.path() : mapping.value();
    }

    private static String[] methodLevelPaths(Method method) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping == null) {
            return new String[0];
        }
        return mapping.path().length > 0 ? mapping.path() : mapping.value();
    }

    private static String firstPath(String[] paths) {
        if (paths == null || paths.length == 0) {
            return "";
        }
        for (String path : paths) {
            if (path != null && !path.isBlank()) {
                return path;
            }
        }
        return "";
    }

    private static String joinPaths(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        if (a.isEmpty()) {
            return normalizePath(b);
        }
        if (b.isEmpty()) {
            return normalizePath(a);
        }
        if (a.endsWith("/")) {
            a = a.substring(0, a.length() - 1);
        }
        if (!b.startsWith("/")) {
            b = "/" + b;
        }
        return normalizePath(a + b);
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String value = path.startsWith("/") ? path : "/" + path;
        if (value.endsWith("/") && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
