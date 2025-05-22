package org.qubership.cloud.routesregistration.common.gateway.route;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;


@Slf4j
public class RouteRetryManagerTest {

    @Test
    public void roteRetryManagerTest_ErrorMessageIsWrittenInLogs() throws InterruptedException, IllegalAccessException, NoSuchFieldException {
        Scheduler scheduler = Schedulers.newThread();
        try {
            LogCaptor logCaptor = LogCaptor.forClass(RouteRetryManager.class);
            AtomicInteger run2Cnt = new AtomicInteger(0);
            RouteRetryManager routeRetryManager = new RouteRetryManager(scheduler, new RoutesRegistrationDelayProvider());
            routeRetryManager.execute(
                    Map.of(1,
                            List.of(
                                    () -> {
                                        // success
                                    },
                                    () -> {
                                        run2Cnt.incrementAndGet();
                                        throw new RuntimeException("This is RuntimeException from runnable task");
                                    },
                                    () -> {
                                        throw new Error("This is Error from runnable task");
                                    }
                            )
                    )
            );

            await().atMost(60, TimeUnit.SECONDS).until(() -> run2Cnt.get() > 2);
            await().atMost(60, TimeUnit.SECONDS).until(() ->
                    logCaptor.getLogEvents().stream()
                            .map(LogEvent::getThrowable)
                            .anyMatch(throwable -> throwable.isPresent() && throwable.get() instanceof Error
                                    && throwable.get().getMessage().contains("This is Error from runnable task"))
            );
        } finally {
            scheduler.shutdown();
        }
    }
}