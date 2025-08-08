package com.netcracker.cloud.maas.client.api.rabbit;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class VHostTest {
	@Test
	public void testApi() {
		VHost vhost = new VHost();
		vhost.setEncodedPassword("plain:abc");
		assertEquals("plain:abc", vhost.getEncodedPassword());
		assertEquals("abc", vhost.getPassword());
		vhost.setCnn("http://abc.com/api/v1/");
		assertEquals(URI.create("http://abc.com/api/v1/"), vhost.getUri());
	}
}
