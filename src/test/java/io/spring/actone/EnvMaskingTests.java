package io.spring.actone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;

class EnvMaskingTests {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void masksBindingCredentials() throws Exception {
		String input = "{\"first\":["
				+ "{\"credentials\":{\"password\":\"secret\"},\"name\":\"one\"},"
				+ "{\"credentials\":[1,2]},"
				+ "{\"credentials\":\"secret\"},"
				+ "{\"credentials\":\"\"},"
				+ "{\"credentials\":7},"
				+ "{\"credentials\":true},"
				+ "{\"credentials\":null},"
				+ "{\"cred\\u0065ntials\":\"secret\"},"
				+ "{}],\"second\":[{\"credentials\":{\"token\":\"secret\"}}]}";
		String expected = "{\"first\":["
				+ "{\"credentials\":\"****\",\"name\":\"one\"},"
				+ "{\"credentials\":\"****\"},"
				+ "{\"credentials\":\"****\"},"
				+ "{\"credentials\":\"****\"},"
				+ "{\"credentials\":\"****\"},"
				+ "{\"credentials\":\"****\"},"
				+ "{\"credentials\":\"****\"},"
				+ "{\"credentials\":\"****\"},"
				+ "{}],\"second\":[{\"credentials\":\"****\"}]}";
		assertEquals(readTree(expected), readTree(EnvController.maskVcapServices(input)));
	}

	@Test
	void leavesNonCredentialMembersIntact() throws Exception {
		String input = "{\"service\":[{\"Credentials\":{\"password\":\"keep\"},"
				+ "\"metadata\":{\"credentials\":\"keep\"},\"name\":\"binding\"}]}";
		assertEquals(readTree(input), readTree(EnvController.maskVcapServices(input)));
	}

	@Test
	void failsClosedOnInvalidVcapServices() {
		List<String> invalidValues = List.of(
				"{", "", "   ", "\uFEFF{\"service\":[]}",
				"{\"service\":[]} trailing", "{\"service\":[]} {}",
				"{\"service\":[],\"service\":[]}",
				"{\"service\":[],\"serv\\u0069ce\":[]}",
				"[]", "null", "\"text\"", "12",
				"{\"service\":{}}", "{\"service\":[1]}",
				"{\"valid\":[{\"credentials\":\"secret\"}],\"invalid\":[1]}",
				"{\"service\":[{\"metadata\":" + "[".repeat(1100) + "0" + "]".repeat(1100) + "}]}");
		for (String invalid : invalidValues) {
			assertEquals("****", EnvController.maskVcapServices(invalid), invalid);
		}
		assertEquals("****", EnvController.maskVcapServices(null));
	}

	@Test
	void neverEchoesInputOnFailure() {
		Logger logger = (Logger) LoggerFactory.getLogger(EnvController.class);
		Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		ListAppender<ILoggingEvent> controllerAppender = new ListAppender<>();
		ListAppender<ILoggingEvent> rootAppender = new ListAppender<>();
		controllerAppender.start();
		rootAppender.start();
		logger.addAppender(controllerAppender);
		rootLogger.addAppender(rootAppender);
		String sentinel = "vcap-sensitive-sentinel";
		try {
			assertEquals("****", EnvController.maskVcapServices(
					"{\"service\":[{\"credentials\":\"" + sentinel));
			List<ILoggingEvent> events = new java.util.ArrayList<>(rootAppender.list);
			events.addAll(controllerAppender.list);
			assertTrue(events.stream().map(EnvMaskingTests::renderEvent)
					.noneMatch(rendering -> rendering.contains(sentinel)));
		} finally {
			logger.detachAppender(controllerAppender);
			rootLogger.detachAppender(rootAppender);
			controllerAppender.stop();
			rootAppender.stop();
		}
	}

	private JsonNode readTree(String value) throws Exception {
		return objectMapper.readTree(value);
	}

	private static String renderEvent(ILoggingEvent event) {
		String throwable = event.getThrowableProxy() == null ? ""
				: ThrowableProxyUtil.asString(event.getThrowableProxy());
		return event.getFormattedMessage() + "\n" + throwable;
	}

	@Test
	void masksInstanceKeyOnly() {
		assertEquals("****", EnvController.maskInstanceKey("CF_INSTANCE_KEY", "private-key"));
		assertEquals("****", EnvController.maskInstanceKey("CF_INSTANCE_KEY", ""));
		assertEquals("****", EnvController.maskInstanceKey("CF_INSTANCE_KEY", null));
		assertEquals("certificate", EnvController.maskInstanceKey("CF_INSTANCE_CERT", "certificate"));
		assertEquals("other", EnvController.maskInstanceKey("CF_INSTANCE_KEY_EXTRA", "other"));
	}
}
