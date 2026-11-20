package com.astabuilds.identityhub;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthActuatorIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private HealthContributorRegistry healthContributorRegistry;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void healthEndpointReturnsUpWithoutExposingComponentDetails() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().has("components")).isFalse();
    }

    @Test
    void databaseHealthIndicatorIsUpOnPostgres17() throws Exception {
        HealthContributor dbContributor = healthContributorRegistry.getContributor("db");

        assertThat(dbContributor).isInstanceOf(HealthIndicator.class);
        assertThat(((HealthIndicator) dbContributor).health().getStatus()).isEqualTo(Status.UP);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(17);
        }
    }

    @Test
    void sensitiveActuatorEndpointsAreNotExposed() {
        assertThat(restTemplate.getForEntity("/actuator/env", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/actuator/beans", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/actuator/flyway", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void flywayIsInitializedWithNoDomainMigrations() {
        assertThat(flyway).isNotNull();
        assertThat(flyway.info().all()).isEmpty();

        Integer historyTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_name = 'flyway_schema_history'",
                Integer.class);
        assertThat(historyTableCount).isEqualTo(1);
    }
}
