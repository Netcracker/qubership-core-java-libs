package com.netcracker.cloud.context.propagation.spring.kafka;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaContextPropagationConfiguration {
	/**
	 * Intercept kafka {@link ProducerFactory} bean creation by adding post processor. Prostprocessor wraps kafka Producer
	 * instances with {@link ProducerWithContextPropagation}, that overrides send message method.
	 *
	 * @param beanFactory instance of sp[ring bean factory
	 * @return bean instance
	 */
	@Bean
	public KafkaProducerContextPropagationWrapperInjector kafkaProducerContextPropagationWrapperInjector(BeanFactory beanFactory) {
		return new KafkaProducerContextPropagationWrapperInjector(beanFactory);
	}
}
