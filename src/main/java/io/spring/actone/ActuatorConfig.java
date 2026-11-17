package io.spring.actone;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

	@Bean
	InMemoryHttpExchangeRepository httpExchangeRepository() {
		return new InMemoryHttpExchangeRepository();
	}

	@Bean
	InMemoryAuditEventRepository auditEventRepository() {
		return new InMemoryAuditEventRepository();
	}

	@Bean
	Supplier<Map<String, String>> environmentSource() {
		return System::getenv;
	}
}
