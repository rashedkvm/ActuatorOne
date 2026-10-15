package io.spring.actone;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
public class MissionsController {

	private static final List<Mission> MISSIONS = List.of(
			new Mission("apollo-11", "Apollo 11", List.of("Neil Armstrong", "Buzz Aldrin", "Michael Collins"), "1969-07-16", "completed"),
			new Mission("artemis-2", "Artemis II", List.of("Reid Wiseman", "Victor Glover", "Christina Koch", "Jeremy Hansen"), "2025-09-01", "planned"));

	@GetMapping
	public List<Mission> missions() {
		return MISSIONS;
	}

	@GetMapping("/{id}")
	public Mission mission(@PathVariable String id) {
		return MISSIONS.stream().filter(mission -> mission.id().equals(id)).findFirst()
				.orElseThrow(() -> new MissingResourceException("Mission not found: " + id));
	}

	record Mission(String id, String name, List<String> crew, String launchDate, String status) {}
}
