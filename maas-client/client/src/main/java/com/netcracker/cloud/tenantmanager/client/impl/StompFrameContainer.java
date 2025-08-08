package com.netcracker.cloud.tenantmanager.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ToString
@AllArgsConstructor
public class StompFrameContainer {
	private static final ObjectMapper mapper = new ObjectMapper();

	@Getter private final String operation;
	@Getter private final StompFrame frame;

	private static final Pattern pattern = Pattern.compile("([a-z])(\\[(\".+\")\\])?");

	public static StompFrameContainer deserialize(String buf) throws UnsupportedOperationException {
		if (buf.length() == 0)
			throw new UnsupportedOperationException("Empty message received");

		Matcher matcher = pattern.matcher(buf);
		if (!matcher.matches()) {
			throw new UnsupportedOperationException("Can't parse frame container structure Empty message received: `" + buf + "'");
		}

		StompFrame frame = null;
		if (matcher.group(3) != null) {
			try {
				frame = StompFrame.deserialize(mapper.readValue(matcher.group(3), String.class));
			} catch (JsonProcessingException e) {
				throw new UnsupportedOperationException("Error extract stomp frame", e);
			}
		}
		return new StompFrameContainer(matcher.group(1), frame);
	}
}
