package com.rigid.clocker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
//Do It for your family and yourself
//todo DEADLINE FOR COMPLETION - 30 September 2020
public class PieChartView extends View {
    /**
     * This PieChart represents the goals of the user in an articulate manner.
     * todo - CLicking on a sector should expand the details of the goal
     *  - User should be given a limit on how many goals they can add with regards to time (min time for each)
     *    this will ensure that the 24hr limit is never crossed for a day
     *  - Data will include times, colours, names given to goals
     *  - User should be able to change the sector times by dragging the arcs in 'edit mode'
     *  - Highlight only the segment that is active - include as a customizable option
     *   - Strict Mode where users data (productivity) is compared with other users globally
     *   - CLock will be available as a widget
     *  - Automatically load apps/links pre set by user at specified times
     *  - EXTRA FEATURES (PAID) : Focus mode where phone functionality can be limited and cant be used until a set time (apart from calls etc)
     *  -New Pie Styles
     * The sectors represent the time assigned respectively to the goals.
     * Total time of the sectors equals the total time in a day i.e 24hrs
     * Goals Clock - Sectors are greyed out as time progresses
     * Quadrant = 6 hrs (24hrs)/ 3 hrs (12hrs)
     * Customizable PieChart - User can determine sectors colours, add an image to a sector (clipped in), enable/disable Goals Clock...
     * *
     */
    private static final int DEFAULT_CIRCLE_COLOR = Color.RED;
    private static final String TAG = PieChartView.class.getSimpleName();
    private final int CLOCK_HOUR_MODE =12; //12 or 24
    private final float TOTAL_SECONDS_IN_A_DAY = CLOCK_HOUR_MODE * 3600;
    private final float CANVAS_ROTATION = -90;

    private int circleColor = DEFAULT_CIRCLE_COLOR;
    private RectF rectF;
    private Paint paint;

    float curr_sector_angle = 0f;
    int curr_sector = 0;
    boolean chart_dirty = true;
    private float sectorTextSize=40f; //default
    float start_angle = 0f;
    private float totalUserAngle; //default

    private ScheduledExecutorService mExecutor;
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
            clockEnabled=true; //when in edit sector mode
    private float startBound=-1,endBound=-1;
    private int affectedSector_index;
    private ArrayList<Sector> sectors;
    private boolean AmOrPm;
    private Path clockNumbersPath = new Path();
    private Path currentTimePath = new Path();

    public PieChartView(Context context) {
        super(context);
        init(context, null);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        sectors= new ArrayList<>();
        //todo minimum total time = 5 mins
        sectors.add(new Sector("Bla1",60,120,Color.RED));//1-2
        sectors.add(new Sector("Bla2",120,240,Color.BLUE));//2-4
        sectors.add(new Sector("Bla3",240,300,Color.GREEN));//4-5
        sectors.add(new Sector("Bla4",300,420,Color.GRAY));//5-7

        isPieFull();
        if(clockEnabled)
            runClockThread();
    }
    //returns total angle
    private void isPieFull(){
        //to check if remaining pie needs to be filled or not with break
            long lastEnd = sectors.get(sectors.size()-1).getEndTime(); //last sectors end = breaks start time, first sectors start = breaks end time
            sectors.add(new Sector("Break",lastEnd,sectors.get(0).getStartTime(),Color.BLACK));
    }

    private void runClockThread() {
        if (mExecutor == null)
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        mExecutor.scheduleWithFixedDelay(() -> {
            try {
                elapsed_time = getCurrentTime(CLOCK_HOUR_MODE);
                //todo start time = start time of first goal, endtime = end time of last goal
                AmOrPm = getCurrentTime(24)<12;

                postInvalidate();
//                Log.d("PieChartView", "elapsed " + elapsed_time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }
    private long getCurrentTime(int mode){
        Calendar rightNow = Calendar.getInstance();
        // offset to add since we're not UTC
        long offset = rightNow.get(Calendar.ZONE_OFFSET) +
                rightNow.get(Calendar.DST_OFFSET);
        long sinceMidnight = (rightNow.getTimeInMillis() + offset) %
                (mode * 60 * 60 * 1000);
        return sinceMidnight / 1000;
    }
    public void setCircleColor(int circleColor) {
        this.circleColor = circleColor;
        invalidate();
    }
    private RectF clockRectF= new RectF();
    private float strokeWidthArc = 20f;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //add text size to the left and top of rect to avoid it clipping at the edges
        //figure out the max radius it can be, taking text in to account
        //although getWidth and height will be the same, using math.min is just a precaution
        float sectorAndStrokeWidth = sectorTextSize+strokeWidthArc;
        float max_radius = Math.min(w,h)- sectorAndStrokeWidth;
        rectF = new RectF(sectorAndStrokeWidth, sectorAndStrokeWidth, max_radius, max_radius);
        float diameterPercentile = 0.10f; //0 - 1
        float clockPosition = sectorAndStrokeWidth+((rectF.width())*diameterPercentile);
        float clockRectSize=sectorTextSize+(rectF.width()*(1-diameterPercentile));
        clockRectF = new RectF(clockPosition,
                clockPosition,
                clockRectSize,
                clockRectSize);
        paint = new Paint();
//        Path path = new Path();
//        PathMeasure pathMeasure = new PathMeasure(path,true);
//        Path path1 = new Path();
//        path1.addCircle(0f,0f,8f, Path.Direction.CW);
//        pathEffect= new PathDashPathEffect(path1,pathMeasure.getLength(),pathMeasure.getLength()*0.5f, PathDashPathEffect.Style.TRANSLATE);
//        pathEffect = new DashPathEffect(new float[]{20,0,0,0},10f);
//        paint.setPathEffect(pathEffect);
//        paint.setColor(Color.RED);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(20f);
    }
//    Path p = new Path();
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        p.rewind();
//        p.addArc(clockRectF,0,360);
//        canvas.clipPath(p, Region.Op.DIFFERENCE);
        makePie(sectors, canvas, rectF);
//        Paint p = new Paint();
//        p.setColor(Color.WHITE);
//        canvas.drawArc(clockRectF,0f,360f,true,p);

//        canvas.drawCircle(rectF.centerX(),rectF.centerY(),rectF.width()*0.25f,p);
        //for the PathDashEffect and PathDashPathEffect use 2 seperate loops
//        canvas.drawPath(createPath(path,4, getWidth()*0.5f),paint);
    }

    public int getCircleColor() {
        return circleColor;
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    private void setSectorTextSize(float size){
        sectorTextSize =size;
    }

    private void enableText(boolean enable){}

    private Sector findSectorForXY(float x, float y, ArrayList<Sector> data){
        //check the sector bounds the angle falls in
        Sector sector = null;
        for (int i=0; i<data.size() ; i++) {
            float startBound = getAngleForTimeInSeconds(data.get(i).getStartTime()*60),
                    endBound = getAngleForTimeInSeconds(data.get(i).getEndTime()*60);
            float a =cartesianToPolar(x,y)-CANVAS_ROTATION;
            a=a>360?a-360f:a;
            float angle = Math.round(a*10f)/10f; // to 1 dp
            Log.d(TAG, "start "+startBound + " end " + endBound+" angle "+angle+" n "+ data.get(i).getName());

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

    //todo be able to change start and end times by dragging at each end of the sector
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //handles selecting sectors and editing their values
        float x = event.getX();
        float y = event.getY();
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                selectedSector = findSectorForXY(x,y, sectors);
                Log.d(TAG,"sector clicked: "+selectedSector.getName());

                //ONLY IN EDIT MODE
                if(isEditSectorMode) {
                    //todo when a sector's total angle = 0, remove that sector upon users permission
                    userStartAngle = cartesianToPolar(x, y)-CANVAS_ROTATION; //for +/- angles
                    userStartAngle=Math.min(360f,userStartAngle>360?userStartAngle-360:userStartAngle);
                    selectedSectorTotalAngle = getAngleForTimeInSeconds(selectedSector.getTotalTime(CLOCK_HOUR_MODE) * 60);
                    startBound =getAngleForTimeInSeconds(selectedSector.getStartTime()*60);
                    endBound=getAngleForTimeInSeconds(selectedSector.getEndTime()*60);
                    float middle = start_angle+(startBound +selectedSectorTotalAngle/2f);

                    //determine if its start or end bound thats changing
                    if(endBound<startBound){ //handle the case where end bound ends up beyond 360
                        if(middle<360) {
                            if (userStartAngle >= startBound && userStartAngle < middle)
                                endBound = -1;
                            else if (userStartAngle >= middle || userStartAngle < endBound)
                                startBound = -1;
                        }else{
                            if(userStartAngle >=startBound || userStartAngle<(middle-360))
                                endBound=-1;
                            else if(userStartAngle >= (middle-360) && userStartAngle < endBound)
                                startBound=-1;
                        }
                    }else {
                        if ((userStartAngle >= startBound) && (userStartAngle < middle)) {
                            endBound = -1;
                        } else if ((userStartAngle >= middle) && (userStartAngle < endBound)) {
                                startBound = -1;
                        }
                    }
                    int in=sectors.indexOf(selectedSector);
                    affectedSector_index = startBound!=-1?
                            (in!=0?in-1:sectors.size()-1):
                            (in!=sectors.size()-1?in+1:0);
                    Log.d(TAG,"st "+startBound+" en "+endBound+" mid "+middle+" u "+userStartAngle);
                    userStartQuadrant = getQuadrant(userStartAngle);
                    //reset these here...
                    switcher = -1;
                    set = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //ONLY IN EDIT MODE
                if(isEditSectorMode) {
                    //suspend clock thread
                    float userCurrAngle = cartesianToPolar(x, y)-CANVAS_ROTATION;
                    userCurrAngle=Math.min(360f,userCurrAngle>360?userCurrAngle-360f:userCurrAngle);
                    float dAngle = userCurrAngle - userStartAngle; // default behaviour - simply + if increasing or - otherwise
                    // angles will shift to 0 or 360 once over the start point for the pie so the following is to handle that...
                    // determined that it only happens when angle goes from 4th to 1st or 1st to 4th quadrants
                    int dQuadrant = getQuadrant(userCurrAngle) - userStartQuadrant;
                    //switch set true or false
                    //when one is true the other is off until the whole event is cancelled (ACTION_UP)
                    if (dQuadrant == -3) { // 4th to 1st quadrant (1-4)
                        if (switcher == 3 || switcher == -1) { // if set previously set=false as its gone back to normal state
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
                    //handle according to the direction of cursor
                    if (set) {
                        if (switcher == -3) {
                            dAngle = (360 + userCurrAngle) - userStartAngle;
                        } else if (switcher == 3) {
                            dAngle = -((360 - userCurrAngle) + userStartAngle); //- since we are decreasing (anticlockwise)
                        }
                    }
                    float newAngle=(startBound!=-1?startBound:endBound) + dAngle;
                    newAngle=newAngle<0f?360f+newAngle:newAngle>360f?newAngle-360f:newAngle;
//                    float newAngleA=affectedAngle + dAngle;
                    Sector affectedSector = sectors.get(affectedSector_index);
                    int newTime=getTimeForAngleInDegrees(Math.min(360, Math.max(0, newAngle))) / 60;
                    Log.d(TAG,"d angle "+newAngle+" time "+newTime);
                    if (startBound != -1) { //start time is being changed
                        selectedSector.setStartTime(newTime);
                        affectedSector.setEndTime(newTime);

                    } else {
                        selectedSector.setEndTime(newTime);
                        affectedSector.setStartTime(newTime);
                    }// sector being changed
                    //todo HAVE USER EXPLICITLY DELETE FROM SECTOR LIST. USER SHOULD NOT BE ABLE TO GO PAST SECTOR MIN TIME
                    if(affectedSector.getTotalTime(CLOCK_HOUR_MODE)==0){
                        Log.d(TAG,"SECTOR REMOVED: "+ affectedSector.getName());
                        sectors.remove(affectedSector);
//                        int in = sectors.indexOf(selectedSector);
//                        index=startBound!=-1?
//                                (in!=0?in-1:sectors.size()-1):
//                                (in!=sectors.size()-1?in+1:0);
                        requestLayout();
                    }
                    userStartQuadrant = getQuadrant(userCurrAngle);
                    invalidate();
                }

                break;
            case MotionEvent.ACTION_UP:
                //todo if clicked then handle displaying details of that sector
                //  -time left,
//                resume clock
//                set new values
                break;
        }
        return true;
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
    //PieChart with respect to time
    //The pie chart represents real time by shading itself as time elapses (24hrs)
    //tells us what sector the time is currently in
    //names each sector
    //t - times for goals

    private void makePie(ArrayList<Sector> sectors, Canvas canvas, RectF rectF) {
        canvas.rotate(CANVAS_ROTATION,rectF.centerX(),rectF.centerY()); // 0 degrees on top instead of at 90 degrees

        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        float radius = (rectF.right - rectF.left) / 2;
//        radius *= 1; // 0 being centered, 1 being on the arc edges.
        paint.setAlpha(getPaintAlpha(1f));
        //lay down the clock numbers
        float startTimeAngle= 0f; // start with 270 degrees as it represents the top of the 'circle'
        //todo take sweepAngle out of the scope to avoid having it called every time
        float sweepAngle = getAngleForTimeInSeconds(3600); //1hr gap
        for(int i = 0; i< CLOCK_HOUR_MODE; i++){
            paint.setTextSize(sectorTextSize);
            paint.setColor(Color.BLACK);

            clockNumbersPath.rewind();
            clockNumbersPath.addArc(rectF,startTimeAngle,sweepAngle); //- offset the text in the middle of the angles
            canvas.drawTextOnPath((i==0? CLOCK_HOUR_MODE :(CLOCK_HOUR_MODE -(CLOCK_HOUR_MODE -i)))+"", clockNumbersPath,0f,-strokeWidthArc,paint);
            startTimeAngle+=sweepAngle;
        }
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(strokeWidthArc);
//        canvas.clipOutRect(clockRectF);
        //lay down the chart sectors
//        float[] time_angles = new float[t.length];
        for(Sector s: sectors){
            float sweep_angle = getAngleForTimeInSeconds(s.getTotalTime(CLOCK_HOUR_MODE)*60);
            paint.setColor(s.getColour());
            canvas.drawArc(rectF, start_angle+getAngleForTimeInSeconds(s.getStartTime()*60), sweep_angle, true, paint);
//            Log.d(TAG,"sectors "+s.getName()+" "+(start_angle+getAngleForTimeInSeconds(s.getStartTime()*60))+" "+(sweep_angle));
        }
        if(clockEnabled) {
            //* TIME *
            //updating the time sector
            float timeStartAngle = getAngleForTimeInSeconds(9*3600); // starts at9  (as a test, set by the user)
            float sweep_time_angle = getAngleForTimeInSeconds((int) elapsed_time);
            //1. this path will draw current time always
            currentTimePath.rewind();
            currentTimePath.moveTo(rectF.centerX(),rectF.centerY());
            currentTimePath.lineTo(rectF.centerX()+(float) (radius*Math.cos(Math.toRadians(sweep_time_angle))),
                    rectF.centerY()+(float)(radius*Math.sin(Math.toRadians(sweep_time_angle))));
            currentTimePath.close();
            paint.setAlpha(getPaintAlpha(1f));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.RED);
            canvas.drawPath(currentTimePath,paint);
            //2. once time reaches the start time, start drawing the arc to represent time past

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLUE);
            paint.setAlpha(getPaintAlpha(0.5f));

            if (sweep_time_angle >= (AmOrPm ? (360 - timeStartAngle) : timeStartAngle)) {
                float sweepy = sweep_time_angle - timeStartAngle;
                if (AmOrPm)
                    sweepy = sweep_time_angle + (360 - timeStartAngle); //gone past 360/0
                canvas.drawArc(rectF, timeStartAngle, sweepy, true, paint);
            }
            Log.d(TAG, "ELAPSED TIME (SECONDS) " + elapsed_time + "-> ANGLE " + sweep_time_angle);

//
            //determines what sector time is in (time_angle > sector_angle? loop to find sector)
            //alternative but more costly: intersection coordinates
//        if (sweep_time_angle > curr_sector_angle )
//            for (float a : time_angles) {
//                if (curr_sector_angle > sweep_time_angle)
//                    break;
//                curr_sector_angle += a;
//                ++curr_sector;
//            }
//        Log.d(TAG, "current sector -> " + curr_sector);
        }
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

    private int getPaintAlpha(float alpha) {
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
    private void invalidateAndRequestLayout(){
        invalidate(); // changes to the views attributes ... added to the onDraw queue
        requestLayout(); // changes to size or shape
    }

    //create Polygon path
    private Path createPath(Path path,int sides, float radius){
        float cx=getWidth()/2f, cy = getHeight()/2f;
        float angle = (float) (2.0 * Math.PI / sides); //since all angles are equal in a polygon
        path.moveTo(
                (float)(cx + (radius * Math.cos(0.0))),
                (float)(cy + (radius * Math.sin(0.0))));
        for (int i=1; i<=sides;i++) {
            path.lineTo(
                    cx + (float) (radius * Math.cos(angle * i)),
                    cy + (float) (radius * Math.sin(angle * i)));
        }
        path.close();
        return path;
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
