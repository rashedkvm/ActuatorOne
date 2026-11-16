package io.spring.actone;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/env")
@ConditionalOnProperty(name = "actone.env.enabled", havingValue = "true")
public class EnvController {

	private final Supplier<Map<String, String>> environmentSource;

	public EnvController(Supplier<Map<String, String>> environmentSource) {
		this.environmentSource = environmentSource;
	}

	@GetMapping
	public EnvInfo env() {
		return new EnvInfo(false, 0, Map.of());
	}

	static String maskVcapServices(String value) {
		return value;
	}

	static String maskInstanceKey(String name, String value) {
		return value;
	}

	public record EnvInfo(boolean platformDetected, int count, Map<String, String> variables) {}
}
