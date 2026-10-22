package com.springboot.EduSystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// This mock web context does not open a TLS listener or require local development certificates.
@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration")
class EduSystemApplicationTests {

	@Test
	void contextLoads() {
	}

}
