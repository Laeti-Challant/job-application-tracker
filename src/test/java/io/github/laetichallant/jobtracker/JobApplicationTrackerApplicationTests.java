package io.github.laetichallant.jobtracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JobApplicationTrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
