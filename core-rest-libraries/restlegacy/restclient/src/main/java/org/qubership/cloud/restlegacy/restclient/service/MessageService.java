package org.qubership.cloud.restlegacy.restclient.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;

public class MessageService {

    private final MessageSource messageSource;

    @Autowired
    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String code, Object... vars) {
        return messageSource.getMessage(code, vars, LocaleContextHolder.getLocale());
    }

    public String getMessageCode(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    public String getMessage(MessageSourceResolvable msr) {
        return messageSource.getMessage(msr, LocaleContextHolder.getLocale());
    }

}