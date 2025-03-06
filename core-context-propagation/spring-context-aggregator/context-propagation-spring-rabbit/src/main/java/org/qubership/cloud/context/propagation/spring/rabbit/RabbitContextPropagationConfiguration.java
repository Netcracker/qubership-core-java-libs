package org.qubership.cloud.context.propagation.spring.rabbit;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitContextPropagationConfiguration {
	@Bean
	public RabbitContextPropagationInterceptorsInjector rabbitContextPropagationInterceptorsInjector(BeanFactory beanFactory) {
		return new RabbitContextPropagationInterceptorsInjector(beanFactory);
	}
}
