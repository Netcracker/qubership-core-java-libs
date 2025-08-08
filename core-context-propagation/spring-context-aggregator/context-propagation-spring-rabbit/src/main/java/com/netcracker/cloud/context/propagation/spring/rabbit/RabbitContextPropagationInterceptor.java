package com.netcracker.cloud.context.propagation.spring.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

class RabbitContextPropagationInterceptor implements MessagePostProcessor {
	private static final Logger log = LoggerFactory.getLogger(RabbitContextPropagationInterceptor.class);

	@Override
	public Message postProcessMessage(Message message) throws AmqpException {
		RabbitContextPropagation.dumpContext(message.getMessageProperties()::setHeader);
		log.debug("Augmented message with context data: {}", message);
		return message;
	}
}
