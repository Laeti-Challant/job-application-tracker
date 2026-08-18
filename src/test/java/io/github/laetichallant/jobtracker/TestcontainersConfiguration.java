package io.github.laetichallant.jobtracker;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides a throwaway PostgreSQL instance for tests.
 *
 * <p>The image tag is pinned on purpose: an unpinned {@code latest} would silently
 * change the database engine under the test suite whenever a new major version ships.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	private static final String POSTGRES_IMAGE = "postgres:18-alpine";

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE));
	}

}
