package com.rigid.clocker;

import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.palette.graphics.Palette;
import androidx.preference.PreferenceManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;

public class PieChartView extends View {
    /**
     * This PieChart represents the goals of the user in an articulate manner.
     * todo - CLicking on a sector should expand the details of the goal
     *   - Strict Mode where users data (productivity) is compared with other users globally
     *   - CLock will be available as a widget
     *  - Automatically load apps/links pre set by user at specified times
     *  - Goals change depending on location. E.g if at office display office tasks, home display home chores etc
     *  - EXTRA FEATURES (PAID) :
     *  - MAIN - WEATHER, BACKGROUND AS ANY IMAGE, BACKGROUND ASSOCIATED WITH SECTORS
     *  (These features can be displayed at different intervals set by user to respect space.
     *   User should always be made aware of increase in battery consumption )
     *  - Alarms
     *  - New Pie Styles
     *  - Cursor Styles - Sun to moon transition
     *  - Display Weather - bg can have weather related theme
     *  - More Edit options
     *  - Miles walked display
     *  - music playing display
     *  - transport display (departure/arrival)
     *  - Focus mode where phone functionality can be limited and cant be used until a set time (apart from calls etc)
     * The sectors represent the time assigned respectively to the goals.
     * Total time of the sectors equals the total time in a day i.e 24hrs
     * Quadrant = 6 hrs (24hrs)/ 3 hrs (12hrs)
     * Customizable PieChart - User can determine sectors colours, add an image to a sector (clipped in), enable/disable Goals Clock...
     * *
     */
    private SharedPreferences sharedPreferences;
    private static final String TAG = PieChartView.class.getSimpleName();
    private int CLOCK_HOUR_MODE ; //12 or 24
    private float TOTAL_SECONDS_IN_A_DAY ;
    private final float CANVAS_ROTATION = -90;
    private final int MIN_SECTOR_TIME = 5;

    private RectF mainBGRectF, clockBorderRectF;
    private Paint paint;
    private Paint clockDetailsPaint;

    float curr_sector_angle = 0f;
    int curr_sector = 0;
    boolean chart_dirty = true;
    private float sectorNumbersTextSize =40f,
            strokeWidthArc,
            clockTextSize; //default
    private Path interiorCircle;
    float start_angle = 0f;
    private float totalUserAngle; //default

    private ScheduledExecutorService mClockExecutor;
    private long elapsed_time = 0;

    //touch events variables
    private float selectedSectorTotalAngle =0f;
//    private float affectedAngle =0f;
    private float userStartAngle =0f;
    private Sector selectedSector;
    private int userStartQuadrant;
    private int switcher=-1;
    private boolean set=false;
    private boolean isEditSectorMode=true,
            clockEnabled=true,
            stampsEnabled = true,
            clockNumbersEnabled = false;
    private float startBoundAngle =-1, endBoundAngle =-1;
    private int affectedSector_index;
    private ArrayList<Sector> sectors;
    private boolean AmOrPm;
    private Path clockNumbersPath = new Path();
    private Path currentTimePath = new Path();
    private long startTime,endTime;
    private float hourGapAngle ; //1hr gap
    private Sector currSector=null;
    private float currentSectorEndAngle =0;
    private boolean checkCurrentSector =false;
    private int clockAlignmentPresets = ClockAlignment.LEFT;
    private int datePresets = DateStylePresets.BOTTOM_P2;
    private float animatedAlphaValue =1;
    private ValueAnimator alphaValueAnimator;
    private Thread clockerTimeThread;
    private Calendar calendar;


    /* Colours */
    int bgColour = Color.argb(getRGBAlpha(0.3f),255,255,255); //test colours--users default preferences go here
    int topColour = Color.BLACK;

    private boolean isEditing =false;

    public PieChartView(Context context) {
        super(context);
        init(context, null);
    }

    /* not used */
    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        CLOCK_HOUR_MODE = Integer.parseInt(sharedPreferences.getString("clockmode","12"));
        TOTAL_SECONDS_IN_A_DAY=CLOCK_HOUR_MODE * 3600;
        hourGapAngle= getAngleForTimeInSeconds(60*60);
        calendar=Calendar.getInstance();
        sectors= new ArrayList<>();
        //todo minimum total time = 5 mins
        sectors.add(new Sector("Bla1",60,120,Color.RED));//1-2
        sectors.add(new Sector("Bla2",120,240,Color.BLUE));//2-4
        sectors.add(new Sector("Bla3",240,300,Color.GREEN));//4-5
        sectors.add(new Sector("Bla4",300,360,Color.BLUE));//5-7
        sectors.add(new Sector("Bla5",400,460,Color.RED));
//        sectors.add(new Sector("Bla5",420,1080,Color.CYAN));//7-18 (24 only test)

        //todo upon user permission
//        isPieFull();

    }
    //returns total angle
    private void isPieFull(){
        //-1 for alpha/non visible
        int s = sectors.size();
        for(int i=0; i<s; i++){
            int next = i+1;
            if(next<s) {
                if (sectors.get(i).getEndTime() != sectors.get(next).getStartTime()) {
                    sectors.add(new Sector("Break1",sectors.get(i).getEndTime(),sectors.get(next).getStartTime(),-1));
                }
            }
        }
        long lastEnd = sectors.get(s - 1).getEndTime(); //last sectors end = breaks start time, first sectors start = breaks end time
        long start = sectors.get(0).getStartTime();
        long secs = (lastEnd-start) *60;
        if(secs!=0) {
            if (getAngleForTimeInSeconds(secs) < 360) {
                //to check if remaining pie needs to be filled or not with break
                sectors.add(new Sector("Break", lastEnd, start, -1));
            }
        }
    }

    private String hourText, minuteText;
    //decided to make this public to use in both in widget provider and here
    public long updateTime(){
        /*TEST*/
        elapsed_time+=600;
        if(elapsed_time>=CLOCK_HOUR_MODE*3600)
            elapsed_time=0;

//        elapsed_time = getCurrentTime(CLOCK_HOUR_MODE);
//        AmOrPm = (CLOCK_HOUR_MODE!=24?getCurrentTime(24):elapsed_time) < 12;
        int[] s = Helpers.timeConversion(elapsed_time);
        hourText = s[0] < 10 ? "0" + s[0] : s[0] + "";
        minuteText = s[1] < 10 ? "0" + s[1] : s[1]+"";
        return elapsed_time;
    }
    public void runClockThread() {
        if(clockEnabled) {
            if(clockerTimeThread ==null||!clockerTimeThread.isAlive()) {
                clockerTimeThread =new Thread(()-> {
                    while (true) {
                        updateTime();
                        postInvalidate();
                        try{
                            /* TEST - 1 ms*/
                            Thread.sleep(100); //call at intervals of one second
                        }catch (InterruptedException e){
                            break;
                        }
                    }
                    Log.i(TAG,Thread.currentThread().getName()+ " has been terminated.");
                },"ClockerActivityThread");
                clockerTimeThread.start();
            }
        }
    }
    private long getCurrentTime(int hourMode){
        // offset to add since we're not UTC
        long offset = calendar.get(Calendar.ZONE_OFFSET) +
                calendar.get(Calendar.DST_OFFSET);
        long sinceMidnight = (calendar.getTimeInMillis() + offset) %
                (hourMode * 60 * 60 * 1000);
        return sinceMidnight / 1000;
    }

    private float outerToInnerGap,dateSize;
    private Path timeCursor = new Path();
    private float[] clockXy,weatherXy;
    private String day,date;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //add text size to the left and top of rect to avoid it clipping at the edges
        //figure out the max radius it can be, taking text in to account
        //although getWidth and height will be the same, using math.min is just a precaution
        float textAndStrokeSize = strokeWidthArc=(Math.round(0.01f*w));
        float rectRightBottom = Math.min(w,h)- textAndStrokeSize*2;
        mainBGRectF = new RectF(textAndStrokeSize*2, textAndStrokeSize*2, rectRightBottom, rectRightBottom);
        float radius=(mainBGRectF.width()/2f);
        clockTextSize = 0.4f*radius; // to keep text in proportion

        paint = new Paint();
        paint.setAntiAlias(true);
        clockDetailsPaint=new Paint();
        clockDetailsPaint.setAntiAlias(true);

        paint.setTextSize(clockTextSize);
        paint.getTextBounds(hourText,0,hourText.length(), clockTextMeasure); //height of the hour text
        maxTextHeight=clockTextMeasure.height();
        clockHourWidth = paint.measureText(hourText)*.5f;
        //alignment coords
        //radius = bg radius - length of the 1 hr arc in 12 hr or 2 hr arc in 24hr
        outerToInnerGap =(0.05f*radius);
        maxDotRadius = .5f*outerToInnerGap;
        //hour arc length
        float length = getArcLengthForTime(mainBGRectF.width()/2f,60*60);
        dateSize =
//                maxTextHeight*.65f
        clockHourWidth *.4f;
        day = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        date = calendar.get(Calendar.DATE)+" "+calendar.getDisplayName
                (Calendar.MONTH,Calendar.SHORT,Locale.getDefault())+" "+
                calendar.get(Calendar.YEAR);
        if(clockAlignmentPresets != ClockAlignment.CENTER) {
            //apply indent from the edge of the circle if dates are in the presets below:
            //right && !p1
            //top && !p2
            //left && !p3
            //bottom && !p4
            if(clockAlignmentPresets== ClockAlignment.RIGHT && datePresets!=DateStylePresets.LEFT_P1 ||
            clockAlignmentPresets== ClockAlignment.TOP && datePresets!=DateStylePresets.BOTTOM_P2 ||
            clockAlignmentPresets== ClockAlignment.LEFT && datePresets!=DateStylePresets.RIGHT_P3 ||
            clockAlignmentPresets== ClockAlignment.BOTTOM && datePresets!=DateStylePresets.TOP_P4) {
                Rect tRect = new Rect();
                paint.setTextSize(dateSize);
                paint.getTextBounds("S", 0, 1, tRect);
                float size = tRect.height();
                paint.setTextSize(((float)day.length()/date.length())*dateSize);
                paint.getTextBounds("0", 0, 1, tRect);
                size += tRect.height();

                length = length + size;
            }
        }
        //xy coords relative to the given radius
        clockXy = polarToCartesian(mainBGRectF.centerX(),mainBGRectF.centerY(),
                clockAlignmentPresets != ClockAlignment.CENTER?
                        radius- length:
                        0, clockAlignmentPresets != ClockAlignment.CENTER ? clockAlignmentPresets :0);
        weatherXy = polarToCartesian(mainBGRectF.centerX(),mainBGRectF.centerY(),
                radius-getArcLengthForTime(radius,60*60), 0);

        if(stampsEnabled) {
             //gap b/w outer and inner circle
            //create the quarterly stamp
            interiorCircle = new Path();
            Path pathStamp = new Path();

            //reason for opting for manual over pathDashPathEffect -
            // 1. draw called unnecessarily for stamps where not needed
            // 2. subsequently that also results in better performance
            float startTimeAngle = 0f;
            float tenMinutely = hourGapAngle/6; //divide in to six points between hours representing 10 min
            for (int i = 0; i < CLOCK_HOUR_MODE*6; i++) { //total dots = 24hrs - 144, 12hrs - 72
                float[] xy = polarToCartesian(mainBGRectF.centerX(), mainBGRectF.centerY(),
                        radius - outerToInnerGap,startTimeAngle);
                boolean isQuarterly = startTimeAngle%90==0,
                isHourly = startTimeAngle%hourGapAngle==0,
                isTenMinutely = startTimeAngle%tenMinutely==0;
                if(isQuarterly) { //quarterly
                    pathStamp.reset();
                    pathStamp.addCircle(0, 0, outerToInnerGap/2f, Path.Direction.CW);
                    interiorCircle.addPath(pathStamp,xy[0],xy[1]);
                }
                if(!isQuarterly && isHourly){ //hourly
                    pathStamp.reset();
                    pathStamp.addCircle(0, 0, outerToInnerGap/4f, Path.Direction.CW);
                    interiorCircle.addPath(pathStamp,xy[0],xy[1]);
                }
                if(!isQuarterly && !isHourly && isTenMinutely){
                    pathStamp.reset();
                    pathStamp.addCircle(0, 0, outerToInnerGap/8f, Path.Direction.CW);
                    interiorCircle.addPath(pathStamp,xy[0],xy[1]);
                }
                startTimeAngle += tenMinutely;
            }
        }
        timeCursor = Helpers.createPath(new Path(),3,strokeWidthArc,0,0);
        /*
         * We moved all the elements that do not change after drawn from the onDraw here to avoid unnecessary draw calls
         * Sectors will only be drawn again in edit mode
         * */
        initStaticChartElements(mainBGRectF, sectors);
    }
    private float getArcLengthForTime(float radius, int seconds){
        return (float) (getAngleForTimeInSeconds(CLOCK_HOUR_MODE/12*seconds)/360f * (2*Math.PI*(radius)));
    }
    private float maxDotRadius;
    private void initStaticChartElements(RectF rectF, ArrayList<Sector> sectors){
        Bitmap bitmap = Bitmap.createBitmap(getWidth(),getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.save();
        canvas.rotate(CANVAS_ROTATION,rectF.centerX(),rectF.centerY()); // 0 degrees on top instead of at 90 degrees
        Paint paint = new Paint();
        //        setLayerType(LAYER_TYPE_HARDWARE,paint); // look in to this if rendering slow on min API version
        paint.setStyle(Paint.Style.FILL);
//        paint.setStrokeWidth(strokeWidthArc/4);
//        paint.setColor(Color.argb(getRGBAlpha(0.3f),255,255,255));
//        paint.setColor(bgColour);
//        paint.setARGB(0,255,255,255); //custom bg
        /* BG circle */
        //we are adding stroke width to radius so that the arc edges don't blend in with the wallpaper
        canvas.drawCircle(rectF.centerX(),rectF.centerY(),rectF.width()/2f+strokeWidthArc,paint);

        /* Custom BG bitmap */
        //if custom bitmap is set then change the top colour based on it
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        Bitmap bmptest = ((BitmapDrawable) WallpaperManager.getInstance(getContext()).getDrawable()).getBitmap();

        //create a copy of the rectF with the radius of the entire circle (We don't change the original RectF to keep the interior elements in proportion)
        RectF circleRectF = new RectF(rectF.left-strokeWidthArc,rectF.top-strokeWidthArc,
                rectF.right+strokeWidthArc,rectF.bottom+strokeWidthArc);
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0,0,bmptest.getWidth(),bmptest.getHeight()),circleRectF, Matrix.ScaleToFit.FILL);
        canvas.drawBitmap(bmptest,m,paint);
        paint.reset();
        //synchronous
        Palette.Swatch swatch= Palette.from(bmptest).generate().getVibrantSwatch();
        topColour=swatch!=null?swatch.getBodyTextColor():topColour;

        /*----lay down the clock numbers----*/
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(topColour);
        paint.setAlpha(getRGBAlpha(1f));
        if(clockNumbersEnabled) {
//        sectorClockRadius *= 1; // 0 being centered, 1 being on the arc edges.
            paint.setTextSize(sectorNumbersTextSize);
            paint.setTextAlign(Paint.Align.CENTER); //defaults to left
            float startTimeAngle = 0f;
            for (int i = 0; i < CLOCK_HOUR_MODE; i++) {
                clockNumbersPath.rewind();
                //a way to center numbers in their exact locations is to make the number's start angle the previous start angle and making the hour gap to 2 hours
                clockNumbersPath.addArc(rectF, startTimeAngle - hourGapAngle < 0 ? 360f - hourGapAngle : startTimeAngle - hourGapAngle, hourGapAngle * 2); //- offset the text in the middle of the angles
                canvas.drawTextOnPath((i == 0 ? CLOCK_HOUR_MODE!=24?CLOCK_HOUR_MODE:"00" : (CLOCK_HOUR_MODE - (CLOCK_HOUR_MODE - i))) + "", clockNumbersPath, 0f, sectorNumbersTextSize, paint);
                startTimeAngle += hourGapAngle;
            }
        }/*----PATH STAMPS----*/
        else if(stampsEnabled) {
            canvas.drawPath(interiorCircle,paint);
        }
        /*---- SECTORS ----*/
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidthArc);
        for(Sector s: sectors){
            float sweep_angle = getAngleForTimeInSeconds(s.getTotalTime(CLOCK_HOUR_MODE)*60);
            paint.setColor(s.getColour());
            paint.setAlpha(getRGBAlpha(s.getColour()!=-1?0.2f:0));
            canvas.drawArc(rectF, start_angle+getAngleForTimeInSeconds(s.getStartTime()*60), sweep_angle, false, paint);
//            Log.d(TAG,"sectors "+s.getName()+" "+(start_angle+getAngleForTimeInSeconds(s.getStartTime()*60))+" "+(sweep_angle));
        }
        canvas.restore();

        /*------------ DATE PRESETS ----------------*/

        //use textsizeforwidth if we dont want gaps but still want to fit
//                dateSize = getTextSizeForWidth(paint,maxTextHeight*2,"Sunday",clockTextSize);
        //max for matching width without chars intersecting - 65% of half the desired width, anything higher results in intersection/not appealing

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(topColour);
        paint.setTextSize(dateSize);
        Rect dayRect = new Rect();
        paint.getTextBounds(day,0,day.length(),dayRect);
        float x =datePresets==DateStylePresets.LEFT_P1 ?
                clockXy[0]-(clockHourWidth + maxDotRadius):
                datePresets==DateStylePresets.BOTTOM_P2 || datePresets==DateStylePresets.TOP_P4 ?
                clockXy[0]- clockHourWidth :
                datePresets==DateStylePresets.RIGHT_P3 ?
                clockXy[0]+ clockHourWidth + maxDotRadius :
                        0;

        float y = datePresets==DateStylePresets.LEFT_P1 ? clockXy[1]+maxTextHeight+ maxDotRadius :
                datePresets==DateStylePresets.BOTTOM_P2 ? clockXy[1]+maxTextHeight+ maxDotRadius *2+dayRect.height():
                        datePresets==DateStylePresets.RIGHT_P3 ? clockXy[1]-maxTextHeight:
                                datePresets==DateStylePresets.TOP_P4 ? clockXy[1]-(maxTextHeight+ maxDotRadius)
                                        :0;
        if(datePresets==DateStylePresets.LEFT_P1 || datePresets==DateStylePresets.RIGHT_P3) {
            canvas.save();
            if(datePresets==DateStylePresets.LEFT_P1) {
                canvas.rotate(-90, x, y);
            }
            else if(datePresets==DateStylePresets.RIGHT_P3) {
                canvas.rotate(90, x, y);
            }
        }
        //the difference in width/height between dates and clock - (clockWidth - text width) / no. of chars to spread between
        float gapDiff =
                datePresets==DateStylePresets.LEFT_P1 || datePresets==DateStylePresets.RIGHT_P3 ?
                ((maxTextHeight*2 + maxDotRadius) - paint.measureText(day))/(day.length()-1):
                        datePresets==DateStylePresets.BOTTOM_P2 || datePresets==DateStylePresets.TOP_P4 ?
                ((clockHourWidth *2) - paint.measureText(day))/(day.length()-1):
                                0;
        //lay out individual chars of the string so it matches the height/width of the clock
        for(int i =0; i<day.length(); i++){
            char c = day.charAt(i);
            //bear in mind x, y inverted so +x -> - ,vice versa
            canvas.drawText(c+"", x,y,paint);
            x+=gapDiff+paint.measureText(c+"");
        }

        x =datePresets==DateStylePresets.LEFT_P1 ?
                clockXy[0]-(clockHourWidth + maxDotRadius *2 +dayRect.height()):
                datePresets==DateStylePresets.RIGHT_P3 ?
                        clockXy[0]+ clockHourWidth + maxDotRadius *2 + dayRect.height():
                        clockXy[0]- clockHourWidth;
        y = datePresets==DateStylePresets.BOTTOM_P2 ? clockXy[1]+maxTextHeight+ maxDotRadius +dayRect.height()*2:
                datePresets==DateStylePresets.TOP_P4 ? clockXy[1]-(maxTextHeight+dayRect.height()+ maxDotRadius)
                        :y;
        if(datePresets==DateStylePresets.LEFT_P1 || datePresets==DateStylePresets.RIGHT_P3) {
            canvas.restore();
            canvas.save();
            if(datePresets==DateStylePresets.LEFT_P1) {
                canvas.rotate(-90, x, y);
            }
            else if(datePresets==DateStylePresets.RIGHT_P3) {
                canvas.rotate(90, x, y);
            }
        }


        // to keep the size in bounds - ratio(day chars/date chars)  * dateSize
        paint.setTextSize(((float)day.length()/date.length())*dateSize);
        gapDiff=datePresets==DateStylePresets.LEFT_P1 || datePresets==DateStylePresets.RIGHT_P3 ?
                ((maxTextHeight*2 + maxDotRadius) - paint.measureText(date))/(date.length()-1):
                datePresets==DateStylePresets.BOTTOM_P2 || datePresets==DateStylePresets.TOP_P4 ?
                        ((clockHourWidth *2) - paint.measureText(date))/(date.length()-1):
                        0;
        for(int i =0; i<date.length(); i++){
            char c = date.charAt(i);
            //bear in mind x, y inverted so +x -> - ,vice versa
            canvas.drawText(c+"", x, y,paint);
            x+=gapDiff+paint.measureText(c+"");
        }
        if(datePresets==DateStylePresets.LEFT_P1 || datePresets==DateStylePresets.RIGHT_P3)
            canvas.restore();
        /* -----------Preset 2 - Bottom Align-----------------*/
//                c.drawText("08 Sep 2020",rectF.centerX()-(clockWidth+radiusOfHourDot*2),rectF.centerY()+maxTextHeight - f.height(), clockTextPaint);

        setBackground(new BitmapDrawable(getResources(),bitmap));

    }
    public void setColourBasedOnWallpaper(int bg, int other){
        bgColour=bg;
        topColour=other;
        initStaticChartElements(mainBGRectF,sectors);
    }
    protected void onDraw(Canvas canvas) {
//        canvas.clipPath(p, Region.Op.DIFFERENCE)
        makePie(canvas, mainBGRectF);
        super.onDraw(canvas);
    }
    //PieChart with respect to time
    //The pie chart represents real time by shading itself as time elapses (24hrs)
    //tells us what sector the time is currently in
    //names each sector
    //t - times for goals
    //todo elements not changed once drawn should be extracted in a bitmap
    //  ARGB values in paint moddable by the user

    // TODO: 08/11/2020
    //  FIND THINGS THAT CAN BE STATIC AND INCLUDE THEM IN STATIC METHOD ABOVE
    //  ALARMS, REMINDERS
    //  MEDIUM:
    //  -WIDGET - time cursor should not be transparent
    //  LOW: (Fix essential for robustness / Occur only in extreme testing conditions/ chances of occurring normally are slim to none)
    //  (TEST - CHANGED CONSTRAINTS: thread.sleep(), elapsedTime)
    //  - (check findsectorangle for this )when testing, skipping 1300s every second caused the green sector to overlap the next sectors till the end,
    //  doesn't happen when skipping every second normally
//    private String nextSectorName;
    private void makePie(Canvas canvas, RectF bgRectF) {
        canvas.save();
        canvas.rotate(CANVAS_ROTATION,bgRectF.centerX(),bgRectF.centerY()); // 0 degrees on top instead of at 90 degrees, for convenience

        /*----SECTORS----*/
        float currTimeAngle = getAngleForTimeInSeconds((int) elapsed_time);
        //determines what sector time is in
        findSectorForAngle(currTimeAngle);
        if(currSector!=null && !isEditing) {
            /* Draw the elapsed time sector */
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidthArc);
            paint.setColor(currSector.getColour());
            paint.setAlpha(currSector.getColour()!=-1?255:0);
//        paint.setAlpha(getRGBAlpha(1f));
            float currSectorStartAngle = getAngleForTimeInSeconds(currSector.getStartTime() * 60);
            canvas.drawArc(bgRectF, currSectorStartAngle,
                    (currTimeAngle-currSectorStartAngle<0?currTimeAngle+360:currTimeAngle)-currSectorStartAngle, false, paint);

        }
        //if editing then change only the selected sector state
        if(isEditing){
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidthArc);
            float sweep_angle = getAngleForTimeInSeconds(selectedSector.getTotalTime(CLOCK_HOUR_MODE)*60);
            paint.setColor(selectedSector.getColour());
            paint.setAlpha(selectedSector.getColour()!=-1?255:0);
            canvas.drawArc(bgRectF, start_angle+getAngleForTimeInSeconds(selectedSector.getStartTime()*60), sweep_angle, false, paint);
//            Log.d(TAG,"sectors "+s.getName()+" "+(start_angle+getAngleForTimeInSeconds(s.getStartTime()*60))+" "+(sweep_angle));
        }

        /* --- Time Cursor --- */
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(topColour);
        float[] xy = polarToCartesian(bgRectF.centerX(),bgRectF.centerY(),bgRectF.width()*.5f,currTimeAngle);
        canvas.save();
        canvas.rotate(currTimeAngle+180,xy[0],xy[1]); // +180 to invert the cursor to point downwards
        canvas.translate(xy[0],xy[1]);
        canvas.drawPath(timeCursor,paint);
        canvas.restore();



//        canvas.clipOutRect(rectF);

        /*----TIME----*/
//        if(clockEnabled) {
//            float currTimeAngle = getAngleForTimeInSeconds((int) elapsed_time);
            /*1. this path will draw current time always*/

            /*2. once time reaches the start time, start drawing the arc to represent time past*/
//            float timeStartAngle = getAngleForTimeInSeconds(9*3600); // starts at9  (as a test, set by the user)
            paint.setStyle(Paint.Style.FILL);
//            paint.setColor(Color.BLUE);
//            paint.setAlpha(getPaintAlpha(0.5f));

//            if (currTimeAngle >= (AmOrPm ? (360 - timeStartAngle) : timeStartAngle)) {
//                float sweepy = currTimeAngle - timeStartAngle;
//                if (AmOrPm)
//                    sweepy = currTimeAngle + (360 - timeStartAngle); //gone past 360/0
//                canvas.drawArc(rectF, timeStartAngle, sweepy, true, paint);
//            }
//            Log.d(TAG, "ELAPSED TIME (SECONDS) " + elapsed_time + "-> ANGLE " + currTimeAngle);
            /*3. draw out clock time text in the middle*/
            canvas.restore(); //restore here so text isn't rotated as well

            paint.setTextSize(clockTextSize);
            paint.setColor(topColour);
//            paint.setTextAlign(Paint.Align.CENTER);

        /* --- WEATHER DISPLAY --- */
            Rect rect = new Rect();
            paint.getTextBounds("8",0,1,rect); // temperature bounds
            float y = weatherXy[1];
            canvas.drawText("8",weatherXy[0],y,paint);
            GradientDrawable degreeIcon = (GradientDrawable) getResources().getDrawable(R.drawable.degree_symbol,null);
            degreeIcon.setStroke((int)(strokeWidthArc*.5f),topColour);
            degreeIcon.setBounds((int)(weatherXy[0]+paint.measureText("8")),(int)(weatherXy[1]-rect.height()),
                    (int)(weatherXy[0]+paint.measureText("8")+rect.height()*.1f),(int)((weatherXy[1]-rect.height())+rect.height()*.1f));
            degreeIcon.draw(canvas);
            //fetched from weather api
            Drawable weatherIcon = getResources().getDrawable(R.drawable.ic_android_black_24dp,null);
            weatherIcon.setBounds((int)bgRectF.centerX(),(int)(weatherXy[1]-rect.height()),(int) weatherXy[0],(int) weatherXy[1]);
            weatherIcon.draw(canvas);
        float totalWidth =weatherIcon.getBounds().width()+degreeIcon.getBounds().width()+paint.measureText("8");
            float x = (weatherXy[0]+degreeIcon.getBounds().width())-weatherIcon.getBounds().width();
            paint.setTextSize(getMaxTextSizeForWidth(paint,totalWidth,"Birmingham",clockTextSize)*.5f);
            paint.getTextBounds("Birmingham",0,"Birmingham".length(),rect); //city label bounds
            drawTextAlongWidth(canvas,"Birmingham",x,y+rect.height(),totalWidth,paint);
            paint.setTextSize(clockTextSize);

            /* Animation handling and clock text*/
            if(isEditing){
                int[] s;
                if(alphaValueAnimator.isRunning()) {
                    s= Helpers.timeConversion(elapsed_time);
                    hourText = s[0] < 10 ? "0" + s[0] : s[0] + "";
                    minuteText = s[1] < 10 ? "0" + s[1] : s[1]+"";
                    //fade out the elapsed time
                    paint.setAlpha(getRGBAlpha(animatedAlphaValue));
                    addClockText(canvas, paint, clockAlignmentPresets);
                    //fade in the edit clock text
                    paint.setAlpha(getRGBAlpha(1- animatedAlphaValue));
                }
                s = Helpers.timeConversion((startBoundAngle!=-1?selectedSector.getStartTime():selectedSector.getEndTime())*60);
                hourText = s[0] < 10 ? "0" + s[0] : s[0] + "";
                minuteText = s[1] < 10 ? "0" + s[1] : s[1]+"";
                addClockText(canvas,paint, clockAlignmentPresets);
            }else{
                if(alphaValueAnimator !=null) { //editing just stopped so animation to bring back clock
                    int[] s;
                    if (selectedSector!=null && alphaValueAnimator.isRunning()) {
                        s = Helpers.timeConversion((startBoundAngle!=-1?selectedSector.getStartTime():selectedSector.getEndTime())*60);
                        hourText = s[0] < 10 ? "0" + s[0] : s[0] + "";
                        minuteText = s[1] < 10 ? "0" + s[1] : s[1]+"";
                        paint.setAlpha(getRGBAlpha(animatedAlphaValue));
                        addClockText(canvas, paint, clockAlignmentPresets);
                        paint.setAlpha(getRGBAlpha(1 - animatedAlphaValue));
                    }
                    if(!alphaValueAnimator.isRunning()) //since it is static setting it to null affects it instantly so we wait for it stop first
                        alphaValueAnimator =null;
                    s= Helpers.timeConversion(elapsed_time);
                    hourText = s[0] < 10 ? "0" + s[0] : s[0] + "";
                    minuteText = s[1] < 10 ? "0" + s[1] : s[1]+"";
                }
                addClockText(canvas,paint, clockAlignmentPresets);
                if(currSector!=null)
                    addSectorsDetailsText(canvas,currSector,bgRectF,clockDetailsPaint, clockAlignmentPresets);
            }

//        }
    }
    private void drawTextAlongWidth(Canvas canvas, String text,float x, float y, float desired,Paint p){
        float g = (desired-p.measureText(text))/(text.length()-1);
        for(int i=0;i<text.length(); i++){
            char c  = text.charAt(i);
            canvas.drawText(c+"",x,y,p);
            x+=g+p.measureText(c+"");
        }
    }
    private void findSectorForAngle(float sweep_time_angle){
        if(currSector==null) {
            for(Sector s:sectors){
                float sAngle = getAngleForTimeInSeconds(s.getStartTime()*60);
                float eAngle = getAngleForTimeInSeconds(s.getEndTime() * 60);

                if(eAngle<sAngle){
                    if((sweep_time_angle>=sAngle && sweep_time_angle>eAngle) ||
                            (sweep_time_angle<sAngle && sweep_time_angle<=eAngle)){
                        currSector=s;
//                        Log.d(TAG, "current sector -> " + currSector.getName());
                        break;
                    }
                }
                if((sweep_time_angle>=sAngle&& sweep_time_angle<eAngle)){
                    currSector=s;
//                    Log.d(TAG, "current sector -> " + currSector.getName());
                    break;
                }
            }
//            if(currSector!=null){
//                int index = sectors.indexOf(currSector);
//                nextSectorName = sectors.get(index!=sectors.size()-1?index+1:0).getName();
//            }
            currentSectorEndAngle = currSector!=null?getAngleForTimeInSeconds(currSector.getEndTime() * 60):0;
        }else {
            //handles both normal and special cases
            //when end < start then the sweep angle will remain greater than currEndAngle until it goes past 12/24
            if (!checkCurrentSector && sweep_time_angle < currentSectorEndAngle)
                checkCurrentSector = true;
            if (checkCurrentSector) {
                if (sweep_time_angle >= currentSectorEndAngle) {
                    currSector = null;
                    checkCurrentSector =false;
                }
            }
        }
    }
    public void terminateClockerTimeThread(){
        if(clockerTimeThread.isAlive()) {
            clockerTimeThread.interrupt();
            clockerTimeThread =null;
        }
    }
    //todo look in to optimising touch event code further
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isEditSectorMode)
            return false;
        //handles selecting sectors and editing their values
        float x = event.getX();
        float y = event.getY();
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                selectedSector = findSectorForXY(x,y, sectors);
                if(selectedSector==null)
                    return false;
                //ONLY IN EDIT MODE
                if(isEditSectorMode) {
                    userStartAngle = cartesianToPolar(x, y)-CANVAS_ROTATION; //for +/- angles
                    userStartAngle=Math.min(360f,userStartAngle>360?userStartAngle-360:userStartAngle);
                    selectedSectorTotalAngle = getAngleForTimeInSeconds(selectedSector.getTotalTime(CLOCK_HOUR_MODE) * 60);
                    startTime=selectedSector.getStartTime();
                    endTime=selectedSector.getEndTime();
                    startBoundAngle =getAngleForTimeInSeconds(startTime*60);
                    endBoundAngle =getAngleForTimeInSeconds(endTime*60);
                    float middle = start_angle+(startBoundAngle +selectedSectorTotalAngle/2f);

                    //determine if its start or end bound thats changing
                    if(endBoundAngle < startBoundAngle){ //handle the case where end bound ends up beyond 360 and find middle
                        if(middle<360) {
                            if (userStartAngle >= startBoundAngle && userStartAngle < middle)
                                endBoundAngle = -1;
                            else if (userStartAngle >= middle || userStartAngle < endBoundAngle)
                                startBoundAngle = -1;
                        }else{
                            if(userStartAngle >= startBoundAngle || userStartAngle<(middle-360))
                                endBoundAngle =-1;
                            else if(userStartAngle >= (middle-360) && userStartAngle < endBoundAngle)
                                startBoundAngle =-1;
                        }
                    }else {
                        if ((userStartAngle >= startBoundAngle) && (userStartAngle < middle)) {
                            endBoundAngle = -1;
                        } else if ((userStartAngle >= middle) && (userStartAngle < endBoundAngle)) {
                            startBoundAngle = -1;
                        }
                    }
                    int in=sectors.indexOf(selectedSector);
                    affectedSector_index = startBoundAngle !=-1?
                            (in!=0?in-1:sectors.size()-1):
                            (in!=sectors.size()-1?in+1:0);
//                    Log.d(TAG,"st "+startBound+" en "+endBound+" mid "+middle+" u "+userStartAngle);
                    userStartQuadrant = getQuadrant(userStartAngle);
                    //reset these here...
                    switcher = -1;
                    set = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(selectedSector==null)
                    return false;
                //ONLY IN EDIT MODE
                if(isEditSectorMode) {
                    //disable clock
                    if(!isEditing) {
                        clockEnabled = false;
                        terminateClockerTimeThread();
                        isEditing = true;
                    }

                    //suspend clock thread
                    float userCurrAngle = cartesianToPolar(x, y)-CANVAS_ROTATION;
                    userCurrAngle=Math.min(360f,userCurrAngle>360?userCurrAngle-360f:userCurrAngle);
//                    float dAngle = userCurrAngle - userStartAngle; // how much angle has moved from start. Default behaviour - simply + if increasing or - otherwise

                    // angles will shift to 0 or 360 from 4th to 1st or 1st to 4th quadrants
                    int dQuadrant = getQuadrant(userCurrAngle) - userStartQuadrant;

                    //when one is true the other is off for one whole event (ACTION_DOWN to ACTION_MOVE to ACTION_UP)
                    if (dQuadrant == -3) { // 4th to 1st quadrant (1-4)
                        if (switcher == 3 || switcher == -1) { // if set previously or first time set=false as its gone back to normal state
                            if (set) {
                                set = false; // switch off
                                switcher = -1;
                            } else {
                                switcher = dQuadrant;
                                set = true;
                            }
                        }
                    } else if (dQuadrant == 3) { //1st to 4th (4-1)
                        if (switcher == -3 || switcher == -1) { // set previously, turn off as its gone back to normal state
                            if (set) {
                                set = false; // switch off
                                switcher = -1;
                            } else {
                                switcher = dQuadrant;
                                set = true;
                            }
                        }
                    }
                    //handle according to the changes in quadrant
//                    if (set) {
//                        if (switcher == -3) {
//                            dAngle = (360 + userCurrAngle) - userStartAngle;
//                        } else if (switcher == 3) {
//                            dAngle = -((360 - userCurrAngle) + userStartAngle); //- since we are decreasing (anticlockwise)
//                        }
//                    }
//                    float newAngle=(startBoundAngle !=-1? startBoundAngle : endBoundAngle) + dAngle;
//                    newAngle=newAngle<0f?360f+newAngle:newAngle>360f?newAngle-360f:newAngle;
                    Sector affectedSector = sectors.get(affectedSector_index);
                    int newTime=getTimeForAngleInDegrees(Math.min(360, Math.max(0, userCurrAngle))) / 60;

                    //Here we handled sectors limitations also handling cases where sector fell in the 'isNotNormal' state-
                    // i.e either endTime < startTime OR affected sectors values differ from the selected sector(e.g should be <=selected sector values if start bound is changed).
//                    boolean isNotNormal = startBoundAngle !=-1?
//                            endTime < affectedSector.getStartTime():
//                            startTime > affectedSector.getEndTime(); // for start bound selected sector normally should have values greater than its affected sector
//                    long minEndTime = startBoundAngle !=-1?
//                            isNotNormal && newTime > endTime ? endTime + (CLOCK_HOUR_MODE * 60) : endTime - MIN_SECTOR_TIME :
//                            isNotNormal && newTime < startTime ? 0 : startTime + MIN_SECTOR_TIME;
//                    boolean isPartNormal = startBoundAngle !=-1?
//                            newTime < endTime && isNotNormal:
//                            newTime > startTime && isNotNormal; //start time < end time as normal however still isNotNormal
//                    boolean limiter = startBoundAngle !=-1?
//                            newTime<=minEndTime && (isPartNormal? newTime<=(affectedSector.getColour()!=-1?affectedSector.getStartTime()+ MIN_SECTOR_TIME:affectedSector.getStartTime()) :
//                                    newTime>=(affectedSector.getColour()!=-1?affectedSector.getStartTime()+ MIN_SECTOR_TIME:affectedSector.getStartTime())) :
//                            newTime>=minEndTime && (isPartNormal? newTime>=(affectedSector.getColour()!=-1?affectedSector.getEndTime()- MIN_SECTOR_TIME:affectedSector.getEndTime()) :
//                                    newTime<=(affectedSector.getColour()!=-1?affectedSector.getEndTime()- MIN_SECTOR_TIME:affectedSector.getEndTime()));
//                    boolean limiter = startBoundAngle !=-1?
//                            newTime<=minEndTime && (isPartNormal? newTime<=affectedSector.getEndTime() :
//                                    newTime>=affectedSector.getEndTime()) :
//                            newTime>=minEndTime && (isPartNormal? newTime>=affectedSector.getStartTime():
//                                    newTime<=affectedSector.getStartTime());

                    if (startBoundAngle != -1) { //start time is being changed
                        //start>end - affected.end will always be lesser than start:
                        //if set = false then:
                        // - (startBound) max(newTime,aff.endTime) - it stops at aff.endTime
                        // - (endBound) min(newTime,aff.start) - it stops at aff.startTime
                        //if set = true then:
                        // - (startBound) min(newTime,end-MIN)
                        // - (endBound) max(newTime,start+MIN)
                        //end>start - affected.end may be greater than start
                        //if set = false then:
                        //-
                        //here newtime is start time

                        //new time and set not in sync -- error occurs due to discrepancy in userCurrAngle and newAngle values
                        Log.d(TAG,"NEW TIME "+newTime+" set "+set+" angle "+" "+userCurrAngle);
                        selectedSector.setStartTime(startTime>endTime?
                                !set?Math.max(newTime,affectedSector.getEndTime()):Math.min(newTime,endTime-MIN_SECTOR_TIME):
                                affectedSector.getEndTime()>startTime?
                                !set?Math.min(Math.min(newTime,endTime-MIN_SECTOR_TIME),affectedSector.getEndTime()):Math.max(newTime,affectedSector.getEndTime()):
                                Math.max(Math.min(newTime,endTime-MIN_SECTOR_TIME),affectedSector.getEndTime()));
//                        if(limiter) {
//                            selectedSector.setStartTime(newTime);
////                            affectedSector.setEndTime(newTime); //prev sector
//                        }
                    } else {
                        //here newtime is end time
                        //affected has to be bigger than selected
                        selectedSector.setEndTime(startTime>endTime?
                                !set?Math.min(newTime,affectedSector.getStartTime()):Math.max(newTime,startTime+MIN_SECTOR_TIME):
                                affectedSector.getStartTime()<endTime?
                                        !set?Math.max(Math.max(newTime,startTime+MIN_SECTOR_TIME),affectedSector.getStartTime()):Math.min(newTime,affectedSector.getStartTime()):
                                        Math.min(Math.max(newTime,startTime+MIN_SECTOR_TIME),affectedSector.getStartTime()));
//                        if(limiter) {
//                            selectedSector.setEndTime(newTime);
////                            affectedSector.setStartTime(newTime); //next sector
//                        }
                    }// sector being changed
                    userStartQuadrant = getQuadrant(userCurrAngle);
                    if(alphaValueAnimator ==null) {
                        alphaValueAnimator = ValueAnimator
                                .ofFloat(1.0f,0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0.0f);
                        alphaValueAnimator.setDuration(1000);
                        alphaValueAnimator.addUpdateListener((animation -> {
                            animatedAlphaValue = (float) animation.getAnimatedValue();
                            postInvalidateOnAnimation();
                        }));
                        alphaValueAnimator.start();
                    }
                    if(!alphaValueAnimator.isRunning()) {
                        //invalidate normally when not running
                        invalidate();
                    }
                    //need the alpha value
                    //todo HAVE USER EXPLICITLY DELETE FROM SECTOR LIST. USER SHOULD NOT BE ABLE TO GO PAST SECTOR MIN TIME
//                    if(affectedSector.getTotalTime(CLOCK_HOUR_MODE)==0){
//                        Log.d(TAG,"SECTOR REMOVED: "+ affectedSector.getName());
//                        sectors.remove(affectedSector);
////                        int in = sectors.indexOf(selectedSector);
////                        index=startBound!=-1?
////                                (in!=0?in-1:sectors.size()-1):
////                                (in!=sectors.size()-1?in+1:0);
//                        requestLayout();
//                    }

                }

                break;
            case MotionEvent.ACTION_UP:
                if(selectedSector==null)
                    return false;
                if(isEditing) {
                    currSector=null;
                    clockEnabled=true;
                    runClockThread();
                    isEditing =false;
                    if(alphaValueAnimator !=null) {
                        //to change back to real time
                        alphaValueAnimator.start();
                    }
                    initStaticChartElements(mainBGRectF,sectors);
                }
                //todo if clicked then handle displaying details of that sector
                //  -time left,
//                resume clock
//                set new values
                break;
        }
        return true;
    }
    // spans the text over the desired width
    private float getMaxTextSizeForWidth(Paint paint, float desiredWidth,
                                         String text, float maxSize) {

//        // Pick a reasonably large value for the test. Larger values produce
//        // more accurate results, but may cause problems with hardware
//        // acceleration. But there are workarounds for that, too; refer to
//        // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(maxSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        return maxSize * desiredWidth / bounds.width();
    }

    private final Rect clockTextMeasure = new Rect();
    private int maxTextHeight;
    private float clockHourWidth;
    private void addClockText(Canvas c, Paint clockTextPaint, @ClockAlignment int alignment){
        //add custom alignments for the text, top left right bottom center.
        maxDotRadius = Math.round(outerToInnerGap*.5f); //radius of the hour 'dot' (max radius) - used as a gap

        paint.getTextBounds(hourText,0,hourText.length(), clockTextMeasure); //height of the hour text
        //THIS WILL KEEP THE HEIGHT SAME WHEN SIZE CHANGES
        if(clockTextMeasure.height()>maxTextHeight)
            maxTextHeight=clockTextMeasure.height();
        switch(alignment) {
            case ClockAlignment.TOP: //270
            case ClockAlignment.LEFT: //180
            case ClockAlignment.RIGHT: //0
                c.drawText(hourText, clockXy[0]- clockHourWidth, clockXy[1], clockTextPaint);
                c.drawText(minuteText, clockXy[0]- clockHourWidth, clockXy[1]+maxTextHeight+ maxDotRadius, clockTextPaint);
                break;
            case ClockAlignment.CENTER:
                c.drawText(hourText, clockXy[0]- clockHourWidth, clockXy[1], clockTextPaint);
                c.drawText(minuteText, clockXy[1]- clockHourWidth, clockXy[1]+ maxTextHeight+ maxDotRadius, clockTextPaint);
                break;
            case ClockAlignment.BOTTOM: //90
                c.drawText(hourText, clockXy[0]- clockHourWidth, clockXy[1] - maxDotRadius, clockTextPaint);
                c.drawText(minuteText, clockXy[0]- clockHourWidth, clockXy[1]+ maxTextHeight, clockTextPaint);
                break;
        }
    }
    private TextPaint sectorTextPaint;
    private void addSectorsDetailsText(Canvas c, Sector currSector, RectF rectF, Paint p, @ClockAlignment int alignment){
        float radiusOfHourDot = Math.round(outerToInnerGap/2f); //radius of the hour 'dot' (max radius)
        p.setTextSize(.06f*rectF.width());
        p.setColor(topColour);
        int[] t = Helpers.timeConversion(elapsed_time<currSector.getEndTime()*60?currSector.getEndTime()*60-elapsed_time:
                currSector.getCorrectedEndTime(CLOCK_HOUR_MODE)*60 -elapsed_time);
        //get only the greatest time value
        String time =
                (t[0]!=0?t[1]!=0?"> "+t[0]+(t[0]!=1?" hrs":" hr"):t[0]+(t[0]!=1?" hrs":" hr"):"")+
                (t[1]!=0&&t[0]==0?(t[1]<10?"0"+t[1]+(t[1]!=1?" mins":" min"):t[1]+" mins"):"")+
                (t[2]!=0&&t[1]==0&&t[0]==0?(t[2]<10?"0"+t[2]+(t[2]!=1?" secs":" sec"):t[2]+" secs"):"");
        //truncate curr sector text
        if(sectorTextPaint==null)
            sectorTextPaint=new TextPaint(p);
        String currSec = TextUtils.ellipsize(
                currSector.getName(),
                sectorTextPaint,
                (rectF.width()*.7f)-(rectF.width()*.3f), //length of the divider line
                TextUtils.TruncateAt.END).toString();

        float[] xy = polarToCartesian(rectF.centerX(),rectF.centerY(),
                (rectF.width()/2f)-(float) (getAngleForTimeInSeconds(CLOCK_HOUR_MODE/12*60*60)/360f * (2*Math.PI*(rectF.width()/2f))),
                90);
        p.setStyle(Paint.Style.FILL);
        Rect rect = new Rect();
        p.getTextBounds(time,0,time.length(),rect);
        switch (alignment){
            case ClockAlignment.TOP:
                //bottom, left , right
                /* Sector name */
                c.drawText(currSec, xy[0]-p.measureText(currSec)*.5f,
                        xy[1], p);
                /* remaining time */
                p.setTextSize(.04f*rectF.width());
                c.drawText(time, xy[0]-p.measureText(time)*.5f,
                        xy[1]+rect.height()+radiusOfHourDot, p);
                break;
            case ClockAlignment.CENTER:
                break;
//            case ClockTextAlignment.BOTTOM:
//                break;
//            case ClockTextAlignment.LEFT:
//                break;
//            case ClockTextAlignment.RIGHT:
//                break;
        }
    }

    @IntDef({ClockAlignment.TOP, ClockAlignment.LEFT, ClockAlignment.CENTER, ClockAlignment.RIGHT, ClockAlignment.BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    private @interface ClockAlignment {
        int CENTER = -1;
        int TOP = 270;
        int LEFT = 180;
        int BOTTOM = 90;
        int RIGHT = 0;
    }
    @IntDef({DateStylePresets.LEFT_P1, DateStylePresets.BOTTOM_P2, DateStylePresets.RIGHT_P3,DateStylePresets.TOP_P4})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DateStylePresets {
        int LEFT_P1 = 0;
        int BOTTOM_P2 = 1;
        int RIGHT_P3 = 2;
        int TOP_P4 = 3;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    private void setSectorNumbersTextSize(float size){
        sectorNumbersTextSize =size;
    }


    private Sector findSectorForXY(float x, float y, ArrayList<Sector> data){
        //check the sector bounds the angle falls in
        Sector sector = null;
        for (int i=0; i<data.size() ; i++) {
            float startBound = getAngleForTimeInSeconds(data.get(i).getStartTime()*60),
                    endBound = getAngleForTimeInSeconds(data.get(i).getEndTime()*60);
            float a =cartesianToPolar(x,y)-CANVAS_ROTATION;
            a=a>360?a-360f:a;
            float angle = Math.round(a*10f)/10f; // to 1 dp
//            Log.d(TAG, "start "+startBound + " end " + endBound+" angle "+angle+" n "+ data.get(i).getName());

            if(angle>=startBound &&
                    angle<=endBound){
                sector= data.get(i);
//                Log.d(TAG,"normal "+data.get(i).getName());
                break;
            }
            //check if a sector lies in the 'special' area - end is lesser than start
            if(endBound-startBound<0){
                if(angle>=startBound || angle<=endBound){
                    sector= data.get(i);
//                    Log.d(TAG,"special "+data.get(i).getName());
                }
            }
        }
        return sector;
    }

    private int getQuadrant(float currentAngle){
        if(currentAngle<0 || currentAngle>360)
        throw new IllegalStateException("Invalid angle! : "+currentAngle);
        return
        currentAngle>0 && currentAngle<=90?1://1st quadrant
        currentAngle>90&&currentAngle<=180?2: //2nd quadrant
        currentAngle>180&&currentAngle<=270?3://3rd quadrant
        currentAngle>270&&currentAngle<=360?4://4th quadrant
        0;
    }

    private boolean isDirectionClockwise(float currentAngle, float dx, float dy){
        return
        ((currentAngle>0&&currentAngle<=90) && (dx<0||dy>0)) || //1st quadrant
        ((currentAngle>90&&currentAngle<=180) && (dx<0||dy<0)) || //2nd quadrant
        ((currentAngle>180&&currentAngle<=270) && (dx>0||dy<0)) || //3rd quadrant
        ((currentAngle>270&&currentAngle<=360) && (dx>0||dy>0)); //4th quadrant
    }
    private float getArcMeasure(PathMeasure pathMeasure, float subAngle){
        return pathMeasure.getLength() * subAngle/360f;
    }
    private float[] polarToCartesian(float cx, float cy, float radius, float angle) {
        float[] xy = new float[2];
        xy[0] = (float) (cx + (radius * Math.cos(Math.toRadians(angle))));// x
        xy[1] = (float) (cy + (radius * Math.sin(Math.toRadians(angle))));// y
        return xy;
    }

    private float cartesianToPolar(float x, float y){
        float rawArcTan = (((float)Math.toDegrees(Math.atan2(y-getHeight()/2f,x-getWidth()/2f)))); //0 - 180
        return rawArcTan>0f?rawArcTan:180+(180+rawArcTan); //0-360
    }
    //Helper : 0 - 1 alpha values

    private int getRGBAlpha(float alpha) {
        if (alpha < 0 || alpha > 1)
            Log.e(TAG, "Value should be between 0 and 1.");
        return (int) (alpha * 255);
    }
    //degrees to seconds ratio

    private float getAngleForTimeInSeconds(long seconds) throws IllegalStateException {
        if (seconds > TOTAL_SECONDS_IN_A_DAY) {
            throw new IllegalStateException("getAngleForTime(): "+seconds+" > total CLOCK_MODE seconds !");
//            Log.e("PieChartView", "getAngleForTime(): "+seconds+"> 86400 !.");
//            return 0;
        }
        return 360f * seconds / TOTAL_SECONDS_IN_A_DAY;
    }
    //seconds to degrees
    private int getTimeForAngleInDegrees(float angleInDegrees) throws IllegalStateException{
        if(angleInDegrees>360 || angleInDegrees<0){
            throw new IllegalStateException("getTimeForAngle(): invalid angle: "+angleInDegrees);
        }
        return (int)(angleInDegrees * TOTAL_SECONDS_IN_A_DAY/360f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //setting both width and height dimensions the same using the minimum of the two
        int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        int specHeight = MeasureSpec.getSize(heightMeasureSpec);
        int minSpecSize = Math.min(specHeight,specWidth);
        int minSpec = minSpecSize==specWidth?widthMeasureSpec:heightMeasureSpec;

        setMeasuredDimension(resolveSize(minSpecSize,minSpec),
                resolveSize(minSpecSize,minSpec));
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }
}
