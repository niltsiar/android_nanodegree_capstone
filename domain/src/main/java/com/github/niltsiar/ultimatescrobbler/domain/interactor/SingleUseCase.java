package com.github.niltsiar.ultimatescrobbler.domain.interactor;

import io.reactivex.Scheduler;
import io.reactivex.Single;

public abstract class SingleUseCase<T, V> {

    private Scheduler executionScheduler;
    private Scheduler postExecutionScheduler;

    public SingleUseCase(Scheduler executionScheduler, Scheduler postExecutionScheduler) {
        this.executionScheduler = executionScheduler;
        this.postExecutionScheduler = postExecutionScheduler;
    }

    protected abstract Single<T> buildUseCaseObservable(V param);

    public Single<T> execute(V param) {
        Single<T> single = buildUseCaseObservable(param);

        if (null != executionScheduler) {
            single = single.subscribeOn(executionScheduler);
        }

        if (null != postExecutionScheduler) {
            single = single.observeOn(postExecutionScheduler);
        }

        return single;
    }
}
