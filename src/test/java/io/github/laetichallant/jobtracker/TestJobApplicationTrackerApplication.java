package io.github.laetichallant.jobtracker;

import org.springframework.boot.SpringApplication;

/**
 * Development entry point: starts the application with the Testcontainers-backed
 * database, so the API can be run locally without installing PostgreSQL.
 *
 * <p>Run this class instead of {@link JobApplicationTrackerApplication} during development.
 */
public class TestJobApplicationTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(JobApplicationTrackerApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(args);
	}

}
