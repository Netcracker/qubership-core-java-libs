package org.qubership.cloud.configserver.common.configuration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.cloud.config.client.ConfigServerBootstrapper;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BootstrapStateEventListeners {

    static BootstrapContext bootstrapContext = null;

    public static final class ContextRefreshedEventListener extends RefreshAmountHolder implements ApplicationListener<ContextRefreshedEvent> {
        public ContextRefreshedEventListener(int maxAmountOfRefreshes) {
            super(maxAmountOfRefreshes);
        }
        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            super.assertNumberOfRefreshes();
            assertInterceptorRegisteredInBootstrapContext(bootstrapContext);
        }
    }

    public static final class EnvironmentChangedAfterRefreshEventListener extends RefreshAmountHolder implements ApplicationListener<EnvironmentChangeEvent> {
        public EnvironmentChangedAfterRefreshEventListener(int maxAmountOfRefreshes) {
            super(maxAmountOfRefreshes);
        }
        @Override
        public void onApplicationEvent(EnvironmentChangeEvent event) {
            super.assertNumberOfRefreshes();
            Object eventCreator = event.getSource(); // Raised by ContextRefresher but ContextRefresher set source to ConfigurableApplicationContext
            Assert.assertTrue(eventCreator instanceof ConfigurableApplicationContext);
            ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) eventCreator;
            // In spring-cloud-context:3.0.3 they use RefreshBootstrapRegistryInitializer to put old BootstrapContext on close event
            // https://github.com/spring-cloud/spring-cloud-commons/commit/9efc70b69497113d8964539b7dc6c98314bc83fb
            try {
                ConfigurableBootstrapContext bootstrapContextAfterRefresh = applicationContext.getBean("bootstrapContext",
                        ConfigurableBootstrapContext.class);
                bootstrapContext = bootstrapContextAfterRefresh;
            } catch (NoSuchBeanDefinitionException e) {
                log.error(e.toString());
                Assert.fail("Cannot find old bootstrapContext in app context, so after RefreshEvent copy of" +
                        " bootstrapContext" + " will be used without our LoaderInterceptor");
            }
            assertInterceptorRegisteredInBootstrapContext(bootstrapContext);
        }
    }

    // Used at bootstrap time so it mentioned in spring.factories
    public static final class ApplicationStartingEventListener implements ApplicationListener<ApplicationStartingEvent> {
        @Override
        public void onApplicationEvent(ApplicationStartingEvent event) {
            BootstrapContext context = event.getBootstrapContext();

            assertInterceptorRegisteredInBootstrapContext(context);
            bootstrapContext = context; // Save it to use in refresh event listeners
        }
    }

    static void assertInterceptorRegisteredInBootstrapContext(BootstrapContext context) {
        Assert.assertNotNull(context);
        Assert.assertTrue(context.isRegistered(ConfigServerBootstrapper.LoaderInterceptor.class));

        ConfigServerBootstrapper.LoaderInterceptor interceptor = context.get(ConfigServerBootstrapper.LoaderInterceptor.class);
        Assert.assertTrue(interceptor instanceof CustomConfigServerBootstrapper.CustomLoaderInterceptor);
    }

    @AllArgsConstructor
    public static abstract class RefreshAmountHolder {
        protected final AtomicInteger numberOfRefreshes = new AtomicInteger(0);
        protected final int maxAmountOfRefreshes;

        public void assertNumberOfRefreshes() {
            numberOfRefreshes.incrementAndGet();
            Assert.assertTrue(getNumberOfRefreshes() <= maxAmountOfRefreshes);
        }

        public int getNumberOfRefreshes() {
            return numberOfRefreshes.get();
        }
    }
}
