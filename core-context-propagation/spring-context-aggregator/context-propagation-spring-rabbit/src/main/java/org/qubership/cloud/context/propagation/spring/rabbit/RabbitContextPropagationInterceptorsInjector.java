package org.qubership.cloud.context.propagation.spring.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

public class RabbitContextPropagationInterceptorsInjector implements BeanPostProcessor {
	private static final Logger log = LoggerFactory.getLogger(RabbitContextPropagationInterceptorsInjector.class);
	private final BeanFactory beanFactory;
	private final MessagePostProcessor propagationInterceptor;
	private final MessagePostProcessor restoreInterceptor;

	public RabbitContextPropagationInterceptorsInjector(BeanFactory beanFactory) {
		this(beanFactory, new RabbitContextPropagationInterceptor(), new RabbitContextRestoreInterceptor());
	}

	public RabbitContextPropagationInterceptorsInjector(BeanFactory beanFactory, MessagePostProcessor propagationInterceptor, MessagePostProcessor restoreInterceptor) {
		this.beanFactory = beanFactory;
		this.propagationInterceptor = propagationInterceptor;
		this.restoreInterceptor = restoreInterceptor;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RabbitTemplate) {
			log.info("Add context propagation interceptor to bean: name={}, instance={}", beanName, bean);
			bean = augmentRabbitTemplate((RabbitTemplate) bean);
		} else if (bean instanceof AbstractRabbitListenerContainerFactory) {
			log.info("Add context restore interceptor to bean: name={}, instance={}", beanName, bean);
			bean = augmentListenerFactory((AbstractRabbitListenerContainerFactory) bean);
		}
		return bean;
	}

	protected RabbitTemplate augmentRabbitTemplate(RabbitTemplate template) {
		Collection<MessagePostProcessor> original = getHiddenFieldValue(RabbitTemplate.class, template, Collection.class, "beforePublishPostProcessors");
		MessagePostProcessor[] change;
		if (original != null) {
			change = new MessagePostProcessor[original.size() + 1];
			original.toArray(change);
			change[original.size()] = propagationInterceptor;
		} else {
			change = new MessagePostProcessor[]{propagationInterceptor};
		}
		template.setBeforePublishPostProcessors(change);
		return template;
	}

	protected AbstractRabbitListenerContainerFactory augmentListenerFactory(AbstractRabbitListenerContainerFactory bean) {
		MessagePostProcessor[] interceptors = getHiddenFieldValue(AbstractRabbitListenerContainerFactory.class, bean, MessagePostProcessor[].class, "afterReceivePostProcessors");
		if (interceptors != null) {
			interceptors = Arrays.copyOf(interceptors, interceptors.length + 1);
			interceptors[interceptors.length - 1] = restoreInterceptor;
		} else {
			interceptors = new MessagePostProcessor[]{restoreInterceptor};
		}
		bean.setAfterReceivePostProcessors(interceptors);
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
