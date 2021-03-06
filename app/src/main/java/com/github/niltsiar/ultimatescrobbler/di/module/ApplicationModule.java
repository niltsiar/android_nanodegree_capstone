package com.github.niltsiar.ultimatescrobbler.di.module;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.github.niltsiar.ultimatescrobbler.BuildConfig;
import com.github.niltsiar.ultimatescrobbler.cache.preferences.ConfigurationCacheImpl;
import com.github.niltsiar.ultimatescrobbler.cache.storage.SongsCacheImpl;
import com.github.niltsiar.ultimatescrobbler.data.ScrobblerDataRepository;
import com.github.niltsiar.ultimatescrobbler.data.UserConfigurationDataRepository;
import com.github.niltsiar.ultimatescrobbler.data.repository.ConfigurationCache;
import com.github.niltsiar.ultimatescrobbler.data.repository.ScrobblerRemote;
import com.github.niltsiar.ultimatescrobbler.data.repository.SongsCache;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.configuration.RetrieveUserConfigurationUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.configuration.SaveUserConfigurationUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.mobilesession.RequestMobileSessionTokenUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.DeletePlayedSongUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.GetCurrentSongUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.GetPlayedSongUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.GetPlayedSongsUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.SaveCurrentSongUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.SavePlayedSongUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.ScrobbleSongsUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.playedsong.SendNowPlayingUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.songinformation.GetSongInformationUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.interactor.songinformation.SaveSongInformationUseCase;
import com.github.niltsiar.ultimatescrobbler.domain.repository.ConfigurationRepository;
import com.github.niltsiar.ultimatescrobbler.domain.repository.ScrobblerRepository;
import com.github.niltsiar.ultimatescrobbler.remote.ScrobblerRemoteImpl;
import com.github.niltsiar.ultimatescrobbler.remote.ScrobblerService;
import com.github.niltsiar.ultimatescrobbler.remote.model.AutoValueMoshiAdapterFactory;
import com.github.niltsiar.ultimatescrobbler.remote.qualifiers.ApiKey;
import com.github.niltsiar.ultimatescrobbler.remote.qualifiers.ApiSecret;
import com.github.niltsiar.ultimatescrobbler.remote.qualifiers.MobileSessionToken;
import com.serjltt.moshi.adapters.FallbackOnNull;
import com.serjltt.moshi.adapters.Wrapped;
import com.squareup.moshi.Moshi;
import dagger.Module;
import dagger.Provides;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

@Module
public abstract class ApplicationModule {

    @Provides
    static Context provideContext(Application application) {
        return application.getApplicationContext();
    }

    @ApiKey
    @Provides
    static String provideApiKey() {
        return BuildConfig.LAST_FM_API_KEY;
    }

    @ApiSecret
    @Provides
    static String provideApiSecret() {
        return BuildConfig.LAST_FM_API_SECRET;
    }

    @MobileSessionToken
    @Provides
    static String provideMobileSessionToken(ConfigurationCache cache) {
        return cache.getMobileSessionToken()
                    .blockingGet();
    }

    @Provides
    static ScrobblerRepository provideScrobblerRepository(ScrobblerDataRepository repository) {
        return repository;
    }

    @Provides
    static ScrobblerRemote provideScrobblerRemote(ScrobblerRemoteImpl remote) {
        return remote;
    }

    @Provides
    static ScrobblerService provideScrobblerService(OkHttpClient okHttpClient) {
        Moshi moshi = new Moshi.Builder().add(AutoValueMoshiAdapterFactory.create())
                                         .add(Wrapped.ADAPTER_FACTORY)
                                         .add(FallbackOnNull.ADAPTER_FACTORY)
                                         .build();

        return new Retrofit.Builder().baseUrl(ScrobblerService.WS_URL)
                                     .client(okHttpClient)
                                     .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                                     .addConverterFactory(MoshiConverterFactory.create(moshi))
                                     .build()
                                     .create(ScrobblerService.class);
    }

    @Provides
    static RequestMobileSessionTokenUseCase provideRequestMobileSessionToken(ScrobblerRepository repository) {
        return new RequestMobileSessionTokenUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static SendNowPlayingUseCase provideSendNowPlaying(ScrobblerRepository repository) {
        return new SendNowPlayingUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static ConfigurationCache provideConfigurationDataStore(RxSharedPreferences rxSharedPreferences) {
        return new ConfigurationCacheImpl(rxSharedPreferences);
    }

    @Provides
    static RxSharedPreferences provideRxSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return RxSharedPreferences.create(sharedPreferences);
    }

    @Provides
    static FirebaseJobDispatcher provideFirebaseJobDispatcher(Context context) {
        return new FirebaseJobDispatcher(new GooglePlayDriver(context));
    }

    @Provides
    static SongsCache provideSongCache(Context context) {
        return new SongsCacheImpl(context);
    }

    @Provides
    static SavePlayedSongUseCase provideSavePlayedSong(ScrobblerRepository repository) {
        return new SavePlayedSongUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static GetPlayedSongsUseCase provideGetStoredPlayedSongs(ScrobblerRepository repository) {
        return new GetPlayedSongsUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static ScrobbleSongsUseCase provideScrobbleSongs(ScrobblerRepository repository) {
        return new ScrobbleSongsUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static GetSongInformationUseCase provideGetSongInformation(ScrobblerRepository repository) {
        return new GetSongInformationUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static GetPlayedSongUseCase provideGetPlayedSongUseCase(ScrobblerRepository repository) {
        return new GetPlayedSongUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static SaveSongInformationUseCase provideSaveSongInformationUseCase(ScrobblerRepository repository) {
        return new SaveSongInformationUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static DeletePlayedSongUseCase provideDeletePlayedSongUseCase(ScrobblerRepository repository) {
        return new DeletePlayedSongUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static RetrieveUserConfigurationUseCase provideRetrieveUserConfigurationUseCase(ConfigurationRepository repository) {
        return new RetrieveUserConfigurationUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static SaveUserConfigurationUseCase provideSaveUserConfigurationUseCase(ConfigurationRepository repository) {
        return new SaveUserConfigurationUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static ConfigurationRepository provideConfigurationRepository(UserConfigurationDataRepository repository) {
        return repository;
    }

    @Provides
    static SaveCurrentSongUseCase provideSaveCurrentSongUseCase(ScrobblerRepository repository) {
        return new SaveCurrentSongUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    static GetCurrentSongUseCase provideGetCurrentsongUseCase(ScrobblerRepository repository) {
        return new GetCurrentSongUseCase(repository, Schedulers.io(), AndroidSchedulers.mainThread());
    }
}
