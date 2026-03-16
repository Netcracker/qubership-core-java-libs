package com.netcracker.cloud.core.log;

import org.slf4j.Logger;

public class LoggerWrapperFactory {
    private LoggerWrapperFactory() {}
    public static Logger getLogger(String name) {
        return new LoggerWrapper(name);
    }
}
