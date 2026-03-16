package com.netcracker.cloud.quarkus.dbaas.mongoclient.deployment;

import com.netcracker.cloud.quarkus.dbaas.mongoclient.AnnotationParsingBean;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.MongoClientAggregator;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.MongoClientRecorder;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.annotations.ServiceDb;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.annotations.TenantDb;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.config.CustomNCMongoClients;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.config.MongoClientConfiguration;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import jakarta.inject.Singleton;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

public class MongoExtensionProcessor {
    private static final String FEATURE = "dbaas-mongo-client";
    private static final DotName DOTNAME_SERVICE = DotName.createSimple(ServiceDb.class.getName());
    private static final DotName DOTNAME_TENANT = DotName.createSimple(TenantDb.class.getName());

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(MongoClientConfiguration.class)
                .addBeanClass(MongoClientAggregator.class)
                .addBeanClass(CustomNCMongoClients.class)
                .build();
    }

    @BuildStep
    @Record(STATIC_INIT)
    void parsingMongoAnnotations(CombinedIndexBuildItem combinedIndexBuildItem,
                         MongoClientRecorder recorder,
                         BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer
    ) {
        IndexView indexView = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> serviceAnnotations = indexView.getAnnotations(DOTNAME_SERVICE);
        List<String> serviceDbs = new ArrayList<>();

        for (AnnotationInstance ann : serviceAnnotations) {
            ClassInfo beanClassInfo = ann.target().asClass();
            serviceDbs.add(beanClassInfo.simpleName());
        }

        Collection<AnnotationInstance> tenantAnnotations = indexView.getAnnotations(DOTNAME_TENANT);
        List<String> tenantDbs = new ArrayList<>();
        for (AnnotationInstance ann : tenantAnnotations) {
            ClassInfo beanClassInfo = ann.target().asClass();
            tenantDbs.add(beanClassInfo.name().toString());
        }

        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(AnnotationParsingBean.class)
                .scope(Singleton.class)
                .supplier(recorder.mongoAnnSupplier(serviceDbs, tenantDbs))
                .done());
    }
}
