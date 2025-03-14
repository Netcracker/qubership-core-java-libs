package org.qubership.maas.declarative.kafka.quarkus.client.test;


import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

@QuarkusTestResource(TestConfiguration.class)
public abstract class AbstractMaasKafkaTest {

    @RegisterExtension
    static final QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.yml")
            );
}
