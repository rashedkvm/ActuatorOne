package io.spring.actone;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime")
public class RuntimeController {

	private static final String[] PROPERTY_NAMES = {
			"java.version", "java.vendor", "java.home", "java.vm.name", "os.name", "os.arch",
			"user.dir", "file.encoding", "java.io.tmpdir"};

	@GetMapping
	public RuntimeInfo runtime() {
		Map<String, String> properties = new LinkedHashMap<>();
		for (String name : PROPERTY_NAMES) {
			properties.put(name, System.getProperty(name));
		}
		Map<String, String> cf = new TreeMap<>();
		System.getenv().forEach((name, value) -> {
			if (name.equals("VCAP_APPLICATION") || name.startsWith("CF_INSTANCE_")) {
				cf.put(name, value);
			}
		});
		return new RuntimeInfo(System.getProperty("java.version"), System.getProperty("java.vendor"), properties,
				ManagementFactory.getRuntimeMXBean().getInputArguments(), cf.isEmpty() ? null : cf);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record RuntimeInfo(String javaVersion, String javaVendor, Map<String, String> systemProperties,
			java.util.List<String> jvmArguments, Map<String, String> cf) {}
}
