package com.rajesh.appbundle;

import android.content.Context;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.google.android.play.core.splitcompat.SplitCompatApplication;

public class MyApplication extends SplitCompatApplication {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        SplitCompat.install(this);
    }
}
