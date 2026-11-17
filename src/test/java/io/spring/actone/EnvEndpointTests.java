package io.spring.actone;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "actone.env.enabled=true")
class EnvEndpointTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TestEnvironmentSource environmentSource;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void returnsSortedVariablesAndCount() throws Exception {
		environmentSource.set(Map.of("VCAP_Z", "z", "CF_A", "a", "PORT", "8080"));
		mockMvc.perform(get("/api/env"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.platformDetected").value(true))
				.andExpect(jsonPath("$.count").value(3))
				.andExpect(jsonPath("$.variables.PORT").value("8080"))
				.andExpect(jsonPath("$.variables.CF_A").value("a"))
				.andExpect(jsonPath("$.variables.VCAP_Z").value("z"))
				.andExpect(jsonPath("$.variables.*", org.hamcrest.Matchers.hasSize(3)))
				.andExpect(result -> {
					String body = result.getResponse().getContentAsString();
					org.junit.jupiter.api.Assertions.assertTrue(body.indexOf("CF_A") < body.indexOf("PORT"));
					org.junit.jupiter.api.Assertions.assertTrue(body.indexOf("PORT") < body.indexOf("VCAP_Z"));
				});
	}

	@Test
	void filtersToAllowlist() throws Exception {
		Map<String, String> source = new LinkedHashMap<>();
		for (String name : new String[] { "VCAP_ONE", "CF_TWO", "PORT", "MEMORY_LIMIT", "INSTANCE_INDEX",
				"INSTANCE_GUID", "LANG", "TMPDIR", "USER", "HOME" }) {
			source.put(name, "value");
		}
		source.put("vcap_lower", "secret");
		source.put("CFX", "secret");
		source.put("PORT_EXTRA", "secret");
		source.put("DATABASE_URL", "password");
		source.put("UNRELATED_SECRET", "secret");
		environmentSource.set(source);
		mockMvc.perform(get("/api/env"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.variables.VCAP_ONE").value("value"))
				.andExpect(jsonPath("$.variables.CF_TWO").value("value"))
				.andExpect(jsonPath("$.variables.PORT").value("value"))
				.andExpect(jsonPath("$.variables.MEMORY_LIMIT").value("value"))
				.andExpect(jsonPath("$.variables.INSTANCE_INDEX").value("value"))
				.andExpect(jsonPath("$.variables.INSTANCE_GUID").value("value"))
				.andExpect(jsonPath("$.variables.LANG").value("value"))
				.andExpect(jsonPath("$.variables.TMPDIR").value("value"))
				.andExpect(jsonPath("$.variables.USER").value("value"))
				.andExpect(jsonPath("$.variables.HOME").value("value"))
				.andExpect(jsonPath("$.variables.vcap_lower").doesNotExist())
				.andExpect(jsonPath("$.variables.CFX").doesNotExist())
				.andExpect(jsonPath("$.variables.PORT_EXTRA").doesNotExist())
				.andExpect(jsonPath("$.variables.DATABASE_URL").doesNotExist())
				.andExpect(jsonPath("$.variables.UNRELATED_SECRET").doesNotExist());
	}

	@Test
	void platformDetectedIsNamePresenceHeuristic() throws Exception {
		environmentSource.set(Map.of());
		mockMvc.perform(get("/api/env"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.platformDetected").value(false))
				.andExpect(jsonPath("$.count").value(0));
		environmentSource.set(Map.of("PORT", "8080"));
		mockMvc.perform(get("/api/env"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.platformDetected").value(false))
				.andExpect(jsonPath("$.count").value(1));
		Map<String, String> platformNames = new LinkedHashMap<>();
		platformNames.put("CF_EMPTY", "");
		platformNames.put("VCAP_NULL", null);
		environmentSource.set(platformNames);
		mockMvc.perform(get("/api/env"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.platformDetected").value(true))
				.andExpect(jsonPath("$.count").value(2));
	}

	@Test
	void preservesEmptyAndNullValues() throws Exception {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("PORT", "");
		values.put("HOME", null);
		environmentSource.set(values);
		mockMvc.perform(get("/api/env"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(2))
				.andExpect(jsonPath("$.variables.PORT").value(""))
				.andExpect(jsonPath("$.variables.HOME").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void masksProtectedValuesOverHttp() throws Exception {
		String credentialSentinel = "http-credential-sentinel";
		String instanceKeySentinel = "http-instance-key-sentinel";
		environmentSource.set(Map.of(
				"VCAP_SERVICES", "{\"postgresql\":[{\"credentials\":{\"password\":\""
						+ credentialSentinel + "\"},\"name\":\"db\"}]}",
				"CF_INSTANCE_KEY", instanceKeySentinel));

		var result = mockMvc.perform(get("/api/env"))
				.andExpect(status().isOk())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		JsonNode response = objectMapper.readTree(body);
		assertEquals("****", response.path("variables").path("CF_INSTANCE_KEY").asText());
		JsonNode expectedServices = objectMapper.readTree(
				"{\"postgresql\":[{\"credentials\":\"****\",\"name\":\"db\"}]}");
		JsonNode services = objectMapper.readTree(response.path("variables").path("VCAP_SERVICES").asText());
		assertEquals(expectedServices, services);
		assertEquals(2, response.path("count").asInt());
		assertTrue(response.path("platformDetected").asBoolean());
		assertFalse(body.contains(credentialSentinel));
		assertFalse(body.contains(instanceKeySentinel));
	}

	@Test
	void failsClosedOverHttp() throws Exception {
		String instanceKeySentinel = "empty-instance-key-sentinel";
		Map<String, String> malformedServices = new LinkedHashMap<>();
		malformedServices.put("VCAP_SERVICES", "{malformed");
		malformedServices.put("CF_INSTANCE_KEY", "key");
		malformedServices.put("PORT", "8080");
		Map<String, String> emptyInstanceKey = new LinkedHashMap<>();
		emptyInstanceKey.put("VCAP_SERVICES", "null");
		emptyInstanceKey.put("CF_INSTANCE_KEY", "");
		emptyInstanceKey.put("PORT", "8080");
		Map<String, String> nullInstanceKey = new LinkedHashMap<>();
		nullInstanceKey.put("VCAP_SERVICES", null);
		nullInstanceKey.put("CF_INSTANCE_KEY", null);
		nullInstanceKey.put("PORT", "8080");

		for (Map<String, String> values : java.util.List.of(malformedServices, emptyInstanceKey,
				nullInstanceKey)) {
			environmentSource.set(values);
			var result = mockMvc.perform(get("/api/env")).andExpect(status().isOk()).andReturn();
			String body = result.getResponse().getContentAsString();
			JsonNode response = objectMapper.readTree(body);
			assertEquals("****", response.path("variables").path("VCAP_SERVICES").asText());
			assertEquals("****", response.path("variables").path("CF_INSTANCE_KEY").asText());
			assertEquals(3, response.path("count").asInt());
			assertTrue(response.path("platformDetected").asBoolean());
			assertFalse(body.contains(instanceKeySentinel));
		}
	}

	@Test
	void preservesEmptyServicesObjectOverHttp() throws Exception {
		environmentSource.set(Map.of("VCAP_SERVICES", "{}"));

		var result = mockMvc.perform(get("/api/env")).andExpect(status().isOk()).andReturn();
		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		JsonNode services = objectMapper.readTree(response.path("variables").path("VCAP_SERVICES").asText());

		assertEquals(objectMapper.readTree("{}"), services);
	}

	@Test
	void routeIsDiscoverable() throws Exception {
		mockMvc.perform(get("/api/routes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.path == '/api/env')].path", org.hamcrest.Matchers.hasSize(1)))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.string(containsString("\"path\":\"/api/env\",\"methods\":[\"GET\"]")));
	}

	@TestConfiguration
	static class EnvironmentSourceConfiguration {
		@Bean
		@Primary
		TestEnvironmentSource testEnvironmentSource() {
			return new TestEnvironmentSource();
		}
	}

	static class TestEnvironmentSource implements Supplier<Map<String, String>> {
		private volatile Map<String, String> values = Map.of();

		void set(Map<String, String> values) {
			this.values = new LinkedHashMap<>(values);
		}

		@Override
		public Map<String, String> get() {
			return values;
		}
	}
}
