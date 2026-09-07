package br.com.diegocordeiro.dscproject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
class DscprojectApplicationTests {

	@Test
	void contextLoads() {
	}

}
