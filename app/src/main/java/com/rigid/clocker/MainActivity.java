package com.rigid.clocker;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Aim is to provide the user with a comfortable way to organize their daily activities as a set of
 * goals and tasks.
 * Functionality will include (but is not limited to):
 * Be able to set goals
 * Constrain goals by time throughout the day
 * Handle completed/missed goals accordingly
 * Give user a feedback everyday on their productivity based on the goals they accomplished (bonus feature?)
 * */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
