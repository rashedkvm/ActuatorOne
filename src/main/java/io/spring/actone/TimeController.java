package io.spring.actone;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/time")
public class TimeController {

	@GetMapping
	public TimeResponse time() {
		return new TimeResponse(Instant.now().toString());
	}

	record TimeResponse(String utc) {}
}
