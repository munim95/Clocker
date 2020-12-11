package com.rigid.clocker;

import android.Manifest;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.rigid.clocker.settings.SettingsActivity;

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
//todo return intent RESULT OK after configured for app to show up
    private PieChartView pieChartView;
    private AppWidgetManager appWidgetManager;
    private ComponentName appWidget;
    private int appWidgetId=-1;
    private MotionLayout motionLayout;
    private View sectorsTab, bgTab, clockDateTab;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.mainToolbar));

        motionLayout = findViewById(R.id.mainLayout);
        motionLayout.addTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int i, int i1) {

            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int i, int i1, float v) {
                if(v>0){
                    //disable pie sector edit mode
                }else{
                    //enable edit mode
                }
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int i) {
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int i, boolean b, float v) {

            }
        });
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    0);
        pieChartView = new PieChartView(this);
//        pieChartView.runClockThread(); // we call this here to only run the thread when activity is open and not during widget
        FrameLayout frameLayout = findViewById(R.id.pieChartFrameLayout);
        frameLayout.addView(pieChartView);

        appWidgetManager = AppWidgetManager.getInstance(this);
        appWidget = new ComponentName(getPackageName(), ClockerWidgetProvider.class.getName());
        sectorsTab = findViewById(R.id.sectorsTab);
        bgTab = findViewById(R.id.backgroundTab);
        clockDateTab = findViewById(R.id.clockDateTab);
        sectorsTab.setOnClickListener(onClickListener());
        bgTab.setOnClickListener(onClickListener());
        clockDateTab.setOnClickListener(onClickListener());

        //load sectors by default
        sectorsTab.callOnClick();
    }

    private View.OnClickListener onClickListener(){
        return v->{
            if(v.getId()==sectorsTab.getId()){
                if(!sectorsTab.isActivated()) {
                    bgTab.setActivated(false);
                    ((TextView)bgTab).setTextColor(0x80FFFFFF); //80 alpha
                    clockDateTab.setActivated(false);
                    ((TextView)clockDateTab).setTextColor(0x80FFFFFF);
                    sectorsTab.setActivated(true);
                    ((TextView)sectorsTab).setTextColor(0xFFFFFFFF);

                    getSupportFragmentManager().beginTransaction().replace(R.id.editFragment,new SectorsFragment()).commit();
                }
            }else if(v.getId()==bgTab.getId()){
                if(!bgTab.isActivated()) {
                    sectorsTab.setActivated(false);
                    ((TextView)sectorsTab).setTextColor(0x80FFFFFF);
                    clockDateTab.setActivated(false);
                    ((TextView)clockDateTab).setTextColor(0x80FFFFFF);
                    bgTab.setActivated(true);
                    ((TextView)bgTab).setTextColor(0xFFFFFFFF);

                    getSupportFragmentManager().beginTransaction().replace(R.id.editFragment,new BackgroundFragment()).commit();
                }
            }else{
                if(!clockDateTab.isActivated()) {
                    sectorsTab.setActivated(false);
                    ((TextView)sectorsTab).setTextColor(0x80FFFFFF);
                    bgTab.setActivated(false);
                    ((TextView)bgTab).setTextColor(0x80FFFFFF);
                    clockDateTab.setActivated(true);
                    ((TextView)clockDateTab).setTextColor(0xFFFFFFFF);

                    getSupportFragmentManager().beginTransaction().replace(R.id.editFragment,new ClockDateFragment()).commit();
                }
            }
        };
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if(requestCode==0){
//            if(grantResults.length!=0 && grantResults[0]==PackageManager.PERMISSION_DENIED){
//                if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
//                    ActivityCompat.requestPermissions(this,
//                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                            0);
//                }else{
//                    //ok we're dealing with some paranoid users here...
//                    Toast.makeText(this,"Allow permissions in device settings for core features!",
//                            Toast.LENGTH_LONG).show();
//                }
//            }
//        }
    }
    public void updateView(Bundle bundle){

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!=null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                appWidgetId = extras.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //to avoid keyboard pushing up the view hierarchy
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

//        Drawable bgDrawable = WallpaperManager.getInstance(this).getDrawable();
//        if(bgDrawable!=null)
//            motionLayout.setBackground(bgDrawable);
        pieChartView.runClockThread();
        //disable widget thread while activity is running
        suspendWidget(true);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pieChartView.terminateClockerTimeThread();
        //resume widget threads if enabled
        suspendWidget(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        suspendWidget(false);

        if(appWidgetId!=-1 && appWidgetId!=0) {
            AppWidgetManager.getInstance(this).updateAppWidget(appWidgetId,
                    new RemoteViews(getPackageName(), R.layout.clocker_widget));
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
            finish(); //back to widget
        }
    }
    private void suspendWidget(boolean suspend){
        if(appWidgetManager.getAppWidgetIds(appWidget).length!=0) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra("widgetthread",!suspend);
            sendBroadcast(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this,SettingsActivity.class));
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
