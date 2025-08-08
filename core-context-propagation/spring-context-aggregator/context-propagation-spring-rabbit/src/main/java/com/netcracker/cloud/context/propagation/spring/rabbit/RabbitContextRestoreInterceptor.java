package com.netcracker.cloud.context.propagation.spring.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.core.annotation.Order;

@Order(-10)
class RabbitContextRestoreInterceptor implements MessagePostProcessor {
	private static final Logger log = LoggerFactory.getLogger(RabbitContextPropagationInterceptor.class);
	@Override
	public Message postProcessMessage(Message message) throws AmqpException {
		log.debug("Restore context from message: {}", message);
		RabbitContextPropagation.restoreContext(message.getMessageProperties().getHeaders());
		return message;
	}
}
