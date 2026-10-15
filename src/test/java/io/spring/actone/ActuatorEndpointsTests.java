package io.spring.actone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorEndpointsTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void actuatorIndexExposesRequiredLinks() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.health").exists())
				.andExpect(jsonPath("$._links.info").exists())
				.andExpect(jsonPath("$._links.shutdown").doesNotExist())
				.andExpect(jsonPath("$._links.httpexchanges").exists())
				.andExpect(jsonPath("$._links.auditevents").exists())
				.andExpect(jsonPath("$._links.threaddump").exists())
				.andExpect(jsonPath("$._links.heapdump").exists())
				.andExpect(jsonPath("$._links.logfile").exists());
	}

	@Test
	void actuatorInfoIncludesBuildAndEnvironment() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.build").isMap())
				.andExpect(jsonPath("$.env").isMap());
	}
}
