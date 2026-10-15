package io.spring.actone;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

	private static final List<Weather> WEATHER = List.of(
			new Weather("oslo", "OSL-01", 8.4, "cloudy"),
			new Weather("helsinki", "HEL-02", 7.1, "light rain"));

	@GetMapping
	public List<Weather> weather() {
		return WEATHER;
	}

	@GetMapping("/{city}")
	public Weather weatherForCity(@PathVariable String city) {
		return WEATHER.stream().filter(weather -> weather.city().equals(city)).findFirst()
				.orElseThrow(() -> new MissingResourceException("Weather not found for city: " + city));
	}

	record Weather(String city, String station, double tempC, String conditions) {}
}
