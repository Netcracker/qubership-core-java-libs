package com.netcracker.cloud.routesregistration.common.gateway.route;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RouteRetryManager {

    private final Scheduler rxScheduler;
    private final RoutesRegistrationDelayProvider delayProvider;
    private final PublishSubject<Object> groupedTasksObservable;
    private final AtomicInteger atomicInteger;

    public RouteRetryManager(Scheduler rxScheduler, RoutesRegistrationDelayProvider delayProvider) {
        this.rxScheduler = rxScheduler;
        this.delayProvider = delayProvider;
        this.groupedTasksObservable = PublishSubject.create();
        this.atomicInteger = new AtomicInteger();
    }

    @PreDestroy
    public void disposeResources() {
        rxScheduler.shutdown();
    }

    @NotNull
    protected SingleOnSubscribe<String> getSource(Runnable task) {
        return singleOnSubscribe -> {
            try {
                task.run();
                singleOnSubscribe.onSuccess("Success");
            } catch (Exception e) {
                delayProvider.pauseRegistration();
                log.error("Error during routes posting:", e);
                singleOnSubscribe.onError(new RoutePostingPauseException(e));
            }
        };
    }

    @NotNull
    private SingleObserver<String> getSubscriber() {
        return new SingleObserver<String>() {

            Disposable disposable;

            @Override
            public void onSubscribe(Disposable d) {
                disposable = d;
            }

            /* onError is termination method.
               As we use retry() for SingleObserver, this method is not called. */
            @Override
            public void onError(Throwable t) {
                log.error("Error during task executing: ", t);
                groupedTasksObservable.onError(t);
                dispose();
            }

            @Override
            public void onSuccess(String message) {
                delayProvider.activateRegistration();
                log.debug("Task have been done successfully");
                if (atomicInteger.decrementAndGet() == 0) {
                    groupedTasksObservable.onNext("onNext");
                }
                dispose();
            }

            private void dispose() {
                disposable.dispose();
                disposable = null;
            }
        };
    }

    private class GroupedTasksSubscriber implements Observer<Object> {
        Disposable disposable;
        Iterator<Integer> prioritiesIterator;
        final Map<Integer, List<Runnable>> tasksByPriority;

        public GroupedTasksSubscriber(Map<Integer, List<Runnable>> tasksByPriority) {
            this.tasksByPriority = tasksByPriority;
            this.prioritiesIterator = tasksByPriority.keySet().iterator();
        }

        @Override
        public void onSubscribe(Disposable d) {
            disposable = d;
            if (tasksByPriority == null || tasksByPriority.isEmpty()) {
                onComplete();
                return;
            }
            runNextTasks();
        }

        @Override
        public void onNext(Object ignore) {
            runNextTasks();
        }

        private void runNextTasks() {
            if (prioritiesIterator == null || !prioritiesIterator.hasNext()) {
                onComplete();
                return;
            }
            List<Runnable> tasks = tasksByPriority.get(prioritiesIterator.next());
            atomicInteger.addAndGet(tasks.size());
            tasks.forEach(runnable ->
                    Single.create(getSource(runnable))
                            .retry(throwable -> throwable instanceof RoutePostingPauseException)
                            .subscribeOn(rxScheduler)
                            .subscribe(getSubscriber())
            );
        }

        @Override
        public void onError(Throwable t) {
            log.error("Error during group processing:", t);
            onComplete();
        }

        @Override
        public void onComplete() {
            log.debug("Group of tasks have been done successfully");
            disposable.dispose();
            disposable = null;
        }
    }

    @Deprecated
    public void execute(Runnable runnable) {
        execute(Map.of(0, Collections.singletonList(runnable)));
    }

    public void execute(Map<Integer, List<Runnable>> tasksByPriority) {
        groupedTasksObservable
                .retry()
                .subscribeOn(Schedulers.single())
                .subscribe(new GroupedTasksSubscriber(tasksByPriority));
    }
}
