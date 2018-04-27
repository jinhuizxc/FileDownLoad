package com.example.jh.filedownload;

import android.app.Application;
import android.content.Context;

public class BaseApplication extends Application {

    private static BaseApplication baseApplication;
    @Override
    public void onCreate() {
        super.onCreate();

        baseApplication = this;
    }

    public static Context getAppContext() {
        return baseApplication;
    }
}
