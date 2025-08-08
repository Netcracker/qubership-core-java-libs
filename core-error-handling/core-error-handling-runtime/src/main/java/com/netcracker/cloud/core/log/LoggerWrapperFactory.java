package org.qubership.cloud.core.log;

import org.slf4j.Logger;

public class LoggerWrapperFactory {
    public static Logger getLogger(String name) {
        return new LoggerWrapper(name);
    }
}
