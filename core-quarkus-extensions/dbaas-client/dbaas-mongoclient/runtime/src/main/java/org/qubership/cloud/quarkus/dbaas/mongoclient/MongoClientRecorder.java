package org.qubership.cloud.quarkus.dbaas.mongoclient;

import io.quarkus.runtime.annotations.Recorder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Recorder
public class MongoClientRecorder {

    public Supplier<AnnotationParsingBean> mongoAnnSupplier(List<String> values, List<String> tenants) {
        log.debug("Creating helpful bean");
        AnnotationParsingBean bean = new AnnotationParsingBean(values, tenants);
        return new Supplier<AnnotationParsingBean>() {
            @Override
            public AnnotationParsingBean get() {
                return bean;
            }
        };
    }

}
