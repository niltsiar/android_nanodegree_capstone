package com.github.niltsiar.ultimatescrobbler.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.SavePlayedSong;
import com.github.niltsiar.ultimatescrobbler.domain.model.PlayedSong;
import com.github.niltsiar.ultimatescrobbler.receivers.SpotifyReceiver;
import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import javax.inject.Inject;
import timber.log.Timber;

public class ScrobblerService extends Service {

    public static final String NOTIFICATION_CHANNEL = "SCROBBLER_SERVICE_NOTIFICATION_CHANNEL";

    @Inject
    SpotifyReceiver spotifyReceiver;

    @Inject
    SavePlayedSong savePlayedSongUseCase;

    @Inject
    FirebaseJobDispatcher dispatcher;

    CompositeDisposable playedSongsDisposables;

    private static final String START_ACTION = "START_SCROBBLER_SERVICE";
    private static final String STOP_ACTION = "STOP_SCROBBLER_SERVICE";
    private static final int SERVICE_NOTIFICATION_ID = 101010;

    public static Intent createStartIntent(Context context) {
        Intent startIntent = new Intent(context, ScrobblerService.class);
        startIntent.setAction(START_ACTION);
        return startIntent;
    }

    public static Intent createStopIntent(Context context) {
        Intent stopIntent = new Intent(context, ScrobblerService.class);
        stopIntent.setAction(STOP_ACTION);
        return stopIntent;
    }

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();

        playedSongsDisposables = new CompositeDisposable();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent || null == intent.getAction()) {
            throw new IllegalArgumentException("intent or intent.getAction() cannot be null");
        }
        switch (intent.getAction()) {
            case START_ACTION:
                initialize();
                break;
            case STOP_ACTION:
                stop();
                break;
        }
        return Service.START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initialize() {
        DisposableObserver<Pair<Long, PlayedSong>> playedSongDisposable = new DisposableObserver<Pair<Long, PlayedSong>>() {

            @Override
            public void onNext(Pair<Long, PlayedSong> longPlayedSongPair) {
                Long count = longPlayedSongPair.first;
                PlayedSong playedSong = longPlayedSongPair.second;
                Timber.i("%d stored songs", count);
                Job sendNowPlayingJob = SendNowPlayingService.createJob(dispatcher, playedSong.getId());
                dispatcher.mustSchedule(sendNowPlayingJob);
                if (0 == count % 2) {
                    Job scrobbledStoredSongs = ScrobblePlayedSongsService.createJob(dispatcher);
                    dispatcher.mustSchedule(scrobbledStoredSongs);
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };

        playedSongsDisposables.add(playedSongDisposable);

        spotifyReceiver.getPlayedSongs()
                       .flatMap(playedSong -> savePlayedSongUseCase.execute(playedSong)
                                                                   .flatMapObservable(countStoredSongs -> Observable.just(new Pair<>(countStoredSongs, playedSong))))
                       .subscribe(playedSongDisposable);

        registerReceiver(spotifyReceiver, SpotifyReceiver.getSpotifyIntents());

        startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification());
    }

    private void stop() {
        playedSongsDisposables.clear();
        unregisterReceiver(spotifyReceiver);
        stopForeground(true);
        stopSelf();
    }

    private Notification createServiceNotification() {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL).setCategory(NotificationCompat.CATEGORY_SERVICE)
                                                                         .setPriority(NotificationCompat.PRIORITY_MIN)
                                                                         .setOngoing(true)
                                                                         .build();
    }
}
