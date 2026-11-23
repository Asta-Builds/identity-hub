package com.astabuilds.identityhub;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdentityHubApplicationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private HealthContributorRegistry healthContributorRegistry;

    @Test
    void actuatorHealthReportsUpAgainstPostgres() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("UP");

        var dbContributor = healthContributorRegistry.getContributor("db");
        assertThat(dbContributor).isInstanceOf(HealthIndicator.class);
        assertThat(((HealthIndicator) dbContributor).health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void flywayAppliesBaselineMigration() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied)
                .filteredOn(info -> "1".equals(info.getVersion().getVersion()))
                .singleElement()
                .satisfies(info -> {
                    assertThat(info.getScript()).isEqualTo("V1__baseline.sql");
                    assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS);
                });
    }
}
