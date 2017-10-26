package com.github.niltsiar.ultimatescrobbler;

import android.app.Activity;
import android.app.Application;
import com.github.niltsiar.ultimatescrobbler.di.DaggerApplicationComponent;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import javax.inject.Inject;
import timber.log.Timber;

public class UltimateScrobblerApplication extends Application implements HasActivityInjector {

    @Inject
    DispatchingAndroidInjector<Activity> activityDispatchingAndroidInjector;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerApplicationComponent.builder()
                                  .application(this)
                                  .build()
                                  .inject(this);

        Timber.plant(new Timber.DebugTree());

    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return activityDispatchingAndroidInjector;
    }
}
