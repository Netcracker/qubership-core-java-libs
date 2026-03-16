package com.netcracker.cloud.context.propagation.spring.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.RecordInterceptor;

import java.lang.reflect.Field;

public class KafkaProducerContextPropagationWrapperInjector implements BeanPostProcessor {
	private static final Logger log = LoggerFactory.getLogger(KafkaProducerContextPropagationWrapperInjector.class);
	private BeanFactory beanFactory;

	public KafkaProducerContextPropagationWrapperInjector(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ProducerFactory) {
			log.info("Add context propagation processor to: {}", bean);
			ProducerFactory factory = (ProducerFactory) bean;
			factory.addPostProcessor(o -> new ProducerWithContextPropagation(beanName, (Producer)o));
		} else if (bean instanceof AbstractKafkaListenerContainerFactory) {
			log.info("Add context propagation processor to: {}", bean);
			AbstractKafkaListenerContainerFactory factory = (AbstractKafkaListenerContainerFactory) bean;
			RecordInterceptor original = getHiddenFieldValue(AbstractKafkaListenerContainerFactory.class, factory, RecordInterceptor.class, "recordInterceptor");
			factory.setRecordInterceptor(new KafkaContextRestoreInterceptor(original));
		}
		return bean;
	}

	static <T> T getHiddenFieldValue(Class<?> refClass, Object ref, Class<T> fieldType, String fieldName) {
		try {
			Field field = refClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			return fieldType.cast(field.get(ref));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Error getting data from: field: inst=" + ref + ", fieldType=" + fieldType + ", field=" + fieldName, e);
		}
	}
}
