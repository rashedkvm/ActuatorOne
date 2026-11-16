package io.spring.actone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class EnvMaskingTests {

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
		assertEquals(expected, EnvController.maskVcapServices(input));
	}

	@Test
	void leavesNonCredentialMembersIntact() {
		String input = "{\"service\":[{\"Credentials\":{\"password\":\"keep\"},"
				+ "\"metadata\":{\"credentials\":\"keep\"},\"name\":\"binding\"}]}";
		assertEquals(input, EnvController.maskVcapServices(input));
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
				"{\"service\":[" + "[".repeat(1100) + "0" + "]".repeat(1100) + "]}");
		for (String invalid : invalidValues) {
			assertEquals("****", EnvController.maskVcapServices(invalid), invalid);
		}
		assertEquals("****", EnvController.maskVcapServices(null));
	}

	@Test
	void neverEchoesInputOnFailure() {
		Logger logger = (Logger) LoggerFactory.getLogger(EnvController.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		String sentinel = "vcap-sensitive-sentinel";
		try {
			assertEquals("****", EnvController.maskVcapServices("malformed " + sentinel));
			assertTrue(appender.list.stream().map(ILoggingEvent::getFormattedMessage)
					.noneMatch(message -> message.contains(sentinel)));
		} finally {
			logger.detachAppender(appender);
			appender.stop();
		}
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
