package com.sentinelcore.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BackendApplicationTests {

	@org.springframework.beans.factory.annotation.Autowired
	private com.sentinelcore.backend.repository.UserRepository userRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}
}
