package org.qubership.cloud.tenantmanager.client.impl;

import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Data
class StompFrame {
	private final StompCommands command;
	private final Map<String, String> headers;
	private final String body;

	public StompFrame(StompCommands command) {
		this(command, Collections.emptyMap(), "");
	}

	public StompFrame(StompCommands command, Map<String, String> headers) {
		this(command, headers, "");
	}

	public StompFrame(StompCommands command, Map<String, String> headers, String body) {
		this.command = command;
		this.headers = headers;
		this.body = body;
	}

	public static Map<String, String> headers(String... keyValuePairs) {
		if (keyValuePairs.length % 2 == 1) {
			throw new IllegalArgumentException("Not even number of input arguments");
		}

		Map<String, String> map = new HashMap<>();
		for(int i=0; i<keyValuePairs.length; i+=2) {
			map.put(keyValuePairs[i], keyValuePairs[i + 1]);
		}
		return map;
	}

	String serialize() {
		StringBuilder buf = new StringBuilder("[\"");
		buf.append(command.toString()).append("\\n");
		for (Map.Entry<String, String> header : headers.entrySet()) {
			buf.append(header.getKey())
					.append(":").
					append(header.getValue())
					.append("\\n");
		}
		buf.append("\\n");
		buf.append(body);
		buf.append("\\u0000\"]");
		return buf.toString();
	}

	private static Pattern partsSplitter = Pattern.compile("\r?\n\r?\n");
	private static Pattern preambleSplitter = Pattern.compile("\r?\n");
	static StompFrame deserialize(String buf) {
		// separate command+headers and body parts
		String[] parts = partsSplitter.split(buf);
		String[] preamble = preambleSplitter.split(parts[0]);

		// parse headers
		Map<String, String> headers = new HashMap<>();
		for(int i=1; i<preamble.length; i++) {
			String[] headerParts = preamble[i].split(":");
			headers.put(headerParts[0].trim(), headerParts[1].trim());
		}

		return new StompFrame(
					StompCommands.valueOf(preamble[0]),
					headers,
					parts[1].trim()
				);
	}

	@Override
	public String toString() {
		return serialize();
	}

	public String getHeader(String name) {
		return headers.get(name);
	}
}
