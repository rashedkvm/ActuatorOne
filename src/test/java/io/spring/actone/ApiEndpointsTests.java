package io.spring.actone;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiEndpointsTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void helloReturnsGreeting() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/hello"))
				.andExpect(status().isOk())
				.andExpect(content().string("Hello World!"));
	}

	@Test
	void missionsReturnsNonEmptyMissionArray() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/missions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$[*].id").isNotEmpty())
				.andExpect(jsonPath("$[*].name").isNotEmpty())
				.andExpect(jsonPath("$[*].crew").isNotEmpty())
				.andExpect(jsonPath("$[*].launchDate").isNotEmpty())
				.andExpect(jsonPath("$[*].status").isNotEmpty())
				.andExpect(jsonPath("$[*].id", hasItem("apollo-11")));
	}

	@Test
	void missionByIdReturnsMissionFields() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/missions/apollo-11"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("apollo-11"))
				.andExpect(jsonPath("$.name").exists())
				.andExpect(jsonPath("$.crew").isArray())
				.andExpect(jsonPath("$.launchDate").exists())
				.andExpect(jsonPath("$.status").exists());
	}

	@Test
	void missingMissionReturnsProblemDetail() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/missions/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Not Found"))
				.andExpect(jsonPath("$.detail").value(containsString("does-not-exist")));
	}

	@Test
	void menuReturnsMenuItems() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/menu"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[*].id").isNotEmpty())
				.andExpect(jsonPath("$[*].name").isNotEmpty())
				.andExpect(jsonPath("$[*].priceUsd").isNotEmpty())
				.andExpect(jsonPath("$[*].category").isNotEmpty())
				.andExpect(jsonPath("$[*].id", hasItem("espresso")));
	}

	@Test
	void ordersReturnsOrdersWithItems() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$[*].id").isNotEmpty())
				.andExpect(jsonPath("$[*].customer").isNotEmpty())
				.andExpect(jsonPath("$[*].items").isNotEmpty())
				.andExpect(jsonPath("$[*].items[0].menuId").isNotEmpty())
				.andExpect(jsonPath("$[*].items[0].quantity").isNotEmpty())
				.andExpect(jsonPath("$[*].totalUsd").isNotEmpty())
				.andExpect(jsonPath("$[*].status").isNotEmpty());
	}

	@Test
	void weatherReturnsWeatherForCities() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/weather"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[*].city").isNotEmpty())
				.andExpect(jsonPath("$[*].station").isNotEmpty())
				.andExpect(jsonPath("$[*].tempC").isNotEmpty())
				.andExpect(jsonPath("$[*].conditions").isNotEmpty())
				.andExpect(jsonPath("$[*].city", hasItem("oslo")));
	}

	@Test
	void weatherByCityReturnsWeatherFields() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/weather/oslo"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.city").value("oslo"))
				.andExpect(jsonPath("$.station").exists())
				.andExpect(jsonPath("$.tempC").exists())
				.andExpect(jsonPath("$.conditions").exists());
	}

	@Test
	void missingWeatherCityReturnsProblemDetail() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/weather/atlantis"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Not Found"))
				.andExpect(jsonPath("$.detail").value(containsString("atlantis")));
	}

	@Test
	void runtimeReturnsJavaAndSystemProperties() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/runtime"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.javaVersion").exists())
				.andExpect(jsonPath("$.javaVendor").exists())
				.andExpect(jsonPath("$.systemProperties").isMap())
				.andExpect(jsonPath("$.systemProperties['java.version']").exists())
				.andExpect(jsonPath("$.systemProperties['os.name']").exists());

		if (System.getenv().keySet().stream().noneMatch(name -> name.equals("VCAP_APPLICATION")
				|| name.startsWith("CF_INSTANCE_"))) {
			mockMvc.perform(MockMvcRequestBuilders.get("/api/runtime"))
					.andExpect(jsonPath("$.cf").doesNotExist());
		}
	}

	@Test
	void timeReturnsParseableUtcInstant() throws Exception {
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/time"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.utc").exists())
				.andReturn();

		Instant.parse(JsonPath.read(result.getResponse().getContentAsString(), "$.utc"));
	}

	@Test
	void routesListsSortedPathsAndGetMethods() throws Exception {
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/routes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andReturn();

		List<String> paths = JsonPath.read(result.getResponse().getContentAsString(), "$[*].path");
		org.junit.jupiter.api.Assertions.assertEquals(paths.stream().sorted().toList(), paths);
		List<Map<String, Object>> routes = JsonPath.read(result.getResponse().getContentAsString(), "$[*]");
		org.junit.jupiter.api.Assertions.assertTrue(routes.stream().anyMatch(route -> route.get("path").equals("/hello")
				&& ((List<?>) route.get("methods")).contains("GET")));
		org.junit.jupiter.api.Assertions.assertTrue(routes.stream().anyMatch(route -> route.get("path").equals("/api/missions/{id}")
				&& ((List<?>) route.get("methods")).contains("GET")));
	}
}
