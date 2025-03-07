package org.qubership.cloud.restlegacy.restclient.configuration;

import org.qubership.cloud.restlegacy.restclient.service.MessageService;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class MessageConfiguration {

    @Bean
    public LocaleResolver localeResolver() {
        final SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.US);
        return slr;
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:locale/common_messages", "classpath:locale/messages");
        messageSource.setCacheSeconds(3600);
        return messageSource;
    }

    @Bean
    public MessageService messageService(MessageSource messageSource) {
        return new MessageService(messageSource);
    }
}