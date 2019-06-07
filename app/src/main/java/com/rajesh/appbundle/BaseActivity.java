package com.rajesh.appbundle;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.splitcompat.SplitCompat;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        SplitCompat.install(this);
    }
}
