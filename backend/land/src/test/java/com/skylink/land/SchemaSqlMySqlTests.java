package com.skylink.land;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@EnabledIfEnvironmentVariable(named = "TEST_MYSQL_URL", matches = ".+")
class SchemaSqlMySqlTests {

    @Test
    void schemaExecutesTwiceAgainstMySql() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            System.getenv("TEST_MYSQL_URL"),
            System.getenv("TEST_MYSQL_USERNAME"),
            System.getenv("TEST_MYSQL_PASSWORD")
        );
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource("schema.sql"),
            new ClassPathResource("data.sql")
        );

        populator.execute(dataSource);
        populator.execute(dataSource);
    }
}
