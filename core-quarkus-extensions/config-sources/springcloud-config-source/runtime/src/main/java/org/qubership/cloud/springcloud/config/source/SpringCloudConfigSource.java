package org.qubership.cloud.springcloud.config.source;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Map;
import java.util.Set;

@Slf4j
public class SpringCloudConfigSource implements ConfigSource {

    private static final long serialVersionUID = 3127679154588598693L;
    private int ordinal;

    public SpringCloudConfigSource(int i) {
        ordinal = i;
        log.debug("SpringCloudConfigSource init finished");
    }

    public SpringCloudConfigSource() {
        log.debug("SpringCloudConfigSource init finished");
    }

    @Override
    public Map<String, String> getProperties() {

        return getData();
    }

    @Override
    public Set<String> getPropertyNames() {
        Map data = getData();
        return data.keySet();
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String getValue(String s) {
        String value = getData().get(s);
        return value;
    }

    @Override
    public String getName() {
        return "SpringCloudConfig";
    }


    private Map<String, String> getData() {
        return PropertyManager.getInstance().getProperties();
    }

}
