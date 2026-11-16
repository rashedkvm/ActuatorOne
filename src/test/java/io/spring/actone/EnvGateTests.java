package io.spring.actone;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EnvGateTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void disabledByDefault() throws Exception {
		mockMvc.perform(get("/api/env")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/routes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.path == '/api/env')]").isEmpty());
	}
}

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "actone.env.enabled=false")
class EnvGateDisabledTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void disabledWhenFalse() throws Exception {
		mockMvc.perform(get("/api/env")).andExpect(status().isNotFound());
	}
}

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "actone.env.enabled=TRUE")
class EnvGateCaseVariantTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void disabledForCaseVariant() throws Exception {
		mockMvc.perform(get("/api/env")).andExpect(status().isNotFound());
	}
}

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "actone.env.enabled=true")
class EnvGateEnabledTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void enabledWhenTrue() throws Exception {
		mockMvc.perform(get("/api/env"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.platformDetected").exists())
				.andExpect(jsonPath("$.count").exists())
				.andExpect(jsonPath("$.variables").isMap());
	}
}
