package io.spring.actone;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@RestController
@RequestMapping("/api/routes")
public class RoutesController {

	private final RequestMappingHandlerMapping mappings;

	public RoutesController(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings) {
		this.mappings = mappings;
	}

	@GetMapping
	public List<Route> routes() {
		Map<String, Set<String>> routes = new TreeMap<>();
		for (RequestMappingInfo mapping : mappings.getHandlerMethods().keySet()) {
			if (mapping.getPathPatternsCondition() == null) {
				continue;
			}
			Set<String> methods = mapping.getMethodsCondition().getMethods().isEmpty()
					? Set.of("*")
					: mapping.getMethodsCondition().getMethods().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
			for (var pattern : mapping.getPathPatternsCondition().getPatterns()) {
				String path = pattern.getPatternString();
				if (path.equals("/hello") || path.equals("/api") || path.startsWith("/api/")) {
					routes.computeIfAbsent(path, ignored -> new TreeSet<>()).addAll(methods);
				}
			}
		}
		return routes.entrySet().stream().map(entry -> new Route(entry.getKey(), List.copyOf(entry.getValue()))).toList();
	}

	record Route(String path, List<String> methods) {}
}
