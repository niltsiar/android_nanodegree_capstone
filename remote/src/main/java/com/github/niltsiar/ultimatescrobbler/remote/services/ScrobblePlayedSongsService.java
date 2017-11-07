package com.github.niltsiar.ultimatescrobbler.remote.services;

import android.os.Bundle;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.github.niltsiar.ultimatescrobbler.remote.model.ScrobbledSongModel;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import timber.log.Timber;

public class ScrobblePlayedSongsService extends ScrobblerJobService {

    @Override
    public boolean onStartJob(JobParameters job) {
        Map<String, String> params = bundleToMap(job.getExtras());

        DisposableSingleObserver<List<ScrobbledSongModel>> observer = new DisposableSingleObserver<List<ScrobbledSongModel>>() {
            @Override
            public void onSuccess(List<ScrobbledSongModel> scrobbledSongModels) {
                for (ScrobbledSongModel scrobbledSong : scrobbledSongModels) {
                    Timber.i(scrobbledSong.toString());
                }
                finishJob(job, false);
            }

            @Override
            public void onError(Throwable e) {

            }
        };

        Disposable disposable = scrobblerService.scrobble(params, RESPONSE_FORMAT)
                                                .subscribeOn(Schedulers.io())
                                                .subscribeWith(observer);
        compositeDisposable.add(disposable);

        return false;
    }

    public static Job createJob(FirebaseJobDispatcher dispatcher, SortedMap<String, String> params) {
        Bundle extras = mapToBundle(params);

        return dispatcher.newJobBuilder()
                         .setService(ScrobblePlayedSongsService.class)
                         .setTag(ScrobblePlayedSongsService.class.getName())
                         .setRecurring(false)
                         .setLifetime(Lifetime.FOREVER)
                         .setTrigger(Trigger.NOW)
                         .setReplaceCurrent(false)
                         .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                         .setConstraints(Constraint.ON_ANY_NETWORK)
                         .setExtras(extras)
                         .build();
    }
}
