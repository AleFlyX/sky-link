package com.skylink.land;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class SchemaSqlTests {

    private static final Set<String> ACTIVE_TABLES = Set.of(
        "department", "user", "role", "permission", "user_role", "role_permission",
        "friend_request", "friendship", "chat_group", "group_member", "message",
        "document", "document_permission", "document_group_permission", "document_favorite",
        "task", "system_config"
    );

    @Test
    void schemaIsPresentIdempotentAndNonDestructive() throws Exception {
        String schema = new ClassPathResource("schema.sql")
            .getContentAsString(StandardCharsets.UTF_8);

        assertThat(schema).doesNotContainIgnoringCase("DROP TABLE");
        assertThat(schema).containsIgnoringCase("CREATE TABLE IF NOT EXISTS");
        assertThat(schemaTables(schema)).containsExactlyInAnyOrderElementsOf(ACTIVE_TABLES);
    }

    @Test
    void schemaInitializationIsEnabledForEveryDatabase() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        Properties properties = yaml.getObject();

        assertThat(properties)
            .containsEntry("spring.sql.init.mode", "always")
            .containsEntry("spring.sql.init.schema-locations", "classpath:schema.sql")
            .containsEntry("spring.sql.init.continue-on-error", false);
    }

    private Set<String> schemaTables(String schema) {
        Matcher matcher = Pattern.compile("(?i)CREATE TABLE IF NOT EXISTS `([^`]+)`").matcher(schema);
        Set<String> tables = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            tables.add(matcher.group(1));
        }
        return tables;
    }

}
