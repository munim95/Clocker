package com.rigid.clocker;

import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.palette.graphics.Palette;

public class ClockerWidgetProvider extends AppWidgetProvider {
    //todo modify the bitmap to match the widgets recommendations
    private static final String WIDGET_THREAD_NAME = "ClockerWidgetThread";
    private static final String TAG = ClockerWidgetProvider.class.getName();
    private PieChartView pieChartView;
    private Bitmap canvasBtmp;
    private Handler minutelyHandler; //runs on UI thread
    private boolean isRunning = false;
    //    private AlarmManager am;

    public ClockerWidgetProvider() {
        super();

    }
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        //get user settings config
        Log.d(TAG,"ONRECEIVE");
//        if(intent.getExtras()!=null){
//            if(intent.getExtras().containsKey("widgetthread")){
//                if(intent.getExtras().getBoolean("widgetthread")){
//                    runWidgetThread(context);
//                }else{
//                    //activity is running so terminate
//                    stopHandler(context);
//                }
//            }
//        }
    }
    /*
    Switched to handler as recommended in the docs/
    Thread.sleep() is unpredictable as it depends on the OS and other factors and certainly not for use with RT ticking clocks
    */
    private Runnable runnable(Context context){
        return new Runnable() {
            @Override
            public void run() {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidget = new ComponentName(context.getPackageName(), ClockerWidgetProvider.class.getName());
                checkWallpaperChanged(context,appWidgetManager,thisAppWidget);
                int s = Helpers.timeConversion(pieChartView.updateTime())[2];
                onUpdate(context,appWidgetManager,appWidgetManager.getAppWidgetIds(thisAppWidget));
                //post on the minute
                minutelyHandler.postAtTime(this,SystemClock.uptimeMillis()+((60*1000)-(s*1000)));
            }
        };
    }
    private void checkWallpaperChanged(Context context,AppWidgetManager awm, ComponentName cn) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        Bitmap newWallBtmp = ((BitmapDrawable) wallpaperManager.getDrawable()).getBitmap();
        //we are comparing the two bitmaps here so we only call this when needed
        if (!isRunning && newWallBtmp!=null) {
            isRunning = true;
            //wall has changed
            Palette.from(newWallBtmp).generate(palette -> {
                if (palette != null) {
                    Palette.Swatch swatch = palette.getDominantSwatch();
                    if (swatch != null) {
                        //set the colours here
                        //only if user pref allow
                        pieChartView.setColourBasedOnWallpaper(swatch.getRgb(), swatch.getBodyTextColor());
                        onUpdate(context,awm,awm.getAppWidgetIds(cn));
                    }
                }
                isRunning=false;
            });
        }

    }
    private void runWidgetHandler(Context context){
        if(minutelyHandler ==null){
            minutelyHandler =new Handler();
            minutelyHandler.post(runnable(context));
        }
    }
    private void stopHandler(Context context){
        if(minutelyHandler !=null) {
            minutelyHandler.getLooper().quit();
            minutelyHandler.removeCallbacksAndMessages(null);
            minutelyHandler =null;
        }

    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        if(pieChartView==null)
            pieChartView=new PieChartView(context);
        //when the first widget instance gets added
        runWidgetHandler(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        //when the last instance of widget is deleted
        stopHandler(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.clocker_widget);
        if(pieChartView==null)
            pieChartView=new PieChartView(context);
        //bitmap memory should not exceed ((screen w * h) * 4 bytes (ARGB_8888) * 1.5)
        // --hence we divide shortest screen size by 4 to stay in safe zone
        int displaySize = Math.min(Resources.getSystem().getDisplayMetrics().widthPixels,
                Resources.getSystem().getDisplayMetrics().heightPixels)/2;
        pieChartView.measure(displaySize,
                displaySize);
        pieChartView.layout(0,0,displaySize,displaySize);
        pieChartView.updateTime();
        if(canvasBtmp !=null){
            canvasBtmp.recycle();
        }
        canvasBtmp = Bitmap.createBitmap(displaySize,
                displaySize,
                Bitmap.Config.ARGB_8888);
        pieChartView.draw(new Canvas(canvasBtmp));
        remoteViews.setImageViewBitmap(R.id.widgetImageView, canvasBtmp);
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);

    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        //when one of the instances of widget is deleted
    }
    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }
}
