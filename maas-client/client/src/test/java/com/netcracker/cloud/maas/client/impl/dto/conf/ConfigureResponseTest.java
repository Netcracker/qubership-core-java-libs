package com.netcracker.cloud.maas.client.impl.dto.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NoArgsConstructor;

class ConfigureResponseTest {
	@Test
	public void testSerialzation() throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		SampleResponse sample = new SampleResponse();
		SampleConfig req = new SampleConfig();
		req.setApiVersion("v1");
		req.setKind("topic");
		req.setPragma(Collections.singletonMap("on-topic-exists", "merge"));
		req.setSpec("some data");
		sample.setRequest(req);
		SampleResult resp = new SampleResult();
		resp.setStatus("ok");
		resp.setData("some response data");
		sample.setResult(resp);

		String json = om.writeValueAsString(sample);
		System.out.println(json);
		assertEquals(
				"{\"request\":{\"apiVersion\":\"v1\",\"kind\":\"topic\",\"pragma\":{\"on-topic-exists\":\"merge\"},\"spec\":\"some data\"},\"result\":{\"status\":\"ok\",\"error\":null,\"data\":\"some response data\"}}",
				json);

		assertEquals(sample, om.readValue(json, SampleResponse.class));
	}


}

@NoArgsConstructor
class SampleConfig extends ConfigResource<String> {}
@NoArgsConstructor
class SampleResponse extends ConfigureResponse<SampleConfig, String> {}
@NoArgsConstructor
class SampleResult extends ConfigureResult<String> {}
