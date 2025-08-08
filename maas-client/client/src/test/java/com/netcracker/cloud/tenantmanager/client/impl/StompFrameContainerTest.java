package org.qubership.cloud.tenantmanager.client.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StompFrameContainerTest {
	@Test
	public void testParseMessageWithBody() {
		String msg = "a[\"CONNECTED\\nheart-beat:1000,1000\\nsession:1f7a16b1-05a9-426b-8dd3-5ee1e6c4125a\\n\\n\\u0000\"]";
		StompFrameContainer container = StompFrameContainer.deserialize(msg);

		assertEquals("a", container.getOperation());
		assertNotNull(container.getFrame());

		StompFrame frame = container.getFrame();
		assertEquals(StompCommands.CONNECTED, frame.getCommand());
		assertEquals("1000,1000", frame.getHeader("heart-beat"));
		assertEquals("1f7a16b1-05a9-426b-8dd3-5ee1e6c4125a", frame.getHeader("session"));

		assertEquals("", frame.getBody());
	}

	@Test
	public void testParseEmptyMessage() {
		String msg = "h";
		StompFrameContainer container = StompFrameContainer.deserialize(msg);

		assertEquals("h", container.getOperation());
		assertNull(container.getFrame());
	}


	@Test
	public void testParseIncorrectMessageFormat() {
		assertThrows(RuntimeException.class, () -> StompFrameContainer.deserialize("foo"));
	}
}