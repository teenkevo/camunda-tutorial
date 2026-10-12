package com.example.governance.runtime.catalog;

import com.example.governance.annotations.DataDomain;
import com.example.governance.annotations.DataOwner;
import com.example.governance.annotations.Sensitive;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.hibernate.annotations.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.stereotype.Component;

/**
 * Always writes DB remarks via {@code COMMENT ON TABLE/COLUMN} on startup.
 * Hibernate's schema update does not reliably apply {@link Comment} on Oracle after tables exist.
 */
@Component
@Order(200)
public class GovernanceDbCommentApplier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GovernanceDbCommentApplier.class);

    private final DataSource dataSource;
    private final Environment environment;
    private final GovernanceCatalogProperties catalogProperties;

    public GovernanceDbCommentApplier(
            DataSource dataSource,
            Environment environment,
            GovernanceCatalogProperties catalogProperties
    ) {
        this.dataSource = dataSource;
        this.environment = environment;
        this.catalogProperties = catalogProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> packages = scanPackages();
        List<String> statements = buildCommentStatements(packages);
        if (statements.isEmpty()) {
            log.warn("No DB comments to apply (scan packages: {})", packages);
            return;
        }
        int applied = 0;
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                try {
                    statement.execute(sql);
                    applied++;
                    log.debug("{}", sql);
                } catch (Exception ex) {
                    log.warn("Failed to apply DB comment: {} — {}", sql, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Governance DB comment pass failed: {}", ex.getMessage());
            return;
        }
        log.info("Applied {}/{} governance DB comment statement(s) for packages {}", applied, statements.size(), packages);
    }

    List<String> buildCommentStatements(Set<String> packages) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.setResourceLoader(new DefaultResourceLoader(classLoader));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<String> sql = new ArrayList<>();
        for (String basePackage : packages) {
            for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
                try {
                    Class<?> type = Class.forName(definition.getBeanClassName(), true, classLoader);
                    sql.addAll(commentsForEntity(type));
                } catch (ClassNotFoundException ex) {
                    throw new IllegalStateException("Failed to load entity " + definition.getBeanClassName(), ex);
                }
            }
        }
        return sql;
    }

    private List<String> commentsForEntity(Class<?> type) {
        List<String> sql = new ArrayList<>();
        String tableName = resolveTableName(type);

        String tableComment = tableComment(type);
        if (tableComment != null && !tableComment.isBlank()) {
            sql.add("COMMENT ON TABLE " + tableName + " IS '" + escape(tableComment) + "'");
        }

        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                String columnComment = columnComment(field);
                if (columnComment == null || columnComment.isBlank()) {
                    continue;
                }
                String columnName = resolveColumnName(field);
                sql.add("COMMENT ON COLUMN " + tableName + "." + columnName + " IS '" + escape(columnComment) + "'");
            }
            current = current.getSuperclass();
        }
        return sql;
    }

    private static String tableComment(Class<?> type) {
        List<String> parts = new ArrayList<>();
        Comment comment = type.getAnnotation(Comment.class);
        if (comment != null && !comment.value().isBlank()) {
            parts.add(comment.value().trim());
        }
        DataOwner owner = type.getAnnotation(DataOwner.class);
        if (owner != null && !owner.value().isBlank()) {
            parts.add("owner=" + owner.value().trim());
        }
        DataDomain domain = type.getAnnotation(DataDomain.class);
        if (domain != null && !domain.value().isBlank()) {
            parts.add("domain=" + domain.value().trim());
        }
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private static String columnComment(Field field) {
        List<String> parts = new ArrayList<>();
        Comment comment = field.getAnnotation(Comment.class);
        if (comment != null && !comment.value().isBlank()) {
            parts.add(comment.value().trim());
        }
        Sensitive sensitive = field.getAnnotation(Sensitive.class);
        if (sensitive != null) {
            parts.add(sensitive.value().commentTag());
        }
        DataOwner owner = field.getAnnotation(DataOwner.class);
        if (owner != null && !owner.value().isBlank()) {
            parts.add("owner=" + owner.value().trim());
        }
        DataDomain domain = field.getAnnotation(DataDomain.class);
        if (domain != null && !domain.value().isBlank()) {
            parts.add("domain=" + domain.value().trim());
        }
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private static String resolveTableName(Class<?> type) {
        Table table = type.getAnnotation(Table.class);
        if (table != null && !table.name().isBlank()) {
            return table.name();
        }
        return toSnakeCase(type.getSimpleName());
    }

    private static String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isBlank()) {
            return column.name();
        }
        return toSnakeCase(field.getName());
    }

    /** Matches Spring Boot default CamelCaseToUnderscoresNamingStrategy. */
    static String toSnakeCase(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escape(String value) {
        return value.replace("'", "''");
    }

    private Set<String> scanPackages() {
        Set<String> packages = new LinkedHashSet<>();
        String configured = environment.getProperty("app.governance.catalog.scan-packages");
        if (configured != null && !configured.isBlank()) {
            for (String part : configured.split(",")) {
                if (!part.isBlank()) {
                    packages.add(part.trim());
                }
            }
        }
        if (packages.isEmpty()) {
            String appName = environment.getProperty("spring.application.name", catalogProperties.getServiceName());
            packages.add("com.example." + appName.replace("-service", "").replace('-', '.'));
        }
        return packages;
    }
}
