package com.github.niltsiar.ultimatescrobbler.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.configuration.RetrieveUserConfigurationUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.SaveCurrentSongUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.SavePlayedSongUseCase;
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
    SaveCurrentSongUseCase saveCurrentSongUseCase;

    @Inject
    SavePlayedSongUseCase savePlayedSongUseCase;

    @Inject
    RetrieveUserConfigurationUseCase retrieveUserConfigurationUseCase;

    @Inject
    FirebaseJobDispatcher dispatcher;

    CompositeDisposable playedSongsDisposables;

    private static final String START_ACTION = "START_SCROBBLER_SERVICE";
    private static final String STOP_ACTION = "STOP_SCROBBLER_SERVICE";
    private static final String SCROBBLE_NOW_ACTION = "SCROBBLE_NOW_SERVICE";
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

    public static Intent createScrobbleNowIntent(Context context) {
        Intent scrobbleNowIntent = new Intent(context, ScrobblerService.class);
        scrobbleNowIntent.setAction(SCROBBLE_NOW_ACTION);
        return scrobbleNowIntent;
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
            initialize();
        } else {
            switch (intent.getAction()) {
                case START_ACTION:
                    initialize();
                    break;
                case STOP_ACTION:
                    stop();
                    break;
                case SCROBBLE_NOW_ACTION:
                    scrobbleNow();
                    break;
            }
        }
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initialize() {
        DisposableObserver<Long> countDisposable = new DisposableObserver<Long>() {

            @Override
            public void onNext(Long numberOfSongsToScrobble) {
                Timber.i("%d stored songs", numberOfSongsToScrobble);
                retrieveUserConfigurationUseCase.execute(null)
                                                .subscribe(userConfiguration -> {
                                                    if (userConfiguration.getNumberOfSongsPerBatch() <= numberOfSongsToScrobble) {
                                                        scrobbleNow();
                                                    }
                                                });
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };

        DisposableObserver<PlayedSong> nowPlayingDisposable = new DisposableObserver<PlayedSong>() {
            @Override
            public void onNext(PlayedSong nowPlaying) {
                retrieveUserConfigurationUseCase.execute(null)
                                                .subscribe(userConfiguration -> {
                                                    if (userConfiguration.getSendNowPlaying()) {
                                                        saveCurrentSongUseCase.execute(nowPlaying)
                                                                              .subscribe(() -> {
                                                                                  Job sendNowPlayingJob = SendNowPlayingService.createJob(dispatcher, nowPlaying.getId());
                                                                                  dispatcher.mustSchedule(sendNowPlayingJob);
                                                                              });
                                                    }
                                                });
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };

        playedSongsDisposables.add(countDisposable);
        playedSongsDisposables.add(nowPlayingDisposable);

        spotifyReceiver.getNowPlayingSong()
                       .subscribe(nowPlayingDisposable);

        spotifyReceiver.getPlayedSongs()
                       .flatMap(playedSong -> savePlayedSongUseCase.execute(playedSong)
                                                                   .flatMapObservable(Observable::just))
                       .subscribe(countDisposable);

        startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification());
        registerReceiver(spotifyReceiver, SpotifyReceiver.getSpotifyIntents());
    }

    private void stop() {
        stopForeground(true);
        stopSelf();
    }

    private void scrobbleNow() {
        Job scrobbledStoredSongs = ScrobblePlayedSongsService.createJob(dispatcher);
        dispatcher.mustSchedule(scrobbledStoredSongs);
    }

    private Notification createServiceNotification() {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL).setCategory(NotificationCompat.CATEGORY_SERVICE)
                                                                         .setPriority(NotificationCompat.PRIORITY_MIN)
                                                                         .setOngoing(true)
                                                                         .build();
    }

    @Override
    public void onDestroy() {
        playedSongsDisposables.clear();
        unregisterReceiver(spotifyReceiver);
        super.onDestroy();
    }
}
