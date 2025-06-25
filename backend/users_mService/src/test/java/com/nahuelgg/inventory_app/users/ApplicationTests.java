package com.nahuelgg.inventory_app.users;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "JWT_KEY=TestSecretKeyForJWT1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ=")
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
