package io.spring.actone;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes selected platform environment variables when explicitly enabled.
 *
 * <h2>Residual exposure</h2>
 * <ol>
 * <li>Applies only when the feature is enabled (`ACTONE_ENV_ENABLED=true`). Anyone who can reach `/api/env` can then read it; the manifests request routes (`random-route: true`) and the app has no authentication.</li>
 * <li>Only exact binding-level `credentials` members of `VCAP_SERVICES` and the exact name `CF_INSTANCE_KEY` are masked. Everything else matching stays raw: service metadata, `syslog_drain_url` (may carry authentication material), `volume_mounts`, `VCAP_APPLICATION` (app/org/space/route metadata), `CF_INSTANCE_ADDR/IP/INTERNAL_IP/PORTS`, `CF_INSTANCE_CERT`, user-set values under matching names (e.g. `Credentials`, `metadata.credentials`, a user-defined `CF_PASSWORD`).</li>
 * <li>`/api/runtime` is unchanged and already returns `VCAP_APPLICATION` and every `CF_INSTANCE_*` value raw, including `CF_INSTANCE_KEY` when present. This feature does not protect it.</li>
 * <li>Acceptable only on a trusted/restricted route audience with no bound services holding real secrets. The guarantee is selected-field masking, not credential-free output.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/env")
@Conditional(EnvEnabledCondition.class)
public class EnvController {

	private static final int MAX_JSON_DEPTH = 100;
	private static final ObjectMapper VCAP_SERVICES_MAPPER = new ObjectMapper()
			.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
			.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
	private static final String MASKED_VALUE = "****";
	private static final String[] ALLOWED_NAMES = {
			"PORT", "MEMORY_LIMIT", "INSTANCE_INDEX", "INSTANCE_GUID", "LANG", "TMPDIR", "USER", "HOME"
	};

	private final Supplier<Map<String, String>> environmentSource;

	public EnvController(Supplier<Map<String, String>> environmentSource) {
		this.environmentSource = environmentSource;
	}

	@GetMapping
	public EnvInfo env() {
		Map<String, String> source = environmentSource.get();
		TreeMap<String, String> variables = new TreeMap<>();
		boolean platformDetected = false;
		for (Map.Entry<String, String> entry : source.entrySet()) {
			String name = entry.getKey();
			if (name.startsWith("VCAP_") || name.startsWith("CF_")) {
				platformDetected = true;
			}
			if (isAllowed(name)) {
				String value = entry.getValue();
				if ("VCAP_SERVICES".equals(name)) {
					value = maskVcapServices(value);
				} else {
					value = maskInstanceKey(name, value);
				}
				variables.put(name, value);
			}
		}
		return new EnvInfo(platformDetected, variables.size(), variables);
	}

	static String maskVcapServices(String value) {
		if (value == null || value.startsWith("\uFEFF") || !hasSafeJsonDepth(value)) {
			return MASKED_VALUE;
		}
		try {
			JsonNode root = VCAP_SERVICES_MAPPER.readTree(value);
			if (root == null || !root.isObject()) {
				return MASKED_VALUE;
			}
			var labels = root.fields();
			while (labels.hasNext()) {
				JsonNode bindings = labels.next().getValue();
				if (!bindings.isArray()) {
					return MASKED_VALUE;
				}
				for (JsonNode binding : bindings) {
					if (!binding.isObject()) {
						return MASKED_VALUE;
					}
					ObjectNode object = (ObjectNode) binding;
					if (object.has("credentials")) {
						object.put("credentials", MASKED_VALUE);
					}
				}
			}
			return VCAP_SERVICES_MAPPER.writeValueAsString(root);
		} catch (Exception | StackOverflowError exception) {
			return MASKED_VALUE;
		}
	}

	static String maskInstanceKey(String name, String value) {
		return "CF_INSTANCE_KEY".equals(name) ? MASKED_VALUE : value;
	}

	public record EnvInfo(boolean platformDetected, int count, Map<String, String> variables) {}

	private static boolean isAllowed(String name) {
		if (name.startsWith("VCAP_") || name.startsWith("CF_")) {
			return true;
		}
		for (String allowedName : ALLOWED_NAMES) {
			if (allowedName.equals(name)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasSafeJsonDepth(String value) {
		int depth = 0;
		boolean inString = false;
		boolean escaped = false;
		for (int index = 0; index < value.length(); index++) {
			char current = value.charAt(index);
			if (inString) {
				if (escaped) {
					escaped = false;
				} else if (current == '\\') {
					escaped = true;
				} else if (current == '"') {
					inString = false;
				}
			} else if (current == '"') {
				inString = true;
			} else if (current == '{' || current == '[') {
				if (++depth > MAX_JSON_DEPTH) {
					return false;
				}
			} else if (current == '}' || current == ']') {
				depth--;
			}
		}
		return true;
	}

}

class EnvEnabledCondition implements Condition {
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return "true".equals(context.getEnvironment().getProperty("actone.env.enabled"));
	}
}
